package netemu.device;

import netemu.common.MacAddress;
import netemu.common.IpAddress;

/**
 * Represents a network interface: MAC, IP, which LAN it's on, and its local UDP port.
 */
public record NetworkInterface(
        MacAddress mac,
        IpAddress ip,
        int lanId,
        int lanEmuPort,
        int localPort
) {
    @Override
    public String toString() {
        return "Interface " + mac + " (IP=" + ip + ", LAN" + lanId +
                ", emuPort=" + lanEmuPort + ", listenPort=" + localPort + ")";
    }
}
