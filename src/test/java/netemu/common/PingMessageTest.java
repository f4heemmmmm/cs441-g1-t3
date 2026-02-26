package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PingMessageTest {

    @Test
    void encodeDecodeRequestRoundtrip() {
        long ts = System.nanoTime();
        PingMessage msg = new PingMessage(PingMessage.TYPE_REQUEST, 42, ts);

        byte[] encoded = msg.encode();
        assertEquals(PingMessage.SIZE, encoded.length);

        PingMessage decoded = PingMessage.decode(encoded);
        assertEquals(PingMessage.TYPE_REQUEST, decoded.type());
        assertEquals(42, decoded.seq());
        assertEquals(ts, decoded.timestamp());
        assertTrue(decoded.isRequest());
        assertFalse(decoded.isReply());
    }

    @Test
    void encodeDecodeReplyRoundtrip() {
        long ts = 123456789L;
        PingMessage msg = new PingMessage(PingMessage.TYPE_REPLY, 0, ts);

        byte[] encoded = msg.encode();
        PingMessage decoded = PingMessage.decode(encoded);

        assertEquals(PingMessage.TYPE_REPLY, decoded.type());
        assertEquals(0, decoded.seq());
        assertEquals(ts, decoded.timestamp());
        assertTrue(decoded.isReply());
    }

    @Test
    void encodeDecodeMaxSeq() {
        PingMessage msg = new PingMessage(PingMessage.TYPE_REQUEST, 255, 0L);
        byte[] encoded = msg.encode();
        PingMessage decoded = PingMessage.decode(encoded);
        assertEquals(255, decoded.seq());
    }

    @Test
    void decodeTooShort() {
        byte[] buf = new byte[5]; // less than SIZE=10
        buf[0] = PingMessage.TYPE_REQUEST;
        assertThrows(IllegalArgumentException.class, () -> PingMessage.decode(buf));
    }

    @Test
    void decodeNullBuffer() {
        assertThrows(IllegalArgumentException.class, () -> PingMessage.decode(null));
    }

    @Test
    void invalidType() {
        assertThrows(IllegalArgumentException.class,
                () -> new PingMessage((byte) 0x03, 0, 0L));
    }

    @Test
    void seqOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new PingMessage(PingMessage.TYPE_REQUEST, 256, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new PingMessage(PingMessage.TYPE_REQUEST, -1, 0L));
    }

    @Test
    void timestampPreservesSign() {
        long ts = -1L; // all bits set
        PingMessage msg = new PingMessage(PingMessage.TYPE_REQUEST, 1, ts);
        byte[] encoded = msg.encode();
        PingMessage decoded = PingMessage.decode(encoded);
        assertEquals(ts, decoded.timestamp());
    }

    @Test
    void fullStackRoundtrip() {
        // Ping inside IP inside Ethernet
        PingMessage ping = new PingMessage(PingMessage.TYPE_REQUEST, 7, System.nanoTime());
        IpPacket ip = new IpPacket(
                new IpAddress(0x12), new IpAddress(0x22), IpPacket.PROTO_PING, ping.encode());
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("R1"), new MacAddress("N1"), ip.encode());

        byte[] wire = frame.encode();
        EthernetFrame decodedFrame = EthernetFrame.decode(wire);
        IpPacket decodedIp = IpPacket.decode(decodedFrame.data());
        PingMessage decodedPing = PingMessage.decode(decodedIp.data());

        assertEquals(7, decodedPing.seq());
        assertEquals(ping.timestamp(), decodedPing.timestamp());
        assertTrue(decodedPing.isRequest());
    }

    @Test
    void toStringContainsInfo() {
        PingMessage msg = new PingMessage(PingMessage.TYPE_REQUEST, 5, 100L);
        String s = msg.toString();
        assertTrue(s.contains("REQUEST"));
        assertTrue(s.contains("5"));
    }
}
