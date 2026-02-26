package netemu.device;

import netemu.common.*;
import netemu.dashboard.EventReporter;


/**
 * Node1: attacker node on LAN1.
 * Supports IP spoofing and sniffing.
 */
public class Node1 extends NodeBase {

    public Node1() {
        super(new NetworkInterface(
                new MacAddress("N1"),
                new IpAddress(0x12),
                1,
                AddressTable.LAN1_PORT,
                AddressTable.NODE1_PORT
        ));
    }

    @Override
    protected void handleCli(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "spoof" -> handleSpoof(parts);
            case "sniff" -> handleSniff(parts);
            case "pingflood" -> handlePingFlood(parts);
            default -> log.warn("Unknown command: " + line + "  (try: help)");
        }
    }

    private void handleSpoof(String[] parts) {
        if (parts.length < 2) {
            log.warn("Usage: spoof on <srcIpHex> | spoof off");
            return;
        }
        if ("on".equalsIgnoreCase(parts[1]) && parts.length >= 3) {
            spoofIp = ByteUtil.parseHexByte(parts[2]);
            spoofEnabled = true;
            log.info("Spoofing ON: pretending to be IP " + ByteUtil.hexByte(spoofIp));
        } else if ("off".equalsIgnoreCase(parts[1])) {
            spoofEnabled = false;
            log.info("Spoofing OFF");
        } else {
            log.warn("Usage: spoof on <srcIpHex> | spoof off");
        }
    }

    private void handleSniff(String[] parts) {
        if (parts.length < 2) {
            log.warn("Usage: sniff on | sniff off");
            return;
        }
        if ("on".equalsIgnoreCase(parts[1])) {
            sniffEnabled = true;
            log.info("Sniffing ON: will show all frames on LAN");
        } else if ("off".equalsIgnoreCase(parts[1])) {
            sniffEnabled = false;
            log.info("Sniffing OFF");
        }
    }

    private void handlePingFlood(String[] parts) {
        // pingflood <dstIpHex> rate <r> duration <sec>
        if (parts.length < 5) {
            log.warn("Usage: pingflood <dstIpHex> rate <r> duration <sec>");
            return;
        }
        try {
            int dstIp = ByteUtil.parseHexByte(parts[1]);
            int rate = 20; // default
            int duration = 5; // default
            for (int i = 2; i < parts.length - 1; i++) {
                if ("rate".equalsIgnoreCase(parts[i])) rate = Integer.parseInt(parts[i + 1]);
                if ("duration".equalsIgnoreCase(parts[i])) duration = Integer.parseInt(parts[i + 1]);
            }
            log.info("Starting ping flood to " + ByteUtil.hexByte(dstIp) +
                    " rate=" + rate + "/s duration=" + duration + "s");
            final int r = rate;
            final int d = duration;
            Thread floodThread = new Thread(() -> doPingFlood(dstIp, r, d), "pingflood");
            floodThread.setDaemon(true);
            floodThread.start();
        } catch (Exception e) {
            log.warn("Pingflood error: " + e.getMessage());
        }
    }

    private void doPingFlood(int dstIp, int rate, int durationSec) {
        long end = System.currentTimeMillis() + (durationSec * 1000L);
        long intervalMs = 1000L / rate;
        int seq = 0;
        while (System.currentTimeMillis() < end && running.get()) {
            try {
                PingMessage req = new PingMessage(PingMessage.TYPE_REQUEST, seq & 0xFF, System.nanoTime());
                IpPacket ip = new IpPacket(nic.ip(), new IpAddress(dstIp), IpPacket.PROTO_PING, req.encode());
                sendIpPacket(ip);
                seq++;
                Thread.sleep(intervalMs);
            } catch (Exception e) {
                log.warn("Flood error: " + e.getMessage());
                break;
            }
        }
        log.info("Ping flood complete: sent " + seq + " pings");
    }

    @Override
    protected void printHelp() {
        super.printHelp();
        System.out.println(Ansi.helpEntry("spoof on <srcIpHex>", "Enable IP spoofing"));
        System.out.println(Ansi.helpEntry("spoof off", "Disable IP spoofing"));
        System.out.println(Ansi.helpEntry("sniff on", "Enable promiscuous sniffing"));
        System.out.println(Ansi.helpEntry("sniff off", "Disable sniffing"));
        System.out.println(Ansi.helpEntry("pingflood <ip> rate <r> duration <s>", "Flood target with pings"));
    }

    public static void main(String[] args) throws Exception {
        EventReporter.init("N1");
        new Node1().start();
    }
}
