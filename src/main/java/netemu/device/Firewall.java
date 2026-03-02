package netemu.device;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import netemu.common.Log;
import netemu.common.IPPacket;
import netemu.common.IPAddress;


public class Firewall {

    private final Log log;
    private volatile boolean enabled = true;
    private final Set<IPAddress> blockedSourceIPAddressesList = new CopyOnWriteArraySet<IPAddress>();

    public Firewall(Log log) {
        this.log = log;
    }

    // Add a source IP Address to the list of blocked source IP Addresses
    public void blockSourceIPAddress(IPAddress ipAddress) {
        blockedSourceIPAddressesList.add(ipAddress);
        log.info("Firewall rule added: block all packets from " + ipAddress);
    }

    // Remove a source IP Address from the list of blocked source IP Addresses
    public void unblockSourceIPAddress(IPAddress ipAddress) {
        blockedSourceIPAddressesList.remove(ipAddress);
        log.info("Firewall rule removed: unblocked " + ipAddress);
    }

    // Check if packet should be dropped based on blocked IP Addresses - Returns TRUE if blocked
    public boolean shouldBlock(IPPacket ipPacket) {
        if (!enabled) return false;
        if (blockedSourceIPAddressesList.contains(ipPacket.sourceIPAddress())) {
            log.security("Firewall dropped packet: " + ipPacket.sourceIPAddress() + " -> " + ipPacket.destinationIPAddress());
            return true;
        }
        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<IPAddress> blockedSourceIPAddressesList() {
        return Set.copyOf(blockedSourceIPAddressesList);
    }

    public void printStatus() {
        System.out.println("  Firewall:    " + (enabled ? "ON (filtering)" : "OFF (disabled)"));
        if (blockedSourceIPAddressesList.isEmpty()) {
            System.out.println("  Blocked IPs: (none)");
        } else {
            System.out.println("  Blocked IPs:");
            for (IPAddress ipAddress : blockedSourceIPAddressesList) {
                System.out.println("    - " + ipAddress);
            }
        }
    }
}
