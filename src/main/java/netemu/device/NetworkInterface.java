package netemu.device;

import netemu.common.IPAddress;
import netemu.common.MACAddress;

public record NetworkInterface (
    MACAddress macAddress,
    IPAddress ipAddress,
    int lanID,
    int devicePortNumber,
    int lanEmulatorPortNumber
) {
    @Override
    public String toString() {
        return String.format("Interface [MAC=%s | IP=%s | LAN%d | port %d]", macAddress, ipAddress, lanID, devicePortNumber);
    }
}
