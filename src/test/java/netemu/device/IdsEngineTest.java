package netemu.device;

import netemu.common.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdsEngineTest {

    private final Log log = new Log("TEST");

    @Test
    void disabledByDefault() {
        IdsEngine ids = new IdsEngine(log);
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"),
                new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 1, new byte[0]).encode());
        IpPacket ip = IpPacket.decode(frame.data());
        assertFalse(ids.inspect(frame, ip)); // should not drop when disabled
    }

    @Test
    void detectsSpoof() {
        IdsEngine ids = new IdsEngine(log);
        ids.setEnabled(true);
        ids.setActiveMode(false); // passive

        // N1 claims to be IP 0x13 (should be 0x12)
        IpPacket ip = new IpPacket(new IpAddress(0x13), new IpAddress(0x22), 1, new byte[0]);
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());

        // Passive mode: alerts but doesn't drop
        assertFalse(ids.inspect(frame, ip));
    }

    @Test
    void activeModeBlocksSpoof() {
        IdsEngine ids = new IdsEngine(log);
        ids.setEnabled(true);
        ids.setActiveMode(true); // active

        IpPacket ip = new IpPacket(new IpAddress(0x13), new IpAddress(0x22), 1, new byte[0]);
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());

        assertTrue(ids.inspect(frame, ip)); // should drop
    }

    @Test
    void legitimateTrafficNotBlocked() {
        IdsEngine ids = new IdsEngine(log);
        ids.setEnabled(true);
        ids.setActiveMode(true);

        // N1 with correct IP 0x12
        IpPacket ip = new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 0, new byte[0]);
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());

        assertFalse(ids.inspect(frame, ip));
    }

    @Test
    void pingFloodDetection() {
        IdsEngine ids = new IdsEngine(log);
        ids.setEnabled(true);
        ids.setActiveMode(true);
        ids.setFloodThreshold(3, 10); // 3 pings per 10 seconds

        IpAddress src = new IpAddress(0x12);
        IpAddress dst = new IpAddress(0x22);

        // Send 4 pings (should trigger on 4th)
        for (int i = 0; i < 3; i++) {
            PingMessage ping = new PingMessage(PingMessage.TYPE_REQUEST, i, System.nanoTime());
            IpPacket ip = new IpPacket(src, dst, IpPacket.PROTO_PING, ping.encode());
            EthernetFrame frame = new EthernetFrame(
                    new MacAddress("R1"), new MacAddress("N1"), ip.encode());
            ids.inspect(frame, ip);
        }

        // 4th ping should be blocked
        PingMessage ping = new PingMessage(PingMessage.TYPE_REQUEST, 3, System.nanoTime());
        IpPacket ip = new IpPacket(src, dst, IpPacket.PROTO_PING, ping.encode());
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());
        assertTrue(ids.inspect(frame, ip));
    }
}
