package netemu.lan;

import netemu.common.AddressTable;
import netemu.dashboard.EventReporter;

public class Lan3Emu {
    public static void main(String[] args) throws Exception {
        EventReporter.init("LAN3");
        new LanEmulator(3, AddressTable.LAN3_PORT).start();
    }
}
