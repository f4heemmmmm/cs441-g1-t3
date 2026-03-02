package netemu.device;

import netemu.common.*;

import java.io.IOException;

/**
 * Node 1
 *  - Role: Attacker in LAN1
 *  - MAC Address: N1
 *  - IP Address: 0x12
 *  - Port Number: 6001
 *  - LAN ID: 1
 */
public class Node1 extends Node {
    
    private volatile boolean spoofing = false;
    private volatile boolean sniffing = false;
    private volatile IPAddress spoofIPAddress = null;

    public Node1() {
        super("Node1", new NetworkInterface(AddressTable.MAC_N1, AddressTable.IP_N1, 1, AddressTable.NODE1_PORT, AddressTable.LAN1_PORT), Ansi.RED);
    }

    @Override
    protected String logColor() {
        return Ansi.RED;
    }

    @Override
    protected void handleFrame(EthernetFrame frame) {
        // SNIFFING MODE - Log all frames
        if (sniffing && !frame.destinationMACAddress().equals(networkInterfaceCard.macAddress())) {
            log.info("[Sniffed] " + frame);

            try {
                IPPacket packet = IPPacket.decode(frame.data());
                log.info("  └─ " + packet);
                if (packet.protocol() == IPPacket.PROTOCOL_ICMP) {
                    PingMessage ping = PingMessage.decode(packet.data());
                    log.info("     └─ " + ping);
                }
            } catch (Exception e) {
                // Invalid IP packet, log the raw frame
            }
        }
        // Normal MAC filtering for processing
        if (frame.destinationMACAddress().equals(networkInterfaceCard.macAddress()) || frame.destinationMACAddress().isBroadcast()) {
            processFrame(frame);
        }
    }

    @Override
    protected void sendIPPacket(IPPacket packet) {
        if (spoofing && spoofIPAddress != null) {
            // Replace source IP Address with spoofed IP Address
            IPPacket spoofedPacket = new IPPacket(spoofIPAddress, packet.destinationIPAddress(), packet.protocol(), packet.data());
            log.warn("Spoofed packet: pretending to be " + spoofIPAddress + " (real IP: " + networkInterfaceCard.ipAddress() + ")");
            super.sendIPPacket(spoofedPacket);
        } else {
            super.sendIPPacket(packet);
        }
    }

    @Override
    protected boolean handleCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        return switch (cmd) {
            case "spoof" -> { handleSpoofCommand(parts); yield true; }
            case "sniff" -> { handleSniffCommand(parts); yield true; }
            case "flood" -> { handleFloodCommand(parts); yield true; }
            default -> super.handleCommand(line);
        };
    }

    private void handleSpoofCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: spoof on <IP> | spoof off");
            return;
        }
        switch (parts[1].toLowerCase()) {
            case "on" -> {
                if (parts.length >= 3) {
                    spoofIPAddress = IPAddress.parse(parts[2]);
                    spoofing = true;
                    log.warn("Spoofing ON — now impersonating " + spoofIPAddress);
                } else {
                    System.out.println("Usage: Spoof on <IP>");
                }
            }
            case "off" -> {
                spoofing = false;
                spoofIPAddress= null;
                log.info("Spoofing OFF — using real IP");
            }
            default -> System.out.println("Usage: Spoof on <IP> | spoof off");
        }
    }

    private void handleSniffCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: sniff on | sniff off");
            return;
        }
        switch (parts[1].toLowerCase()) {
            case "on" -> {
                sniffing = true;
                log.warn("Sniffing ON — capturing all LAN" + networkInterfaceCard.lanID() + " traffic");
            }
            case "off" -> {
                sniffing = false;
                log.info("Sniffing OFF");
            }
            default -> System.out.println("Usage: sniff on | sniff off");
        }
    }

    private void handleFloodCommand(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: flood <destIP> <count>");
            return;
        }
        IPAddress destinationIPAddress = IPAddress.parse(parts[1]);
        int count = Integer.parseInt(parts[2]);

        log.warn("Flood attack: sending " + count + " rapid pings to " + destinationIPAddress);
        for (int i = 0; i < count; i++) {
            PingMessage ping = PingMessage.request(i + 1);
            IPPacket packet = IPPacket.icmp(
                    spoofing && spoofIPAddress != null ? spoofIPAddress : networkInterfaceCard.ipAddress(),
                    destinationIPAddress, ping.encode());
            super.sendIPPacket(packet);
        }
        log.warn("Flood attack complete: " + count + " pings sent");
    }

    @Override
    protected void printHelp() {
        super.printHelp();
        System.out.println("  spoof on <IP> | off        - Enable/disable IP spoofing");
        System.out.println("  sniff on | off             - Enable/disable promiscuous sniffing");
        System.out.println("  flood <destIP> <count>     - Send ping flood");
    }

    public static void main(String[] args) throws IOException {
        new Node1().start();
    }
}
