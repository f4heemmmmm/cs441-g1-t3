package netemu.lan;

import netemu.common.AddressTable;
import netemu.dashboard.EventReporter;

public class Lan2Emu {
    public static void main(String[] args) throws Exception {
        EventReporter.init("LAN2");
        new LanEmulator(2, AddressTable.LAN2_PORT).start();
    }
}
