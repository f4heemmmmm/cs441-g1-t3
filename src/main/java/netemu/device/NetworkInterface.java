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
        return String.format("Network Interface Card[macAddress=%s ipAddress=%s lanID=%d portNumber=%d]", macAddress, ipAddress, lanID, devicePortNumber);
    }
}
