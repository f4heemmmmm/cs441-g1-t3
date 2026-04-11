package netemu.device;

import netemu.common.*;

import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public abstract class Node {
    
    protected final Log log;
    protected final String name;
    protected DatagramSocket socket;
    protected InetSocketAddress lanEmulatorAddress;
    protected final NetworkInterface networkInterfaceCard;

    protected Node(String name, NetworkInterface networkInterfaceCard, String color) {
        this.name = name;
        this.networkInterfaceCard = networkInterfaceCard;
        this.log = new Log(name, color);
    }

    protected String logColor() {
        return Ansi.GREEN;
    }

    /**
     * Starts the node by:
     *  1. Binding socket
     *  2. Registering with the LAN Emulator
     *  3. Starting the receiver thread
     *  4. Starting the CLI
     */
    public void start() throws IOException {
        socket = new DatagramSocket(networkInterfaceCard.devicePortNumber());
        lanEmulatorAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), networkInterfaceCard.lanEmulatorPortNumber());

        Ansi.banner(name + " | MAC: " + networkInterfaceCard.macAddress() + " | IP: " + networkInterfaceCard.ipAddress() + " | LAN" + networkInterfaceCard.lanID(), logColor());
        log.info("Listening on port " + networkInterfaceCard.devicePortNumber());

        register();
        startReceiver();
        runCLI();
    }

    /**
     * Registers with the LAN Emulator.
     * Simulates the startup handshake.
     */
    private void register() throws IOException {
        // Build registration command with the node's MAC Address and listening Port Number
        String message = "REGISTER " + networkInterfaceCard.macAddress().value() + " " + networkInterfaceCard.devicePortNumber();

        // Encode the command as raw bytes for UDP transport
        byte[] data = message.getBytes();

        // Create a UDP packet addressed to the LAN emulator
        DatagramPacket packet = new DatagramPacket(data, data.length, lanEmulatorAddress);

        // Send registration so the emulator can map the  MAC Address to the node's Port Number
        socket.send(packet);
        log.info("Connected to LAN" + networkInterfaceCard.lanID() + " emulator");
    }

    private void startReceiver() {
        Thread thread = new Thread(() -> {
            byte buffer[] = new byte[1024];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                    EthernetFrame frame = EthernetFrame.decode(data);
                    handleFrame(frame);
                } catch (Exception e) {
                    log.error("Receive error: " + e.getMessage());
                }
            }
        }, name + "-rx");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Handles the incoming Ethernet Frame
     */
    protected void handleFrame(EthernetFrame frame) {
        // MAC filtering: Only accept frames addressed to the node or broadcast 
        if (!frame.destinationMACAddress().equals(networkInterfaceCard.macAddress()) && !frame.destinationMACAddress().isBroadcast()) {
            return; // Silently drop
        }
        processFrame(frame);
    }

    /**
     * Processes a frame that has passed MAC filtering
     */
    protected void processFrame(EthernetFrame frame) {
        // Decode the Ethernet frame payload into an IP packet and dispatch it for handling
        IPPacket packet = IPPacket.decode(frame.data());

        // Opportunistic ARP learning from all IP-based traffic.
        // This keeps cache warm without requiring an explicit ARP exchange every time.
        arpCache.put(packet.sourceIPAddress(), frame.sourceMACAddress());

        log.rx(frame.toString());
        log.rx("  └─ " + packet);
        handleIPPacket(packet, frame);
    }

    /**
     * Handle an IP packet extracted from a frame (after de-encapsulation)
     */
    protected void handleIPPacket(IPPacket packet, EthernetFrame frame) {
        if (packet.protocol() == IPPacket.PROTOCOL_ICMP) {
            handlePing(packet, frame);
        } else if (packet.protocol() == IPPacket.PROTOCOL_DATA) {
            handleTextMessage(packet);
        }
    }

    /**
     * Handle incoming text message (plaintext or encrypted)
     */
    protected void handleTextMessage(IPPacket packet) {
        TextMessage msg = TextMessage.decode(packet.data());
        if (msg.isEncrypted()) {
            log.rx("Encrypted message from " + packet.sourceIPAddress() + ": \"" + msg.text() + "\"");
        } else {
            log.rx("Message from " + packet.sourceIPAddress() + ": \"" + msg.text() + "\"");
        }
    }

    /**
     * Handle ICMP ping request/reply
     */
    protected void handlePing(IPPacket packet, EthernetFrame frame) {
        PingMessage ping = PingMessage.decode(packet.data());

         if (ping.isRequest()) {
            log.rx("Ping request from " + packet.sourceIPAddress() + " (seq=" + ping.sequence() + ")");

            // Send reply
            PingMessage reply = ping.toReply();
            IPPacket replyPacket = IPPacket.icmp(networkInterfaceCard.ipAddress(), packet.sourceIPAddress(), reply.encode());
            sendIPPacket(replyPacket);
        } else if (ping.isReply()) {
            long rtt = ping.RTT();
            log.rx("Ping reply from " + packet.sourceIPAddress() + " (seq=" + ping.sequence() + ", RTT=" + rtt + "ms)");
        }
    }

    /**
     * Send an IP Packet
     * Encapsulated in an Ethernet frame with proper addressing
     */
    protected void sendIPPacket(IPPacket packet) {
        try {
            int destinationLAN = packet.destinationIPAddress().lanID();
            MACAddress destinationMACAddress;

            if (destinationLAN == networkInterfaceCard.lanID()) {
                // If same LAN, resolve destination MAC Address
                destinationMACAddress = AddressTable.resolve(packet.destinationIPAddress());
            } else {
                // If different LAN, resolve local router interface MAC (gateway) via ARP cache + request.
                IPAddress gatewayIP = AddressTable.getRouterIPAddressForLAN(networkInterfaceCard.lanID());
                destinationMACAddress = resolveSameLANMACAddress(gatewayIP);
            }

            if (destinationMACAddress == null) {
                log.warn("ARP resolution timed out for " + packet.destinationIPAddress() + " — packet dropped");
                return;
            }

            EthernetFrame frame = new EthernetFrame(networkInterfaceCard.macAddress(), destinationMACAddress, packet.encode());
            sendFrame(frame, lanEmulatorAddress);
        } catch (Exception e) {
            log.error("Failed to send packet: " + e.getMessage());
        }
    }

    private MACAddress resolveSameLANMACAddress(IPAddress targetIP) {
        MACAddress cached = arpCache.get(targetIP);
        if (cached != null) {
            return cached;
        }

        sendARPRequest(targetIP);

        // Wait briefly for ARP reply to keep UX responsive in CLI usage.
        long deadline = System.currentTimeMillis() + 400;
        while (System.currentTimeMillis() < deadline) {
            cached = arpCache.get(targetIP);
            if (cached != null) {
                return cached;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }

        // ARP resolution timed out — no static fallback; caller decides what to do.
        return null;
    }

    protected void sendARPRequest(IPAddress targetIP) {
        try {
            ARPMessage arp = ARPMessage.request(networkInterfaceCard.ipAddress(), networkInterfaceCard.macAddress(), targetIP);
            IPPacket packet = IPPacket.arp(networkInterfaceCard.ipAddress(), targetIP, arp.encode());
            EthernetFrame frame = new EthernetFrame(networkInterfaceCard.macAddress(), MACAddress.BROADCAST, packet.encode());
            sendFrame(frame, lanEmulatorAddress);
        } catch (Exception e) {
            log.error("Failed to send ARP request: " + e.getMessage());
        }
    }

    /**
     * Send an Ethernet Frame to the LAN emulator
     */
    protected void sendFrame(EthernetFrame frame, InetSocketAddress targetAddress) throws IOException {
        byte[] data = frame.encode();
        DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress);
        socket.send(packet);
        log.tx(frame.toString());
        log.tx("  └─ " + IPPacket.decode(frame.data()));
    }

    /**
     * Run the CLI for the node
     */
    protected void runCLI() {
        try (Scanner scanner = new Scanner(System.in)) {
            printHelp();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                try {
                    if (!handleCommand(line)) {
                        System.out.println("Unknown command. Type 'help' for available commands.");
                    }
                } catch (Exception e) {
                    log.error("Invalid command: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles a CLI command
     * Returns true if command is handled
     */
    protected boolean handleCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        return switch (cmd) {
            case "ping" -> { handlePingCommand(parts); yield true; }
            case "arp" -> { handleArpCommand(parts); yield true; }
            case "msg" -> { handleMsgCommand(parts, false); yield true; }
            case "emsg" -> { handleMsgCommand(parts, true); yield true; }
            case "help" -> { printHelp(); yield true; }
            case "info" -> { printInfo(); yield true; }
            case "quit", "exit" -> { System.exit(0); yield true; }
            default -> false;
        };
    }

    /**
     * Handle the 'msg' or 'emsg' CLI command
     */
    private void handleMsgCommand(String[] parts, boolean encrypted) {
        if (parts.length < 3) {
            System.out.println("Usage: " + (encrypted ? "emsg" : "msg") + " <destIP> <text>");
            return;
        }

        IPAddress destinationIPAddress = IPAddress.parse(parts[1]);

        // Join remaining parts as the message text
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(parts[i]);
        }
        String text = sb.toString();

        TextMessage msg = encrypted ? TextMessage.encrypted(text) : TextMessage.plaintext(text);
        IPPacket packet = IPPacket.data(networkInterfaceCard.ipAddress(), destinationIPAddress, msg.encode());
        log.info("Sending " + (encrypted ? "encrypted" : "plaintext") + " message to " + destinationIPAddress + ": \"" + text + "\"");
        sendIPPacket(packet);
    }

    /**
     * Handle the 'ping' CLI command
     */
    private void handlePingCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: ping <destIP> [count <n>]");
            return;
        }

        IPAddress destinationIPAddress = IPAddress.parse(parts[1]);
        int count = 1;
        for (int i = 2; i < parts.length - 1; i++) {
            if ("count".equalsIgnoreCase(parts[i])) {
                count = Integer.parseInt(parts[i + 1]);
            }
        }

        log.info("Pinging " + destinationIPAddress + " with " + count + " packets...");
        for (int i = 0; i < count; i++) {
            PingMessage ping = PingMessage.request(i + 1);
            IPPacket packet = IPPacket.icmp(networkInterfaceCard.ipAddress(), destinationIPAddress, ping.encode());
            sendIPPacket(packet);
            if (i < count - 1) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void handleArpCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: arp show | arp resolve <ip>");
            return;
        }

        switch (parts[1].toLowerCase()) {
            case "show" -> {
                System.out.println("ARP cache:");
                if (arpCache.isEmpty()) {
                    System.out.println("  (empty)");
                } else {
                    for (Map.Entry<IPAddress, MACAddress> e : arpCache.entrySet()) {
                        System.out.println("  " + e.getKey() + " -> " + e.getValue());
                    }
                }
            }
            case "resolve" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: arp resolve <ip>");
                    return;
                }
                IPAddress targetIPAddress = IPAddress.parse(parts[2]);
                sendARPRequest(targetIPAddress);
            }
            default -> System.out.println("Usage: arp show | arp resolve <ip>");
        }
    }

    /**
     * Print available CLI commands
     */
    protected void printHelp() {
        System.out.println("\n--- " + name + " Commands ---");
        System.out.println("  ping <destIP> [count <n>]  - Send ping(s)");
        System.out.println("  arp show                   - Show learned ARP cache");
        System.out.println("  arp resolve <ip>           - Send ARP request for an IP");
        System.out.println("  msg <destIP> <text>        - Send plaintext message");
        System.out.println("  emsg <destIP> <text>       - Send encrypted message");
        System.out.println("  info                       - Show interface info");
        System.out.println("  help                       - Show this help");
        System.out.println("  quit                       - Exit");
    }

    /** 
     * Print interface information
     */
    protected void printInfo() {
        System.out.println("  Name: " + name);
        System.out.println("  MAC:  " + networkInterfaceCard.macAddress());
        System.out.println("  IP:   " + networkInterfaceCard.ipAddress());
        System.out.println("  LAN:  " + networkInterfaceCard.lanID());
        System.out.println("  Port: " + networkInterfaceCard.devicePortNumber());
    }
}
