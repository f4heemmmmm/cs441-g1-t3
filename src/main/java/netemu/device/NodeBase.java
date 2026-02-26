package netemu.device;

import netemu.common.*;
import static netemu.common.Ansi.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for network nodes. Provides:
 * - UDP socket management and LAN emulator registration
 * - Receive loop with Ethernet frame decoding and MAC filtering
 * - Frame sending via LAN emulator
 * - Ping send/receive
 *
 * Subclasses override {@link #handleCli(String)} and {@link #onIpPacket(EthernetFrame, IpPacket)}
 * for node-specific behavior.
 */
public abstract class NodeBase {

    protected static final int BUF_SIZE = 1024;

    protected final NetworkInterface nic;
    protected final Log log;
    protected final AtomicBoolean running = new AtomicBoolean(true);
    protected DatagramSocket socket;

    // Spoofing (only used by Node1, but field is here for simplicity)
    protected volatile boolean spoofEnabled = false;
    protected volatile int spoofIp = 0;

    // Sniffing (only used by Node1)
    protected volatile boolean sniffEnabled = false;

    public NodeBase(NetworkInterface nic) {
        this.nic = nic;
        this.log = new Log(nic.mac().value());
    }

    public void start() throws Exception {
        socket = new DatagramSocket(nic.localPort());
        System.out.println(Ansi.banner(
                nic.mac().value() + " - Network Node",
                "IP: " + nic.ip() + "  MAC: " + nic.mac().value() + "  LAN" + nic.lanId(),
                "Local port: " + nic.localPort(),
                "Type 'help' for commands"
        ));
        System.out.println();
        log.info("Node started: " + nic);

        // Register with LAN emulator
        register();

        // Receiver thread
        Thread recv = new Thread(this::receiveLoop, nic.mac() + "-recv");
        recv.setDaemon(true);
        recv.start();

        // CLI loop (blocking, on main thread)
        cliLoop();

        socket.close();
        log.info("Node stopped.");
    }

    private void register() throws IOException {
        String msg = "REGISTER " + nic.mac().value() + " " + nic.localPort();
        byte[] data = msg.getBytes();
        DatagramPacket pkt = new DatagramPacket(data, data.length,
                InetAddress.getByName("localhost"), nic.lanEmuPort());
        socket.send(pkt);
        log.info("Registered with LAN" + nic.lanId() + " emulator (port " + nic.lanEmuPort() + ")");
    }

    private void receiveLoop() {
        byte[] buf = new byte[BUF_SIZE];
        while (running.get()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                byte[] raw = new byte[pkt.getLength()];
                System.arraycopy(buf, 0, raw, 0, pkt.getLength());
                processFrame(raw);
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("Receive error: " + e.getMessage());
                }
            }
        }
    }

    private void processFrame(byte[] raw) {
        EthernetFrame frame;
        try {
            frame = EthernetFrame.decode(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed EthernetFrame: " + e.getMessage());
            return;
        }

        log.info("Received frame: " + frame);

        // Sniffing: always show everything when enabled
        if (sniffEnabled) {
            try {
                IpPacket ip = IpPacket.decode(frame.data());
                log.sniff(frame + " | " + ip);
            } catch (Exception e) {
                log.sniff(frame + " (not a valid IP packet)");
            }
        }

        // MAC filtering
        boolean myFrame = frame.dstMac().equals(nic.mac()) || frame.dstMac().isBroadcast();
        if (!myFrame) {
            log.info("Dropped frame (MAC mismatch: dst=" + frame.dstMac() +
                    ", mine=" + nic.mac() + ")");
            return;
        }

        // Parse IP
        IpPacket ip;
        try {
            ip = IpPacket.decode(frame.data());
        } catch (IllegalArgumentException e) {
            log.warn("Malformed IP packet: " + e.getMessage());
            return;
        }

        log.info("Accepted packet: " + ip);
        onIpPacket(frame, ip);
    }

    /**
     * Called when a frame passes MAC filter and IP is decoded.
     * Override in subclasses for custom behavior (firewall, etc.).
     */
    protected void onIpPacket(EthernetFrame frame, IpPacket ip) {
        handleIp(ip);
    }

    protected void handleIp(IpPacket ip) {
        if (ip.protocol() == IpPacket.PROTO_PING) {
            handlePing(ip);
        } else {
            log.info("Received data message: \"" + new String(ip.data()) + "\"");
        }
    }

    private void handlePing(IpPacket ip) {
        try {
            PingMessage ping = PingMessage.decode(ip.data());
            log.info("Received " + ping + " from " + ip.srcIp());

            if (ping.isRequest()) {
                // Send reply
                PingMessage reply = new PingMessage(PingMessage.TYPE_REPLY, ping.seq(), ping.timestamp());
                IpPacket replyIp = new IpPacket(
                        nic.ip(),
                        ip.srcIp(),
                        IpPacket.PROTO_PING,
                        reply.encode()
                );
                sendIpPacket(replyIp);
                log.info("Sent Ping REPLY to " + ip.srcIp());
            } else {
                // Reply received
                long rtt = (System.nanoTime() - ping.timestamp()) / 1_000_000;
                log.info("Ping reply from " + ip.srcIp() + ": seq=" + ping.seq() + ", RTT=" + rtt + "ms");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Malformed PingMessage: " + e.getMessage());
        }
    }

    /** Send an IP packet, determining MAC addresses based on routing. */
    protected void sendIpPacket(IpPacket ip) {
        int dstIpVal = ip.dstIp().value();
        int dstLan = AddressTable.ipToLan(dstIpVal);
        int myLan = nic.lanId();

        MacAddress dstMac;
        if (dstLan == myLan) {
            // Same LAN: direct delivery
            String mac = AddressTable.ipToMac(dstIpVal);
            if (mac == null) {
                log.warn("Cannot resolve MAC for destination IP " + ip.dstIp());
                return;
            }
            dstMac = new MacAddress(mac);
        } else {
            // Different LAN: send to router
            String routerMac = AddressTable.lanToRouterMac(myLan);
            if (routerMac == null) {
                log.warn("No router interface found for LAN" + myLan);
                return;
            }
            dstMac = new MacAddress(routerMac);
        }

        // Source IP: use spoofed IP if enabled
        IpPacket actualIp = ip;
        if (spoofEnabled) {
            actualIp = new IpPacket(new IpAddress(spoofIp), ip.dstIp(), ip.protocol(), ip.data());
        }

        EthernetFrame frame = new EthernetFrame(dstMac, nic.mac(), actualIp.encode());
        sendFrame(frame);
    }

    /** Send a raw Ethernet frame to our LAN emulator. */
    protected void sendFrame(EthernetFrame frame) {
        try {
            byte[] data = frame.encode();
            DatagramPacket pkt = new DatagramPacket(data, data.length,
                    InetAddress.getByName("localhost"), nic.lanEmuPort());
            socket.send(pkt);
            log.info("Sent frame: " + frame);
        } catch (IOException e) {
            log.warn("Send error: " + e.getMessage());
        }
    }

    private void cliLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running.get()) {
            System.out.print(bold(GREEN, nic.mac() + "> "));
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if ("quit".equalsIgnoreCase(line)) {
                running.set(false);
                socket.close();
                break;
            } else if ("help".equalsIgnoreCase(line)) {
                printHelp();
            } else if (line.startsWith("ping ")) {
                handlePingCmd(line);
            } else {
                handleCli(line);
            }
        }
    }

    private void handlePingCmd(String line) {
        // ping <dstIpHex> count <n>
        String[] parts = line.split("\\s+");
        if (parts.length < 4 || !"count".equalsIgnoreCase(parts[2])) {
            log.warn("Usage: ping <dstIpHex> count <n>");
            return;
        }
        try {
            int dstIp = ByteUtil.parseHexByte(parts[1]);
            int count = Integer.parseInt(parts[3]);
            for (int i = 0; i < count; i++) {
                PingMessage req = new PingMessage(PingMessage.TYPE_REQUEST, i & 0xFF, System.nanoTime());
                IpPacket ip = new IpPacket(nic.ip(), new IpAddress(dstIp), IpPacket.PROTO_PING, req.encode());
                sendIpPacket(ip);
                log.info("Sent Ping REQUEST #" + i + " to " + ByteUtil.hexByte(dstIp));
                if (i < count - 1) {
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            log.warn("Ping error: " + e.getMessage());
        }
    }

    /** Override to handle node-specific CLI commands. */
    protected abstract void handleCli(String line);

    protected void printHelp() {
        System.out.println(bold(WHITE, "Commands:"));
        System.out.println(Ansi.helpEntry("ping <ip> count <n>", "Send ICMP ping(s)"));
        System.out.println(Ansi.helpEntry("quit", "Exit node"));
    }
}
