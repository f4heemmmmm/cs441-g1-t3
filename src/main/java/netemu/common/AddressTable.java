package netemu.common;

import java.util.Map;

public final class AddressTable {
    
    private AddressTable() {}

    // Port Numbers - LAN EMULATORS
    public static final int LAN1_PORT = 5001;
    public static final int LAN2_PORT = 5002;
    public static final int LAN3_PORT = 5003;

    // Port Numbers - NODES
    public static final int NODE1_PORT = 6001;
    public static final int NODE2_PORT = 6002;
    public static final int NODE3_PORT = 6003;
    public static final int NODE4_PORT = 6004;

    // Port Numbers - ROUTERS
    public static final int ROUTER1_PORT = 6011;
    public static final int ROUTER2_PORT = 6012;
    public static final int ROUTER3_PORT = 6013;

    // MAC Addresses - NODES
    public static final MACAddress MAC_N1 = new MACAddress("N1");
    public static final MACAddress MAC_N2 = new MACAddress("N2");
    public static final MACAddress MAC_N3 = new MACAddress("N3");
    public static final MACAddress MAC_N4 = new MACAddress("N4");

    // MAC Addresses - ROUTERS
    public static final MACAddress MAC_R1 = new MACAddress("R1");
    public static final MACAddress MAC_R2 = new MACAddress("R2");
    public static final MACAddress MAC_R3 = new MACAddress("R3");

    // IP Addresses - NODES
    public static final IPAddress IP_N1 = new IPAddress(0x12);
    public static final IPAddress IP_N2 = new IPAddress(0x13);
    public static final IPAddress IP_N3 = new IPAddress(0x22);
    public static final IPAddress IP_N4 = new IPAddress(0x32);

    // IP Addresses - ROUTERS
    public static final IPAddress IP_R1 = new IPAddress(0x11);
    public static final IPAddress IP_R2 = new IPAddress(0x21);
    public static final IPAddress IP_R3 = new IPAddress(0x31);

    // Maps LAN ID to LAN Emulator Port Number
    private static final Map<Integer, Integer> LAN_PORTS_MAP = Map.of(
        1, LAN1_PORT,
        2, LAN2_PORT,
        3, LAN3_PORT
    );

    // Maps IP Address to MAC Address (for lookup, similar to ARP resolution)
    private static final Map<IPAddress, MACAddress> IP_TO_MAC = Map.of(
        // NODES
        IP_N1, MAC_N1,
        IP_N2, MAC_N2,
        IP_N3, MAC_N3,
        IP_N4, MAC_N4,

        // ROUTERS
        IP_R1, MAC_R1,
        IP_R2, MAC_R2,
        IP_R3, MAC_R3
    );

    // Maps each interface IP Address to the LAN segment (by the LAN ID) it belongs to
    private static final Map<IPAddress, Integer> IP_TO_LAN = Map.of(
        // LAN 1 contains NODE 1, NODE 2 and ROUTER 1
        IP_N1, 1,
        IP_N2, 1,
        IP_R1, 1,

        // LAN 2 contains NODE 3 and ROUTER 2
        IP_N3, 2,
        IP_R2, 2,

        // LAN 3 contains NODE 4 and ROUTER 3
        IP_N4, 3,
        IP_R3, 3
    );

    // Get the LAN emulator port number for a LAN ID
    public static int getLANPortNumber(int lanID) {
        Integer port = LAN_PORTS_MAP.get(lanID);
        if (port == null) throw new IllegalArgumentException("Unknown LAN ID: " + lanID);
        return port;
    }

    // Resolve an IP Address to a MAC Address 
    public static MACAddress resolve(IPAddress ipAddress) {
        MACAddress macAddress = IP_TO_MAC.get(ipAddress);
        if (macAddress == null) throw new IllegalArgumentException("Unknown IP: " + ipAddress);
        return macAddress;
    }

    // Get the which LAN the IP Address belongs to
    public static int getLANForIP(IPAddress ipAddress) {
        Integer lan = IP_TO_LAN.get(ipAddress);
        if (lan == null) throw new IllegalArgumentException("Unknown IP: " + ipAddress);
        return lan;
    }

    // Determine the router's MAC Address for a destination IP Address when routing cross-LAN
    public static MACAddress getRouterMACAddressForLAN(int lanID) {
        return switch (lanID) {
            case 1 -> MAC_R1;
            case 2 -> MAC_R2;
            case 3 -> MAC_R3;
            default -> throw new IllegalArgumentException("Unknown LAN ID: " + lanID);
        };
    }

    // Get the router's IP Address on a given LAN
    public static IPAddress getRouterIPAddressForLAN(int lanID) {
        return switch (lanID) {
            case 1 -> IP_R1;
            case 2 -> IP_R2;
            case 3 -> IP_R3;
            default -> throw new IllegalArgumentException("Unknown LAN ID: " + lanID);
        };
    }
}
