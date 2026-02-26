package netemu.device;

import netemu.common.ByteUtil;
import netemu.common.IpPacket;
import netemu.common.Log;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * IP-layer firewall: blocks incoming packets by source IP.
 */
public class Firewall {

    private final Log log;
    private final Set<Integer> blockedSrcIps = new CopyOnWriteArraySet<>();

    public Firewall(Log log) {
        this.log = log;
    }

    /** Returns true if packet should be dropped. */
    public boolean check(IpPacket pkt) {
        if (blockedSrcIps.contains(pkt.srcIp().value())) {
            log.firewall("Blocked packet: src=" + pkt.srcIp() + ", dst=" + pkt.dstIp() +
                    ", proto=" + pkt.protocol());
            return true; // drop
        }
        return false; // allow
    }

    public void addBlockSrc(int ip) {
        blockedSrcIps.add(ip);
        log.info("Firewall rule added: BLOCK packets from src=" + ByteUtil.hexByte(ip));
    }

    public void delBlockSrc(int ip) {
        blockedSrcIps.remove(ip);
        log.info("Firewall rule removed: BLOCK packets from src=" + ByteUtil.hexByte(ip));
    }

    public void list() {
        if (blockedSrcIps.isEmpty()) {
            log.info("Firewall rules: (none)");
        } else {
            log.info("Firewall rules (" + blockedSrcIps.size() + " active):");
            for (int ip : blockedSrcIps) {
                log.info("  BLOCK src=" + ByteUtil.hexByte(ip));
            }
        }
    }
}
