package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MacAddressTest {

    @Test
    void createValid() {
        MacAddress mac = new MacAddress("N1");
        assertEquals("N1", mac.value());
    }

    @Test
    void createInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new MacAddress("A"));
        assertThrows(IllegalArgumentException.class, () -> new MacAddress("ABC"));
        assertThrows(IllegalArgumentException.class, () -> new MacAddress(""));
    }

    @Test
    void createNull() {
        assertThrows(IllegalArgumentException.class, () -> new MacAddress(null));
    }

    @Test
    void encodeDecodeRoundtrip() {
        MacAddress mac = new MacAddress("R2");
        byte[] buf = new byte[4];
        mac.encode(buf, 1);
        MacAddress decoded = MacAddress.decode(buf, 1);
        assertEquals(mac, decoded);
    }

    @Test
    void isBroadcast() {
        assertTrue(new MacAddress("FF").isBroadcast());
        assertTrue(MacAddress.BROADCAST.isBroadcast());
        assertFalse(new MacAddress("N1").isBroadcast());
    }

    @Test
    void equality() {
        assertEquals(new MacAddress("N1"), new MacAddress("N1"));
        assertNotEquals(new MacAddress("N1"), new MacAddress("N2"));
    }

    @Test
    void hashCodeConsistent() {
        assertEquals(new MacAddress("N1").hashCode(), new MacAddress("N1").hashCode());
    }

    @Test
    void toStringReturnsValue() {
        assertEquals("N1", new MacAddress("N1").toString());
    }
}
