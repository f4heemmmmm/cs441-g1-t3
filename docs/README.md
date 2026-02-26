# CS441 Network Emulator - Detailed Overview

## Architecture

This project emulates an IP-over-Ethernet network using multiple OS processes communicating via UDP sockets. Each network device (node, router, LAN emulator) runs as a separate JVM process.

### Processes (8 core + 2 optional dashboard)

| Process | Class | Role |
|---------|-------|------|
| LAN1 Emulator | `netemu.lan.Lan1Emu` | Broadcast hub for LAN1 (port 5001) |
| LAN2 Emulator | `netemu.lan.Lan2Emu` | Broadcast hub for LAN2 (port 5002) |
| LAN3 Emulator | `netemu.lan.Lan3Emu` | Broadcast hub for LAN3 (port 5003) |
| Router | `netemu.device.RouterR` | Forwards packets between LANs |
| Node1 | `netemu.device.Node1` | Attacker node (spoof + sniff) on LAN1 |
| Node2 | `netemu.device.Node2` | Normal node on LAN1 |
| Node3 | `netemu.device.Node3` | Firewall node on LAN2 |
| Node4 | `netemu.device.Node4` | Normal node on LAN3 |
| Dashboard (Java) | `netemu.dashboard.DashboardServer` | _(optional)_ UDP event collector + SSE on port 8080 |
| Dashboard (NestJS) | `dashboard/` (Node.js) | _(optional)_ Enhanced frontend on port 3000 |

### Address Table

| Device | MAC | IP | UDP Port | LAN |
|--------|-----|-----|----------|-----|
| Node1 | N1 | 0x12 | 6001 | LAN1 |
| Node2 | N2 | 0x13 | 6002 | LAN1 |
| Node3 | N3 | 0x22 | 6003 | LAN2 |
| Node4 | N4 | 0x32 | 6004 | LAN3 |
| R1 | R1 | 0x11 | 6011 | LAN1 |
| R2 | R2 | 0x21 | 6012 | LAN2 |
| R3 | R3 | 0x31 | 6013 | LAN3 |

### Communication Model

1. **Registration**: On startup, each endpoint sends a registration message to its LAN emulator: `REGISTER <MAC> <port>`
2. **Sending**: Endpoint encodes an Ethernet frame and sends it via UDP to the LAN emulator
3. **Broadcasting**: LAN emulator receives frames and forwards to ALL registered endpoints
4. **MAC Filtering**: Each endpoint checks the destination MAC; drops if not addressed to it
5. **Routing**: Router receives frames on one interface, looks up destination LAN by IP prefix, re-encapsulates, and sends out the appropriate interface

### Package Structure

```
netemu/
  common/           - Wire formats, utilities, address table
    MacAddress       - 2-byte ASCII MAC address
    IpAddress        - 1-byte IP address
    EthernetFrame    - Ethernet frame encode/decode
    IpPacket         - IP packet encode/decode
    PingMessage      - Ping request/reply encode/decode
    ByteUtil         - Safe byte parsing helpers
    AddressTable     - Hardcoded topology configuration
    Log              - Timestamped logging
  lan/               - LAN emulators
    LanEmulator      - Registration + broadcast logic
    Lan1Emu          - LAN1 entrypoint (port 5001)
    Lan2Emu          - LAN2 entrypoint (port 5002)
    Lan3Emu          - LAN3 entrypoint (port 5003)
  device/            - Network devices
    NetworkInterface - NIC record (mac, ip, lan, ports)
    NodeBase         - Common node logic (receive, send, CLI)
    Node1            - Attacker: spoof + sniff + pingflood
    Node2            - Normal LAN1 node
    Node3            - Firewall-enabled LAN2 node
    Node4            - Normal LAN3 node
    RouterR          - 3-interface router with IDS
    Firewall         - IP-layer firewall (block by srcIP)
    IdsEngine        - Spoof detection + ping flood detection
  dashboard/         - Dashboard monitoring
    DashboardServer  - UDP collector (port 9000) + HTTP/SSE server (port 8080)
    EventReporter    - Fire-and-forget UDP event sender (used by all processes)
```

### Enhanced Dashboard (`dashboard/` directory, Node.js)

A NestJS + TailwindCSS frontend that proxies the Java SSE stream and adds:
- Interactive SVG topology with node tooltips (click for MAC/IP/role)
- Collapsible scenario guide (Parts A-F with commands and what to watch)
- Event type legend (15 types with color badges)
- Filter buttons (All / Frames / Pings / Security / System)
- Process status chips with online/offline dots and role labels

```bash
cd dashboard && npm install && npm run start:dev   # port 3000
```

Requires the Java DashboardServer running on port 8080 as the SSE event source.

### Concurrency

Each node/router has:
- **Receiver thread**: Blocks on UDP receive, decodes frames, logs, processes
- **CLI thread**: Reads stdin commands, triggers send/config actions

LAN emulators have:
- **Receiver loop**: Handles registration messages and frame broadcasts

All threads are daemon threads. The `quit` command cleanly shuts down each process.

### Routing Logic

The high nibble of the destination IP determines the target LAN:
- `0x1_` → LAN1 (via R1)
- `0x2_` → LAN2 (via R2)
- `0x3_` → LAN3 (via R3)

When a node sends to a different LAN, it addresses the frame to its local router interface. The router then re-encapsulates with the correct egress MAC.
