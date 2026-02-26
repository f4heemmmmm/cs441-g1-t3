package netemu.device;

import netemu.common.*;
import netemu.dashboard.EventReporter;


/**
 * Node3: node on LAN2 with firewall capability.
 */
public class Node3 extends NodeBase {

    private final Firewall firewall;

    public Node3() {
        super(new NetworkInterface(
                new MacAddress("N3"),
                new IpAddress(0x22),
                2,
                AddressTable.LAN2_PORT,
                AddressTable.NODE3_PORT
        ));
        this.firewall = new Firewall(log);
    }

    @Override
    protected void onIpPacket(EthernetFrame frame, IpPacket ip) {
        // Apply firewall before processing
        if (firewall.check(ip)) {
            return; // dropped by firewall
        }
        handleIp(ip);
    }

    @Override
    protected void handleCli(String line) {
        String[] parts = line.split("\\s+");
        if ("fw".equalsIgnoreCase(parts[0])) {
            handleFw(parts);
        } else {
            log.warn("Unknown command: " + line + "  (try: help)");
        }
    }

    private void handleFw(String[] parts) {
        if (parts.length < 2) {
            log.warn("Usage: fw add block src <ipHex> | fw del block src <ipHex> | fw list");
            return;
        }
        String sub = parts[1].toLowerCase();
        switch (sub) {
            case "list" -> firewall.list();
            case "add" -> {
                // fw add block src <ipHex>
                if (parts.length >= 5 && "block".equalsIgnoreCase(parts[2]) && "src".equalsIgnoreCase(parts[3])) {
                    int ip = ByteUtil.parseHexByte(parts[4]);
                    firewall.addBlockSrc(ip);
                } else {
                    log.warn("Usage: fw add block src <ipHex>");
                }
            }
            case "del" -> {
                // fw del block src <ipHex>
                if (parts.length >= 5 && "block".equalsIgnoreCase(parts[2]) && "src".equalsIgnoreCase(parts[3])) {
                    int ip = ByteUtil.parseHexByte(parts[4]);
                    firewall.delBlockSrc(ip);
                } else {
                    log.warn("Usage: fw del block src <ipHex>");
                }
            }
            default -> log.warn("Unknown fw command: " + sub);
        }
    }

    @Override
    protected void printHelp() {
        super.printHelp();
        System.out.println(Ansi.helpEntry("fw add block src <ipHex>", "Block source IP"));
        System.out.println(Ansi.helpEntry("fw del block src <ipHex>", "Unblock source IP"));
        System.out.println(Ansi.helpEntry("fw list", "List firewall rules"));
    }

    public static void main(String[] args) throws Exception {
        EventReporter.init("N3");
        new Node3().start();
    }
}
