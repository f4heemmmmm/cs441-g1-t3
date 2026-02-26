package netemu.device;

import netemu.common.*;
import netemu.dashboard.EventReporter;

import static netemu.common.Ansi.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Router with three interfaces (R1 on LAN1, R2 on LAN2, R3 on LAN3).
 * Forwards IP packets between LANs. Includes IDS engine.
 */
public class RouterR {

    private static final int BUF_SIZE = 1024;

    private final Log log = new Log("ROUTER");
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final IdsEngine ids = new IdsEngine(log);

    // Three interfaces
    private final NetworkInterface r1 = new NetworkInterface(
            new MacAddress("R1"), new IpAddress(0x11), 1,
            AddressTable.LAN1_PORT, AddressTable.ROUTER_R1_PORT);
    private final NetworkInterface r2 = new NetworkInterface(
            new MacAddress("R2"), new IpAddress(0x21), 2,
            AddressTable.LAN2_PORT, AddressTable.ROUTER_R2_PORT);
    private final NetworkInterface r3 = new NetworkInterface(
            new MacAddress("R3"), new IpAddress(0x31), 3,
            AddressTable.LAN3_PORT, AddressTable.ROUTER_R3_PORT);

    private DatagramSocket sock1, sock2, sock3;

    public void start() throws Exception {
        sock1 = new DatagramSocket(r1.localPort());
        sock2 = new DatagramSocket(r2.localPort());
        sock3 = new DatagramSocket(r3.localPort());

        System.out.println(Ansi.banner(
                "Router R - Multi-LAN Gateway",
                "R1: " + r1.ip() + " on LAN1 (port " + r1.localPort() + ")",
                "R2: " + r2.ip() + " on LAN2 (port " + r2.localPort() + ")",
                "R3: " + r3.ip() + " on LAN3 (port " + r3.localPort() + ")",
                "IDS: disabled  |  Type 'help' for commands"
        ));
        System.out.println();
        log.info("Router started with 3 interfaces:");
        log.info("  R1 -> " + r1);
        log.info("  R2 -> " + r2);
        log.info("  R3 -> " + r3);

        // Register all three interfaces with their LANs
        register(sock1, r1);
        register(sock2, r2);
        register(sock3, r3);

        // Start receiver threads for each interface
        startReceiver(sock1, r1, "R1-recv");
        startReceiver(sock2, r2, "R2-recv");
        startReceiver(sock3, r3, "R3-recv");

        // CLI
        cliLoop();

        sock1.close();
        sock2.close();
        sock3.close();
        log.info("Router stopped.");
    }

    private void register(DatagramSocket sock, NetworkInterface nic) throws IOException {
        String msg = "REGISTER " + nic.mac().value() + " " + nic.localPort();
        byte[] data = msg.getBytes();
        DatagramPacket pkt = new DatagramPacket(data, data.length,
                InetAddress.getByName("localhost"), nic.lanEmuPort());
        sock.send(pkt);
        log.info("Registered " + nic.mac() + " with LAN" + nic.lanId() +
                " emulator (port " + nic.lanEmuPort() + ")");
    }

    private void startReceiver(DatagramSocket sock, NetworkInterface nic, String name) {
        Thread t = new Thread(() -> receiveLoop(sock, nic), name);
        t.setDaemon(true);
        t.start();
    }

    private void receiveLoop(DatagramSocket sock, NetworkInterface nic) {
        byte[] buf = new byte[BUF_SIZE];
        while (running.get()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                sock.receive(pkt);
                byte[] raw = new byte[pkt.getLength()];
                System.arraycopy(buf, 0, raw, 0, pkt.getLength());
                handleFrame(raw, nic);
            } catch (IOException e) {
                if (running.get()) {
                    log.warn(nic.mac() + " receive error: " + e.getMessage());
                }
            }
        }
    }

    private void handleFrame(byte[] raw, NetworkInterface ingressNic) {
        EthernetFrame frame;
        try {
            frame = EthernetFrame.decode(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed EthernetFrame on " + ingressNic.mac() + ": " + e.getMessage());
            return;
        }

        log.info("Received frame on " + ingressNic.mac() + ": " + frame);

        // MAC filter: only accept frames addressed to this router interface
        if (!frame.dstMac().equals(ingressNic.mac()) && !frame.dstMac().isBroadcast()) {
            log.info("Dropped frame on " + ingressNic.mac() + " (MAC mismatch: dst=" +
                    frame.dstMac() + ", mine=" + ingressNic.mac() + ")");
            return;
        }

        // Parse IP
        IpPacket ip;
        try {
            ip = IpPacket.decode(frame.data());
        } catch (IllegalArgumentException e) {
            log.warn("Malformed IP packet on " + ingressNic.mac() + ": " + e.getMessage());
            return;
        }

        log.info("Accepted packet on " + ingressNic.mac() + ": " + ip);

        // Check if it's addressed to the router itself (ping to router interface)
        if (ip.dstIp().equals(ingressNic.ip())) {
            handleRouterPing(ip, ingressNic);
            return;
        }

        // IDS inspection
        if (ids.inspect(frame, ip)) {
            log.info("Packet dropped by IDS (see IDS alert above)");
            return;
        }

        // Forward
        forwardPacket(ip, ingressNic);
    }

    private void handleRouterPing(IpPacket ip, NetworkInterface nic) {
        if (ip.protocol() == IpPacket.PROTO_PING) {
            try {
                PingMessage ping = PingMessage.decode(ip.data());
                if (ping.isRequest()) {
                    log.info("Received Ping REQUEST for router " + nic.mac() + " from " + ip.srcIp());
                    PingMessage reply = new PingMessage(PingMessage.TYPE_REPLY, ping.seq(), ping.timestamp());
                    IpPacket replyIp = new IpPacket(nic.ip(), ip.srcIp(), IpPacket.PROTO_PING, reply.encode());
                    sendOnLan(replyIp, nic);
                }
            } catch (Exception e) {
                log.warn("Ping error: " + e.getMessage());
            }
        }
    }

    private void forwardPacket(IpPacket ip, NetworkInterface ingressNic) {
        int dstIpVal = ip.dstIp().value();
        int egressLan = AddressTable.ipToLan(dstIpVal);

        // Determine egress interface
        NetworkInterface egressNic = switch (egressLan) {
            case 1 -> r1;
            case 2 -> r2;
            case 3 -> r3;
            default -> null;
        };

        if (egressNic == null) {
            log.warn("No route found for destination " + ip.dstIp() + " (target LAN" + egressLan + ")");
            return;
        }

        // Determine destination MAC
        String dstMac = AddressTable.ipToMac(dstIpVal);
        if (dstMac == null) {
            log.warn("Cannot resolve MAC for destination " + ip.dstIp());
            return;
        }

        log.info("Forwarding: " + ingressNic.mac() + " -> " + egressNic.mac() +
                " (dstMAC=" + dstMac + ") " + ip);

        // Re-encapsulate with new Ethernet header
        EthernetFrame outFrame = new EthernetFrame(
                new MacAddress(dstMac),
                egressNic.mac(),
                ip.encode()
        );

        sendFrame(outFrame, egressNic);
    }

    /** Send an IP packet on the LAN of the given interface, with proper MAC resolution. */
    private void sendOnLan(IpPacket ip, NetworkInterface nic) {
        int dstIpVal = ip.dstIp().value();
        String dstMac = AddressTable.ipToMac(dstIpVal);
        if (dstMac == null) {
            log.warn("Cannot resolve MAC for destination " + ip.dstIp());
            return;
        }
        EthernetFrame frame = new EthernetFrame(
                new MacAddress(dstMac), nic.mac(), ip.encode());
        sendFrame(frame, nic);
    }

    private void sendFrame(EthernetFrame frame, NetworkInterface nic) {
        try {
            DatagramSocket sock = switch (nic.lanId()) {
                case 1 -> sock1;
                case 2 -> sock2;
                case 3 -> sock3;
                default -> throw new IllegalStateException("Unknown LAN");
            };
            byte[] data = frame.encode();
            DatagramPacket pkt = new DatagramPacket(data, data.length,
                    InetAddress.getByName("localhost"), nic.lanEmuPort());
            sock.send(pkt);
            log.info("Sent frame on " + nic.mac() + ": " + frame);
        } catch (IOException e) {
            log.warn("Send error on " + nic.mac() + ": " + e.getMessage());
        }
    }

    private void cliLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running.get()) {
            System.out.print(bold(GREEN, "ROUTER> "));
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if ("quit".equalsIgnoreCase(line)) {
                running.set(false);
                sock1.close();
                sock2.close();
                sock3.close();
                break;
            } else if ("help".equalsIgnoreCase(line)) {
                printHelp();
            } else if (line.startsWith("ids ")) {
                handleIds(line);
            } else if (line.startsWith("ethsend ")) {
                handleEthSend(line);
            } else {
                log.warn("Unknown command: " + line);
            }
        }
    }

    private void handleIds(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            log.warn("Usage: ids on|off|mode|status|thresholds");
            return;
        }
        String sub = parts[1].toLowerCase();
        switch (sub) {
            case "on" -> {
                ids.setEnabled(true);
                log.info("IDS enabled");
            }
            case "off" -> {
                ids.setEnabled(false);
                log.info("IDS disabled");
            }
            case "mode" -> {
                if (parts.length >= 3) {
                    if ("passive".equalsIgnoreCase(parts[2])) {
                        ids.setActiveMode(false);
                        log.info("IDS mode: PASSIVE (alert only)");
                    } else if ("active".equalsIgnoreCase(parts[2])) {
                        ids.setActiveMode(true);
                        log.info("IDS mode: ACTIVE (alert + block)");
                    }
                } else {
                    log.warn("Usage: ids mode passive|active");
                }
            }
            case "status" -> ids.printStatus();
            case "thresholds" -> {
                // ids thresholds pingflood <count> per <seconds>
                if (parts.length >= 6 && "pingflood".equalsIgnoreCase(parts[2]) && "per".equalsIgnoreCase(parts[4])) {
                    int count = Integer.parseInt(parts[3]);
                    int sec = Integer.parseInt(parts[5]);
                    ids.setFloodThreshold(count, sec);
                } else {
                    log.warn("Usage: ids thresholds pingflood <count> per <seconds>");
                }
            }
            default -> log.warn("Unknown ids command: " + sub);
        }
    }

    private void handleEthSend(String line) {
        // ethsend lan1|lan2|lan3 dst <mac> msg "<text>"
        String[] parts = line.split("\\s+", 6);
        if (parts.length < 6) {
            log.warn("Usage: ethsend lan1|lan2|lan3 dst <mac> msg \"<text>\"");
            return;
        }
        String lanStr = parts[1].toLowerCase();
        String dstMac = parts[3];
        String msgText = parts[5];
        // Strip surrounding quotes if present
        if (msgText.startsWith("\"") && msgText.endsWith("\"")) {
            msgText = msgText.substring(1, msgText.length() - 1);
        }

        NetworkInterface nic = switch (lanStr) {
            case "lan1" -> r1;
            case "lan2" -> r2;
            case "lan3" -> r3;
            default -> null;
        };

        if (nic == null) {
            log.warn("Unknown LAN: " + lanStr);
            return;
        }

        // Build a simple data IP packet (proto=0) from the router interface
        byte[] msgBytes = msgText.getBytes();
        IpPacket ip = new IpPacket(nic.ip(), new IpAddress(0x00), IpPacket.PROTO_DATA, msgBytes);
        EthernetFrame frame = new EthernetFrame(
                new MacAddress(dstMac), nic.mac(), ip.encode());
        sendFrame(frame, nic);
        log.info("Sent raw Ethernet frame to MAC=" + dstMac + " via " + lanStr.toUpperCase());
    }

    private void printHelp() {
        System.out.println(bold(WHITE, "Router Commands:"));
        System.out.println(Ansi.helpEntry("ids on|off", "Enable/disable IDS"));
        System.out.println(Ansi.helpEntry("ids mode passive|active", "Set IDS mode"));
        System.out.println(Ansi.helpEntry("ids status", "Show IDS status"));
        System.out.println(Ansi.helpEntry("ids thresholds pingflood <n> per <s>", "Set flood threshold"));
        System.out.println(Ansi.helpEntry("ethsend <lan> dst <mac> msg \"<text>\"", "Send raw Ethernet frame"));
        System.out.println(Ansi.helpEntry("quit", "Exit router"));
    }

    public static void main(String[] args) throws Exception {
        EventReporter.init("ROUTER");
        new RouterR().start();
    }
}
