package netemu.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTableTest {

    @Test
    void lanPortsAreDistinct() {
        assertNotEquals(AddressTable.LAN1_PORT, AddressTable.LAN2_PORT);
        assertNotEquals(AddressTable.LAN2_PORT, AddressTable.LAN3_PORT);
        assertNotEquals(AddressTable.LAN1_PORT, AddressTable.LAN3_PORT);
    }

    @Test
    void lanPortsMatchExpected() {
        assertEquals(5001, AddressTable.LAN1_PORT);
        assertEquals(5002, AddressTable.LAN2_PORT);
        assertEquals(5003, AddressTable.LAN3_PORT);
    }

    @Test
    void routerPortsMatchExpected() {
        assertEquals(6011, AddressTable.ROUTER1_PORT);
        assertEquals(6012, AddressTable.ROUTER2_PORT);
        assertEquals(6013, AddressTable.ROUTER3_PORT);
    }

    @Test
    void nodePortsMatchExpected() {
        assertEquals(6001, AddressTable.NODE1_PORT);
        assertEquals(6002, AddressTable.NODE2_PORT);
        assertEquals(6003, AddressTable.NODE3_PORT);
        assertEquals(6004, AddressTable.NODE4_PORT);
    }

    @Test
    void resolveNodeIPs() {
        assertEquals(AddressTable.MAC_N1, AddressTable.resolve(AddressTable.IP_N1));
        assertEquals(AddressTable.MAC_N2, AddressTable.resolve(AddressTable.IP_N2));
        assertEquals(AddressTable.MAC_N3, AddressTable.resolve(AddressTable.IP_N3));
        assertEquals(AddressTable.MAC_N4, AddressTable.resolve(AddressTable.IP_N4));
    }

    @Test
    void resolveRouterIPs() {
        assertEquals(AddressTable.MAC_R1, AddressTable.resolve(AddressTable.IP_R1));
        assertEquals(AddressTable.MAC_R2, AddressTable.resolve(AddressTable.IP_R2));
        assertEquals(AddressTable.MAC_R3, AddressTable.resolve(AddressTable.IP_R3));
    }

    @Test
    void resolveUnknownIPThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressTable.resolve(new IPAddress(0xFF)));
    }

    @Test
    void getLANPortNumber() {
        assertEquals(5001, AddressTable.getLANPortNumber(1));
        assertEquals(5002, AddressTable.getLANPortNumber(2));
        assertEquals(5003, AddressTable.getLANPortNumber(3));
    }

    @Test
    void getLANPortNumberUnknownThrows() {
        assertThrows(IllegalArgumentException.class, () -> AddressTable.getLANPortNumber(4));
    }

    @Test
    void getLANForIP() {
        assertEquals(1, AddressTable.getLANForIP(AddressTable.IP_N1));
        assertEquals(1, AddressTable.getLANForIP(AddressTable.IP_N2));
        assertEquals(1, AddressTable.getLANForIP(AddressTable.IP_R1));
        assertEquals(2, AddressTable.getLANForIP(AddressTable.IP_N3));
        assertEquals(2, AddressTable.getLANForIP(AddressTable.IP_R2));
        assertEquals(3, AddressTable.getLANForIP(AddressTable.IP_N4));
        assertEquals(3, AddressTable.getLANForIP(AddressTable.IP_R3));
    }

    @Test
    void getLANForIPUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressTable.getLANForIP(new IPAddress(0xFF)));
    }

    @Test
    void getRouterMACAddressForLAN() {
        assertEquals(AddressTable.MAC_R1, AddressTable.getRouterMACAddressForLAN(1));
        assertEquals(AddressTable.MAC_R2, AddressTable.getRouterMACAddressForLAN(2));
        assertEquals(AddressTable.MAC_R3, AddressTable.getRouterMACAddressForLAN(3));
    }

    @Test
    void getRouterMACAddressForLANUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressTable.getRouterMACAddressForLAN(0));
    }

    @Test
    void getRouterIPAddressForLAN() {
        assertEquals(AddressTable.IP_R1, AddressTable.getRouterIPAddressForLAN(1));
        assertEquals(AddressTable.IP_R2, AddressTable.getRouterIPAddressForLAN(2));
        assertEquals(AddressTable.IP_R3, AddressTable.getRouterIPAddressForLAN(3));
    }

    @Test
    void getRouterIPAddressForLANUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressTable.getRouterIPAddressForLAN(99));
    }

    @Test
    void ipAddressesMatchExpected() {
        assertEquals(0x12, AddressTable.IP_N1.value());
        assertEquals(0x13, AddressTable.IP_N2.value());
        assertEquals(0x22, AddressTable.IP_N3.value());
        assertEquals(0x32, AddressTable.IP_N4.value());
        assertEquals(0x11, AddressTable.IP_R1.value());
        assertEquals(0x21, AddressTable.IP_R2.value());
        assertEquals(0x31, AddressTable.IP_R3.value());
    }

    @Test
    void macAddressesMatchExpected() {
        assertEquals("N1", AddressTable.MAC_N1.value());
        assertEquals("N2", AddressTable.MAC_N2.value());
        assertEquals("N3", AddressTable.MAC_N3.value());
        assertEquals("N4", AddressTable.MAC_N4.value());
        assertEquals("R1", AddressTable.MAC_R1.value());
        assertEquals("R2", AddressTable.MAC_R2.value());
        assertEquals("R3", AddressTable.MAC_R3.value());
    }
}
