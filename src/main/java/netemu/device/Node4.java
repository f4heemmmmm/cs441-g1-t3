package netemu.device;

import netemu.common.*;
import netemu.dashboard.EventReporter;

/**
 * Node4: normal node on LAN3.
 */
public class Node4 extends NodeBase {

    public Node4() {
        super(new NetworkInterface(
                new MacAddress("N4"),
                new IpAddress(0x32),
                3,
                AddressTable.LAN3_PORT,
                AddressTable.NODE4_PORT
        ));
    }

    @Override
    protected void handleCli(String line) {
        log.warn("Unknown command: " + line + "  (try: help)");
    }

    public static void main(String[] args) throws Exception {
        EventReporter.init("N4");
        new Node4().start();
    }
}
