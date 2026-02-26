package netemu.common;

import java.util.Map;

/**
 * Hardcoded address table for the network topology.
 * Provides IP-to-MAC, IP-to-LAN, and LAN-to-router-MAC mappings.
 */
public final class AddressTable {

    private AddressTable() {}

    // LAN emulator ports
    public static final int LAN1_PORT = 5001;
    public static final int LAN2_PORT = 5002;
    public static final int LAN3_PORT = 5003;

    // Node receive ports (each endpoint has its own unique port)
    public static final int NODE1_PORT = 6001;
    public static final int NODE2_PORT = 6002;
    public static final int NODE3_PORT = 6003;
    public static final int NODE4_PORT = 6004;
    public static final int ROUTER_R1_PORT = 6011;
    public static final int ROUTER_R2_PORT = 6012;
    public static final int ROUTER_R3_PORT = 6013;

    /** IP -> MAC mapping for all nodes. */
    public static final Map<Integer, String> IP_TO_MAC = Map.of(
            0x11, "R1",
            0x12, "N1",
            0x13, "N2",
            0x21, "R2",
            0x22, "N3",
            0x31, "R3",
            0x32, "N4"
    );

    /** MAC -> expected IP mapping (for IDS). */
    public static final Map<String, Integer> MAC_TO_IP = Map.of(
            "R1", 0x11,
            "N1", 0x12,
            "N2", 0x13,
            "R2", 0x21,
            "N3", 0x22,
            "R3", 0x31,
            "N4", 0x32
    );

    /** IP -> LAN ID. High nibble determines LAN. */
    public static int ipToLan(int ip) {
        return (ip >> 4) & 0x0F;
    }

    /** LAN ID -> router MAC on that LAN. */
    public static String lanToRouterMac(int lanId) {
        return switch (lanId) {
            case 1 -> "R1";
            case 2 -> "R2";
            case 3 -> "R3";
            default -> null;
        };
    }

    /** LAN ID -> LAN emulator port. */
    public static int lanToPort(int lanId) {
        return switch (lanId) {
            case 1 -> LAN1_PORT;
            case 2 -> LAN2_PORT;
            case 3 -> LAN3_PORT;
            default -> throw new IllegalArgumentException("Unknown LAN: " + lanId);
        };
    }

    /** Get the MAC for a destination IP. */
    public static String ipToMac(int ip) {
        return IP_TO_MAC.get(ip);
    }
}
