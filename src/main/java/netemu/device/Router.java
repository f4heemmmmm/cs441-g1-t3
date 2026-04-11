package netemu.device;

import netemu.common.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import java.util.Scanner;

/**
 * Router with 3 interfaces (R1, R2, R3), one per LAN.
 * Performs IP packet forwarding between LANs.
 * Integrates IDS for security monitoring.
 */
public class Router {

    private final Log log = new Log("Router", Ansi.PURPLE);

    // R1
    private final NetworkInterface networkInterfaceCardRouter1 = new NetworkInterface(AddressTable.MAC_R1, AddressTable.IP_R1, 1, AddressTable.ROUTER1_PORT, AddressTable.LAN1_PORT);

    // R2
    private final NetworkInterface networkInterfaceCardRouter2 = new NetworkInterface(AddressTable.MAC_R2, AddressTable.IP_R2, 2, AddressTable.ROUTER2_PORT, AddressTable.LAN2_PORT);

    // R3
    private final NetworkInterface networkInterfaceCardRouter3 = new NetworkInterface(AddressTable.MAC_R3, AddressTable.IP_R3, 3, AddressTable.ROUTER3_PORT, AddressTable.LAN3_PORT);

    private DatagramSocket socket1, socket2, socket3;
    private InetSocketAddress LAN1Address, LAN2Address, LAN3Address;

    private IntrusionDetectionSystem IDS;

    public void start() throws IOException {
        Ansi.banner("Router [R1/R2/R3]", Ansi.PURPLE);

        InetAddress loopback = InetAddress.getLoopbackAddress();
        LAN1Address = new InetSocketAddress(loopback, AddressTable.LAN1_PORT);
        LAN2Address = new InetSocketAddress(loopback, AddressTable.LAN2_PORT);
        LAN3Address = new InetSocketAddress(loopback, AddressTable.LAN3_PORT);

        socket1 = new DatagramSocket(AddressTable.ROUTER1_PORT);
        socket2 = new DatagramSocket(AddressTable.ROUTER2_PORT);
        socket3 = new DatagramSocket(AddressTable.ROUTER3_PORT);

        IDS = new IntrusionDetectionSystem(log);

        register(socket1, LAN1Address, networkInterfaceCardRouter1);
        register(socket2, LAN2Address, networkInterfaceCardRouter2);
        register(socket3, LAN3Address, networkInterfaceCardRouter3);

        startReceiver(socket1, networkInterfaceCardRouter1, "Router-LAN1-rx");
        startReceiver(socket2, networkInterfaceCardRouter2, "Router-LAN2-rx");
        startReceiver(socket3, networkInterfaceCardRouter3, "Router-LAN3-rx");

        log.info("Router started with 3 interfaces (R1, R2 and R3)");
        runCLI();
    }

    private void register(DatagramSocket socket, InetSocketAddress LANAddress, NetworkInterface networkInterfaceCard) throws IOException {
        String msg = "REGISTER " + networkInterfaceCard.macAddress().value() + " " + networkInterfaceCard.devicePortNumber();
        byte[] data = msg.getBytes();
        DatagramPacket pkt = new DatagramPacket(data, data.length, LANAddress);
        socket.send(pkt);
        log.info("Registered interface " + networkInterfaceCard.macAddress() + " on LAN" + networkInterfaceCard.lanID());
    }

    private void startReceiver(DatagramSocket socket, NetworkInterface networkInterfaceCard, String threadName) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    socket.receive(pkt);
                    byte[] data = new byte[pkt.getLength()];
                    System.arraycopy(pkt.getData(), pkt.getOffset(), data, 0, pkt.getLength());
                    EthernetFrame frame = EthernetFrame.decode(data);
                    handleFrame(frame, networkInterfaceCard);
                } catch (Exception e) {
                    log.error("Failed to receive on " + networkInterfaceCard.macAddress() + ": " + e.getMessage());
                }
            }
        }, threadName);
        t.setDaemon(true);
        t.start();
    }

    private void handleFrame(EthernetFrame frame, NetworkInterface incomingNetworkInterfaceCard) {
        IPPacket packet = IPPacket.decode(frame.data());

        // ARP is link-local and visible to all participants on the LAN emulator.
        // Inspect ARP before MAC filtering so DAI can detect forged replies even
        // when they are sent directly to a victim host.
        if (packet.protocol() == IPPacket.PROTOCOL_ARP) {
            handleArpPacket(packet, frame, incomingNetworkInterfaceCard);
            return;
        }

        // Only accept frames addressed to this router interface or broadcast
        if (!frame.destinationMACAddress().equals(incomingNetworkInterfaceCard.macAddress()) && !frame.destinationMACAddress().isBroadcast()) {
            return;
        }

        log.rx(frame + " on " + incomingNetworkInterfaceCard.macAddress());
        log.rx("  └─ " + packet);

        // IDS Inspection
        if (IDS.isEnabled()) {
            if (IDS.inspect(packet, frame, incomingNetworkInterfaceCard)) {
                return; // IDS blocked this packet
            }
        }

        // Is this packet addressed to one of the router's own IPs?
        if (isOurIp(packet.destinationIPAddress())) {
            handleLocalPacket(packet, frame, incomingNetworkInterfaceCard);
        } else {
            forwardPacket(packet, incomingNetworkInterfaceCard);
        }
    }

    private boolean isOurIp(IPAddress ipAddress) {
        return ipAddress.equals(AddressTable.IP_R1) || ipAddress.equals(AddressTable.IP_R2) || ipAddress.equals(AddressTable.IP_R3);
    }

    private void handleLocalPacket(IPPacket packet, EthernetFrame frame, NetworkInterface incomingNetworkInterfaceCard) {
        if (packet.protocol() == IPPacket.PROTOCOL_ICMP) {
            PingMessage ping = PingMessage.decode(packet.data());
            if (ping.isRequest()) {
                log.rx("Ping request from " + packet.sourceIPAddress() + " (seq=" + ping.sequence() + ") on interface " + incomingNetworkInterfaceCard.macAddress());
                
                // Reply from the interface that received the packet
                PingMessage reply = ping.toReply();
                IPPacket replyPacket = IPPacket.icmp(incomingNetworkInterfaceCard.ipAddress(), packet.sourceIPAddress(), reply.encode());
                sendToLAN(replyPacket, incomingNetworkInterfaceCard);
            } else {
                log.rx("Ping reply from " + packet.sourceIPAddress() + " (seq=" + ping.sequence() + ")");
            }
        }
    }

    private void forwardPacket(IPPacket packet, NetworkInterface incomingNetworkInterfaceCard) {
        int destinationLAN = packet.destinationIPAddress().lanID();
        log.info("Routing: " + packet.sourceIPAddress() + " -> " + packet.destinationIPAddress() + " via LAN" + destinationLAN);

        NetworkInterface outgoingNetworkInterfaceCard = networkInterfaceCardForLAN(destinationLAN);
        if (outgoingNetworkInterfaceCard == null) {
            log.error("Cannot route: no interface for LAN" + destinationLAN);
            return;
        }

        sendToLAN(packet, outgoingNetworkInterfaceCard);
    }

    private void sendToLAN(IPPacket packet, NetworkInterface outgoingNetworkInterfaceCard) {
        try {
            MACAddress destinationMACAddress = AddressTable.resolve(packet.destinationIPAddress());
            EthernetFrame frame = new EthernetFrame(outgoingNetworkInterfaceCard.macAddress(), destinationMACAddress, packet.encode());
            sendRawFrameToLAN(frame, outgoingNetworkInterfaceCard);
            log.tx(frame.toString());
            log.tx("  └─ " + packet);
        } catch (Exception e) {
            log.error("Failed to forward packet: " + e.getMessage());
        }
    }

    private void sendRawFrameToLAN(EthernetFrame frame, NetworkInterface outgoingNetworkInterfaceCard) {
        try {
            byte[] data = frame.encode();
            InetSocketAddress target = LANAddressforNetworkInterfaceCard(outgoingNetworkInterfaceCard);
            DatagramPacket udp = new DatagramPacket(data, data.length, target);
            socketForNic(outgoingNetworkInterfaceCard).send(udp);
        } catch (Exception e) {
            log.error("Failed to send frame: " + e.getMessage());
        }
    }

    private NetworkInterface networkInterfaceCardForLAN(int lanID) {
        return switch (lanID) {
            case 1 -> networkInterfaceCardRouter1;
            case 2 -> networkInterfaceCardRouter2;
            case 3 -> networkInterfaceCardRouter3;
            default -> null;
        };
    }

    private DatagramSocket socketForNic(NetworkInterface networkInterfaceCard) {
        return switch (networkInterfaceCard.lanID()) {
            case 1 -> socket1;
            case 2 -> socket2;
            case 3 -> socket3;
            default -> throw new IllegalArgumentException("Unknown LAN: " + networkInterfaceCard.lanID());
        };
    }

    private InetSocketAddress LANAddressforNetworkInterfaceCard(NetworkInterface networkInterfaceCard) {
        return switch (networkInterfaceCard.lanID()) {
            case 1 -> LAN1Address;
            case 2 -> LAN2Address;
            case 3 -> LAN3Address;
            default -> throw new IllegalArgumentException("Unknown LAN: " + networkInterfaceCard.lanID());
        };
    }

    private void runCLI() {
        try (Scanner scanner = new Scanner(System.in)) {
            printHelp();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                try {
                    String[] parts = line.split("\\s+");
                    String cmd = parts[0].toLowerCase();

                    switch (cmd) {
                        case "ping" -> handlePingCommand(parts);
                        case "ids" -> handleIDSCommand(parts);
                        case "info" -> printInfo();
                        case "help" -> printHelp();
                        case "quit", "exit" -> System.exit(0);
                        default -> System.out.println("Unknown command. Type 'help'.");
                    }
                } catch (Exception e) {
                    log.error("Invalid command: " + e.getMessage());
                }
            }
        }
    }

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

        // Determine which interface to send from based on destination LAN
        int destinationLAN = destinationIPAddress.lanID();
        NetworkInterface outgoingNetworkInterfaceCard = networkInterfaceCardForLAN(destinationLAN);
        if (outgoingNetworkInterfaceCard == null) {
            log.error("Cannot reach destination on LAN" + destinationLAN);
            return;
        }

        log.info("Pinging " + destinationIPAddress + " from " + outgoingNetworkInterfaceCard.macAddress() + " with " + count + " packets...");
        for (int i = 0; i < count; i++) {
            PingMessage ping = PingMessage.request(i + 1);
            IPPacket packet = IPPacket.icmp(outgoingNetworkInterfaceCard.ipAddress(), destinationIPAddress, ping.encode());
            sendToLAN(packet, outgoingNetworkInterfaceCard);
            if (i < count - 1) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void handleIDSCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: ids <on|off|mode <active|passive>|status>");
            return;
        }
        switch (parts[1].toLowerCase()) {
            case "on" -> { IDS.setEnabled(true); log.info("IDS ON — monitoring traffic"); }
            case "off" -> { IDS.setEnabled(false); log.info("IDS OFF — no monitoring"); }
            case "mode" -> {
                if (parts.length >= 3) {
                    IDS.setActiveMode("active".equalsIgnoreCase(parts[2]));
                    log.info("IDS mode: " + (IDS.isActiveMode() ? "ACTIVE (drop suspicious)" : "PASSIVE (log only)"));
                } else {
                    System.out.println("Usage: IDS mode <active|passive>");
                }
            }
            case "status" -> IDS.printStatus();
            default -> System.out.println("Unknown IDS command. Use: on, off, mode, status");
        }
    }

    private void printHelp() {
        System.out.println("\n--- Router Commands ---");
        System.out.println("  ping <destIP> [count <n>]      - Send ping(s)");
        System.out.println("  ids on|off                     - Enable/disable IDS");
        System.out.println("  ids mode active|passive        - Set IDS mode");
        System.out.println("  ids status                     - Show IDS status");
        System.out.println("  info                           - Show interface info");
        System.out.println("  help                           - Show this help");
        System.out.println("  quit                           - Exit");
    }

    private void printInfo() {
        System.out.println("  R1: " + networkInterfaceCardRouter1);
        System.out.println("  R2: " + networkInterfaceCardRouter2);
        System.out.println("  R3: " + networkInterfaceCardRouter3);
    }

    public static void main(String[] args) throws IOException {
        new Router().start();
    }
}
