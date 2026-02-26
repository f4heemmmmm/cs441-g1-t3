package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    @Test
    void createValid() {
        IpAddress ip = new IpAddress(0x12);
        assertEquals(0x12, ip.value());
    }

    @Test
    void createOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new IpAddress(-1));
        assertThrows(IllegalArgumentException.class, () -> new IpAddress(256));
    }

    @Test
    void lanId() {
        assertEquals(1, new IpAddress(0x12).lanId());
        assertEquals(2, new IpAddress(0x22).lanId());
        assertEquals(3, new IpAddress(0x32).lanId());
        assertEquals(1, new IpAddress(0x11).lanId());
    }

    @Test
    void encodeDecodeRoundtrip() {
        IpAddress ip = new IpAddress(0x22);
        byte[] buf = new byte[3];
        ip.encode(buf, 1);
        IpAddress decoded = IpAddress.decode(buf, 1);
        assertEquals(ip, decoded);
    }

    @Test
    void equality() {
        assertEquals(new IpAddress(0x12), new IpAddress(0x12));
        assertNotEquals(new IpAddress(0x12), new IpAddress(0x13));
    }

    @Test
    void toStringHex() {
        assertEquals("0x12", new IpAddress(0x12).toString());
        assertEquals("0x00", new IpAddress(0).toString());
        assertEquals("0xff", new IpAddress(255).toString());
    }
}
