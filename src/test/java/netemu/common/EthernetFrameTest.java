package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EthernetFrameTest {

    @Test
    void encodeDecodeRoundtrip() {
        MacAddress dst = new MacAddress("N1");
        MacAddress src = new MacAddress("R1");
        byte[] data = "Hello".getBytes();
        EthernetFrame frame = new EthernetFrame(dst, src, data);

        byte[] encoded = frame.encode();
        EthernetFrame decoded = EthernetFrame.decode(encoded);

        assertEquals("N1", decoded.dstMac().value());
        assertEquals("R1", decoded.srcMac().value());
        assertArrayEquals(data, decoded.data());
        assertEquals(5, decoded.dataLen());
    }

    @Test
    void encodeDecodeEmptyData() {
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("N2"), new MacAddress("N3"), new byte[0]);
        byte[] encoded = frame.encode();
        EthernetFrame decoded = EthernetFrame.decode(encoded);
        assertEquals(0, decoded.dataLen());
        assertEquals("N2", decoded.dstMac().value());
    }

    @Test
    void encodeDecodeMaxData() {
        byte[] data = new byte[255];
        for (int i = 0; i < 255; i++) data[i] = (byte) i;
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("FF"), new MacAddress("N4"), data);
        byte[] encoded = frame.encode();
        EthernetFrame decoded = EthernetFrame.decode(encoded);
        assertEquals(255, decoded.dataLen());
        assertArrayEquals(data, decoded.data());
    }

    @Test
    void decodeTooShort() {
        byte[] buf = new byte[3]; // less than HEADER_SIZE=5
        assertThrows(IllegalArgumentException.class, () -> EthernetFrame.decode(buf));
    }

    @Test
    void decodeDataTruncated() {
        byte[] buf = new byte[]{
                'N', '1', 'R', '1',
                10  // claims 10 bytes data but buffer ends here
        };
        assertThrows(IllegalArgumentException.class, () -> EthernetFrame.decode(buf));
    }

    @Test
    void decodeNullBuffer() {
        assertThrows(IllegalArgumentException.class, () -> EthernetFrame.decode(null));
    }

    @Test
    void constructorRejectsNullMacs() {
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(null, new MacAddress("N1"), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(new MacAddress("N1"), null, new byte[0]));
    }

    @Test
    void constructorRejectsOversizedData() {
        byte[] data = new byte[256]; // max is 255
        assertThrows(IllegalArgumentException.class,
                () -> new EthernetFrame(new MacAddress("N1"), new MacAddress("N2"), data));
    }

    @Test
    void toStringContainsInfo() {
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("N1"), new MacAddress("R2"), new byte[]{1, 2, 3});
        String s = frame.toString();
        assertTrue(s.contains("N1"));
        assertTrue(s.contains("R2"));
        assertTrue(s.contains("3"));
    }

    @Test
    void dataIsCopied() {
        byte[] data = {1, 2, 3};
        EthernetFrame frame = new EthernetFrame(
                new MacAddress("N1"), new MacAddress("N2"), data);
        data[0] = 99; // mutate original
        assertNotEquals(99, frame.data()[0]); // frame should be unaffected
    }
}
