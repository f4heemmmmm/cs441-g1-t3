package netemu.lan;

import netemu.common.AddressTable;
import netemu.dashboard.EventReporter;

public class Lan1Emu {
    public static void main(String[] args) throws Exception {
        EventReporter.init("LAN1");
        new LanEmulator(1, AddressTable.LAN1_PORT).start();
    }
}
