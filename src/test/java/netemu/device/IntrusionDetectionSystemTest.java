package netemu.device;

import netemu.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntrusionDetectionSystemTest {

    private IntrusionDetectionSystem ids;
    private Log log;
    private NetworkInterface lan1Interface;
    private NetworkInterface lan2Interface;

    @BeforeEach
    void setUp() {
        log = new Log("TestIDS", Ansi.WHITE);
        ids = new IntrusionDetectionSystem(log);
        lan1Interface = new NetworkInterface(
                AddressTable.MAC_R1, AddressTable.IP_R1, 1,
                AddressTable.ROUTER1_PORT, AddressTable.LAN1_PORT);
        lan2Interface = new NetworkInterface(
                AddressTable.MAC_R2, AddressTable.IP_R2, 2,
                AddressTable.ROUTER2_PORT, AddressTable.LAN2_PORT);
    }

    @Test
    void disabledByDefault() {
        assertFalse(ids.isEnabled());
    }

    @Test
    void disabledDoesNotInspect() {
        // Source IP claims LAN2 but arrives on LAN1 — should be spoofing
        // But IDS is disabled, so should not flag it
        IPPacket packet = IPPacket.icmp(new IPAddress(0x22), new IPAddress(0x32),
                PingMessage.request(1).encode());
        EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet.encode());

        boolean drop = ids.inspect(packet, frame, lan1Interface);
        assertFalse(drop);
        assertEquals(0, ids.spoofAlertCount());
    }

    @Test
    void detectsSpoofInPassiveMode() {
        ids.setEnabled(true);
        ids.setActiveMode(false);

        // Source IP 0x22 (LAN2) arrives on LAN1 interface
        IPPacket packet = IPPacket.icmp(new IPAddress(0x22), new IPAddress(0x32),
                PingMessage.request(1).encode());
        EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet.encode());

        boolean drop = ids.inspect(packet, frame, lan1Interface);
        assertFalse(drop); // passive mode: log only, don't drop
        assertEquals(1, ids.spoofAlertCount());
    }

    @Test
    void detectsSpoofInActiveMode() {
        ids.setEnabled(true);
        ids.setActiveMode(true);

        IPPacket packet = IPPacket.icmp(new IPAddress(0x22), new IPAddress(0x32),
                PingMessage.request(1).encode());
        EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet.encode());

        boolean drop = ids.inspect(packet, frame, lan1Interface);
        assertTrue(drop); // active mode: drop the packet
        assertEquals(1, ids.spoofAlertCount());
    }

    @Test
    void noSpoofAlertForLegitimatePacket() {
        ids.setEnabled(true);
        ids.setActiveMode(true);

        // Source IP 0x12 (LAN1) arrives on LAN1 interface — legitimate
        IPPacket packet = IPPacket.icmp(new IPAddress(0x12), new IPAddress(0x22),
                PingMessage.request(1).encode());
        EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet.encode());

        boolean drop = ids.inspect(packet, frame, lan1Interface);
        assertFalse(drop);
        assertEquals(0, ids.spoofAlertCount());
    }

    @Test
    void sameLanSpoofNotDetected() {
        ids.setEnabled(true);
        ids.setActiveMode(true);

        // Node1 (0x12, LAN1) spoofs as Node2 (0x13, LAN1) — same LAN, undetectable
        IPPacket packet = IPPacket.icmp(new IPAddress(0x13), new IPAddress(0x22),
                PingMessage.request(1).encode());
        EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet.encode());

        boolean drop = ids.inspect(packet, frame, lan1Interface);
        assertFalse(drop);
        assertEquals(0, ids.spoofAlertCount());
    }

    @Test
    void floodDetectionTriggersAfterThreshold() {
        ids.setEnabled(true);
        ids.setActiveMode(true);

        IPAddress src = new IPAddress(0x12);
        IPAddress dst = new IPAddress(0x22);

        // Send 9 pings — should not trigger flood
        for (int i = 0; i < 9; i++) {
            IPPacket packet = IPPacket.icmp(src, dst, PingMessage.request(i + 1).encode());
            EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                    packet.encode());
            ids.inspect(packet, frame, lan1Interface);
        }
        assertEquals(0, ids.floodAlertCount());

        // 10th ping should trigger flood detection
        IPPacket packet10 = IPPacket.icmp(src, dst, PingMessage.request(10).encode());
        EthernetFrame frame10 = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                packet10.encode());
        boolean drop = ids.inspect(packet10, frame10, lan1Interface);
        assertTrue(drop);
        assertTrue(ids.floodAlertCount() >= 1);
    }

    @Test
    void floodDetectionPassiveModeDoesNotDrop() {
        ids.setEnabled(true);
        ids.setActiveMode(false);

        IPAddress src = new IPAddress(0x12);
        IPAddress dst = new IPAddress(0x22);

        for (int i = 0; i < 15; i++) {
            IPPacket packet = IPPacket.icmp(src, dst, PingMessage.request(i + 1).encode());
            EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                    packet.encode());
            boolean drop = ids.inspect(packet, frame, lan1Interface);
            assertFalse(drop); // passive: never drop
        }
        assertTrue(ids.floodAlertCount() >= 1);
    }

    @Test
    void alertCountsAccumulate() {
        ids.setEnabled(true);
        ids.setActiveMode(false);

        // Two spoof attempts
        for (int i = 0; i < 2; i++) {
            IPPacket packet = IPPacket.icmp(new IPAddress(0x22), new IPAddress(0x32),
                    PingMessage.request(i + 1).encode());
            EthernetFrame frame = new EthernetFrame(new MACAddress("N1"), new MACAddress("R1"),
                    packet.encode());
            ids.inspect(packet, frame, lan1Interface);
        }
        assertEquals(2, ids.spoofAlertCount());
    }
}
