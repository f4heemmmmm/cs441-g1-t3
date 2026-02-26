package netemu.device;

import netemu.common.AddressTable;
import netemu.common.ByteUtil;
import netemu.common.EthernetFrame;
import netemu.common.IpPacket;
import netemu.common.Log;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Intrusion Detection System engine for the router.
 * - Spoof detection: checks MAC-to-IP consistency.
 * - Ping flood detection: sliding window rate limiting.
 * Modes: passive (alert only), active (alert + block).
 */
public class IdsEngine {

    private final Log log;
    private volatile boolean enabled = false;
    private volatile boolean activeMode = false; // false=passive, true=active

    // Ping flood thresholds
    private volatile int floodThreshold = 10;
    private volatile int floodWindowSec = 5;

    // Blocked IPs with expiry time
    private final Map<Integer, Long> blockedIps = new ConcurrentHashMap<>();
    private static final long BLOCK_DURATION_MS = 10_000; // 10 seconds

    // Ping timestamps per srcIP for flood detection
    private final Map<Integer, Queue<Long>> pingTimestamps = new ConcurrentHashMap<>();

    public IdsEngine(Log log) {
        this.log = log;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean on) { this.enabled = on; }
    public boolean isActiveMode() { return activeMode; }
    public void setActiveMode(boolean active) { this.activeMode = active; }

    public void setFloodThreshold(int count, int windowSec) {
        this.floodThreshold = count;
        this.floodWindowSec = windowSec;
        log.info("IDS ping flood threshold set: max " + count + " pings per " + windowSec + "s");
    }

    public void printStatus() {
        log.info("IDS status: " + (enabled ? "ENABLED" : "DISABLED") +
                ", mode=" + (activeMode ? "ACTIVE (alert + block)" : "PASSIVE (alert only)") +
                ", flood threshold=" + floodThreshold + " pings/" + floodWindowSec + "s");
        if (!blockedIps.isEmpty()) {
            log.info("IDS blocked IPs:");
            long now = System.currentTimeMillis();
            blockedIps.forEach((ip, expiry) -> {
                long remaining = (expiry - now) / 1000;
                if (remaining > 0) {
                    log.info("  " + ByteUtil.hexByte(ip) + " (" + remaining + "s remaining)");
                }
            });
        }
    }

    /**
     * Check an incoming frame+packet. Returns true if the packet should be DROPPED.
     */
    public boolean inspect(EthernetFrame frame, IpPacket pkt) {
        if (!enabled) return false;

        // Clean expired blocks
        cleanExpiredBlocks();

        // Check if srcIP is currently blocked (active mode)
        if (activeMode && isBlocked(pkt.srcIp().value())) {
            log.ids("[BLOCK] Dropping srcIP=" + pkt.srcIp() +
                    " (currently blocked by IDS)");
            return true;
        }

        boolean shouldBlock = false;

        // Rule 1: Spoof detection
        shouldBlock |= checkSpoof(frame, pkt);

        // Rule 2: Ping flood detection
        if (pkt.protocol() == IpPacket.PROTO_PING) {
            shouldBlock |= checkPingFlood(pkt);
        }

        // In active mode, block the source
        if (shouldBlock && activeMode) {
            blockIp(pkt.srcIp().value());
            return true; // drop
        }

        return false; // passive mode: alert only, don't drop
    }

    private boolean checkSpoof(EthernetFrame frame, IpPacket pkt) {
        String srcMac = frame.srcMac().value();
        Integer expectedIp = AddressTable.MAC_TO_IP.get(srcMac);
        if (expectedIp != null && expectedIp != pkt.srcIp().value()) {
            log.ids("[ALERT] Spoof suspected: ethSrc=" + srcMac +
                    " expectedIP=" + ByteUtil.hexByte(expectedIp) +
                    " but ipSrc=" + pkt.srcIp() + " dst=" + pkt.dstIp());
            return true;
        }
        return false;
    }

    private boolean checkPingFlood(IpPacket pkt) {
        int srcIp = pkt.srcIp().value();
        Queue<Long> timestamps = pingTimestamps.computeIfAbsent(srcIp, k -> new ConcurrentLinkedQueue<>());
        long now = System.currentTimeMillis();
        timestamps.add(now);

        // Remove old entries outside the window
        long windowStart = now - (floodWindowSec * 1000L);
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        int count = timestamps.size();
        if (count > floodThreshold) {
            log.ids("[ALERT] Ping flood: srcIP=" + pkt.srcIp() +
                    " count=" + count + "/" + floodWindowSec + "s");
            return true;
        }
        return false;
    }

    private boolean isBlocked(int ip) {
        Long expiry = blockedIps.get(ip);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blockedIps.remove(ip);
            return false;
        }
        return true;
    }

    private void blockIp(int ip) {
        long expiry = System.currentTimeMillis() + BLOCK_DURATION_MS;
        blockedIps.put(ip, expiry);
        log.ids("[BLOCK] Dropping srcIP=" + ByteUtil.hexByte(ip) +
                " for " + (BLOCK_DURATION_MS / 1000) + "s");
    }

    private void cleanExpiredBlocks() {
        long now = System.currentTimeMillis();
        blockedIps.entrySet().removeIf(e -> now > e.getValue());
    }
}
