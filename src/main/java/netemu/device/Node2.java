package netemu.device;

import netemu.common.*;
import netemu.dashboard.EventReporter;

/**
 * Node2: normal node on LAN1.
 */
public class Node2 extends NodeBase {

    public Node2() {
        super(new NetworkInterface(
                new MacAddress("N2"),
                new IpAddress(0x13),
                1,
                AddressTable.LAN1_PORT,
                AddressTable.NODE2_PORT
        ));
    }

    @Override
    protected void handleCli(String line) {
        log.warn("Unknown command: " + line + "  (try: help)");
    }

    public static void main(String[] args) throws Exception {
        EventReporter.init("N2");
        new Node2().start();
    }
}
