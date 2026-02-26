package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressTableTest {

    @Test
    void ipToLan() {
        assertEquals(1, AddressTable.ipToLan(0x12));
        assertEquals(1, AddressTable.ipToLan(0x13));
        assertEquals(2, AddressTable.ipToLan(0x22));
        assertEquals(3, AddressTable.ipToLan(0x32));
    }

    @Test
    void ipToMac() {
        assertEquals("N1", AddressTable.ipToMac(0x12));
        assertEquals("N2", AddressTable.ipToMac(0x13));
        assertEquals("N3", AddressTable.ipToMac(0x22));
        assertEquals("N4", AddressTable.ipToMac(0x32));
        assertEquals("R1", AddressTable.ipToMac(0x11));
        assertNull(AddressTable.ipToMac(0x99));
    }

    @Test
    void lanToRouterMac() {
        assertEquals("R1", AddressTable.lanToRouterMac(1));
        assertEquals("R2", AddressTable.lanToRouterMac(2));
        assertEquals("R3", AddressTable.lanToRouterMac(3));
        assertNull(AddressTable.lanToRouterMac(4));
    }

    @Test
    void lanToPort() {
        assertEquals(5001, AddressTable.lanToPort(1));
        assertEquals(5002, AddressTable.lanToPort(2));
        assertEquals(5003, AddressTable.lanToPort(3));
        assertThrows(IllegalArgumentException.class, () -> AddressTable.lanToPort(4));
    }

    @Test
    void macToIp() {
        assertEquals(0x12, AddressTable.MAC_TO_IP.get("N1"));
        assertEquals(0x13, AddressTable.MAC_TO_IP.get("N2"));
        assertEquals(0x22, AddressTable.MAC_TO_IP.get("N3"));
        assertEquals(0x32, AddressTable.MAC_TO_IP.get("N4"));
    }
}
