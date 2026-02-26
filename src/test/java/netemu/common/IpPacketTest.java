package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IpPacketTest {

    @Test
    void encodeDecodeRoundtrip() {
        IpAddress src = new IpAddress(0x12);
        IpAddress dst = new IpAddress(0x22);
        byte[] data = "payload".getBytes();
        IpPacket pkt = new IpPacket(src, dst, IpPacket.PROTO_PING, data);

        byte[] encoded = pkt.encode();
        IpPacket decoded = IpPacket.decode(encoded);

        assertEquals(0x12, decoded.srcIp().value());
        assertEquals(0x22, decoded.dstIp().value());
        assertEquals(IpPacket.PROTO_PING, decoded.protocol());
        assertArrayEquals(data, decoded.data());
    }

    @Test
    void encodeDecodeEmptyData() {
        IpPacket pkt = new IpPacket(
                new IpAddress(0x11), new IpAddress(0x31), 0, new byte[0]);
        byte[] encoded = pkt.encode();
        IpPacket decoded = IpPacket.decode(encoded);
        assertEquals(0, decoded.dataLen());
    }

    @Test
    void decodeTooShort() {
        byte[] buf = new byte[2];
        assertThrows(IllegalArgumentException.class, () -> IpPacket.decode(buf));
    }

    @Test
    void decodeDataTruncated() {
        byte[] buf = new byte[]{
                0x12, 0x22, 1, 50 // claims 50 bytes data
        };
        assertThrows(IllegalArgumentException.class, () -> IpPacket.decode(buf));
    }

    @Test
    void decodeNullBuffer() {
        assertThrows(IllegalArgumentException.class, () -> IpPacket.decode(null));
    }

    @Test
    void constructorRejectsNullIps() {
        assertThrows(IllegalArgumentException.class,
                () -> new IpPacket(null, new IpAddress(0x22), 1, new byte[0]));
    }

    @Test
    void protocolRange() {
        // Valid: 0-255
        assertDoesNotThrow(() -> new IpPacket(new IpAddress(1), new IpAddress(2), 0, new byte[0]));
        assertDoesNotThrow(() -> new IpPacket(new IpAddress(1), new IpAddress(2), 255, new byte[0]));
        // Invalid
        assertThrows(IllegalArgumentException.class,
                () -> new IpPacket(new IpAddress(1), new IpAddress(2), 256, new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> new IpPacket(new IpAddress(1), new IpAddress(2), -1, new byte[0]));
    }

    @Test
    void toStringContainsInfo() {
        IpPacket pkt = new IpPacket(new IpAddress(0x12), new IpAddress(0x22), 1, new byte[5]);
        String s = pkt.toString();
        assertTrue(s.contains("0x12"));
        assertTrue(s.contains("0x22"));
        assertTrue(s.contains("1")); // protocol
    }

    @Test
    void insideEthernetFrame() {
        // Test IP packet inside Ethernet frame roundtrip
        IpPacket ip = new IpPacket(new IpAddress(0x12), new IpAddress(0x32), 1, "test".getBytes());
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());

        byte[] wire = frame.encode();
        EthernetFrame decodedFrame = EthernetFrame.decode(wire);
        IpPacket decodedIp = IpPacket.decode(decodedFrame.data());

        assertEquals(0x12, decodedIp.srcIp().value());
        assertEquals(0x32, decodedIp.dstIp().value());
        assertArrayEquals("test".getBytes(), decodedIp.data());
    }
}
