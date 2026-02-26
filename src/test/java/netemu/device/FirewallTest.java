package netemu.device;

import netemu.common.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FirewallTest {

    private final Log log = new Log("TEST");

    @Test
    void allowByDefault() {
        Firewall fw = new Firewall(log);
        IpPacket pkt = new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 1, new byte[0]);
        assertFalse(fw.check(pkt));
    }

    @Test
    void blockSrcIp() {
        Firewall fw = new Firewall(log);
        fw.addBlockSrc(0x13);
        IpPacket pkt = new IpPacket(new IpAddress(0x13), new IpAddress(0x22), 1, new byte[0]);
        assertTrue(fw.check(pkt));
    }

    @Test
    void blockDoesNotAffectOtherIps() {
        Firewall fw = new Firewall(log);
        fw.addBlockSrc(0x13);
        IpPacket pkt = new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 1, new byte[0]);
        assertFalse(fw.check(pkt));
    }

    @Test
    void deleteRule() {
        Firewall fw = new Firewall(log);
        fw.addBlockSrc(0x13);
        fw.delBlockSrc(0x13);
        IpPacket pkt = new IpPacket(new IpAddress(0x13), new IpAddress(0x22), 1, new byte[0]);
        assertFalse(fw.check(pkt));
    }

    @Test
    void multipleRules() {
        Firewall fw = new Firewall(log);
        fw.addBlockSrc(0x12);
        fw.addBlockSrc(0x13);

        assertTrue(fw.check(new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 1, new byte[0])));
        assertTrue(fw.check(new IpPacket(new IpAddress(0x13), new IpAddress(0x22), 1, new byte[0])));
        assertFalse(fw.check(new IpPacket(new IpAddress(0x32), new IpAddress(0x22), 1, new byte[0])));
    }
}
