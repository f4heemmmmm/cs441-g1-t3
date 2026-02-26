# CS441 IP-over-Ethernet Network Emulator

A multi-process network emulator that simulates IP-over-Ethernet communication using UDP sockets and LAN broadcast emulation.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Build
```bash
mvn clean package -q
```

### Run (8 separate terminals)

| Terminal | Process | Command |
|----------|---------|---------|
| T1 | LAN1 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan1Emu` |
| T2 | LAN2 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan2Emu` |
| T3 | LAN3 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan3Emu` |
| T4 | Router | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.RouterR` |
| T5 | Node1 (Attacker) | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node1` |
| T6 | Node2 | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node2` |
| T7 | Node3 (Firewall) | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node3` |
| T8 | Node4 | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node4` |
| T9 | Dashboard | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.dashboard.DashboardServer` |

Start in order: T1-T3 first, then T4, then T5-T8.

### Dashboard (optional, T9)

A live web dashboard provides a unified view of all network activity.

```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.dashboard.DashboardServer
```

Then open [http://localhost:8080](http://localhost:8080) in your browser. The dashboard shows:
- **SVG topology** with live node status (green = online, grey = offline)
- **Real-time event feed** with color-coded events (IDS alerts, firewall blocks, pings, etc.)
- **Statistics panel** tracking frame counts, IDS alerts, firewall blocks, and more
- **Animated packet flow** on topology links when packets route between LANs

The dashboard is fire-and-forget: if it's not running, the 8 emulator processes work normally with no impact.

### Enhanced Dashboard (NestJS + TailwindCSS, T10)

A richer dashboard with topology tooltips, scenario guide, event filtering, and process status tracking.

```bash
# Install (one-time)
cd dashboard && npm install

# Run (start Java DashboardServer on T9 first)
npm run start:dev
```

Then open [http://localhost:3000](http://localhost:3000). Features:
- **Interactive topology** — click nodes for MAC/IP/role details
- **Scenario guide** — 6 collapsible tabs (Parts A-F) with commands and what to watch
- **Event type legend** — all 15 event types with color badges and descriptions
- **Filter buttons** — All / Frames / Pings / Security / System
- **Process status chips** — 8 chips with online/offline dots and role labels

Requires the Java DashboardServer (T9) running on port 8080 as the SSE event source.

### Run Tests
```bash
mvn clean test
```

## Topology

```
LAN1 (port 5001)          LAN2 (port 5002)          LAN3 (port 5003)
+-------+-------+         +-------+-------+         +-------+-------+
| Node1 | Node2 |         | Node3 |       |         | Node4 |       |
| N1    | N2    |         | N3    |       |         | N4    |       |
| 0x12  | 0x13  |         | 0x22  |       |         | 0x32  |       |
+-------+-------+         +-------+-------+         +-------+-------+
        |                         |                         |
      [R1]                      [R2]                      [R3]
      0x11                      0x21                      0x31
        +-------------------------+-------------------------+
                            RouterR
```

## Documentation
- [docs/README.md](docs/README.md) - Detailed architecture overview
- [docs/PROTOCOLS.md](docs/PROTOCOLS.md) - Wire format specification
- [docs/DEMO_RUNBOOK.md](docs/DEMO_RUNBOOK.md) - Demo index (links to per-scenario guides)
- [docs/demo/](docs/demo/) - Individual demo scenario guides (Setup, Parts A-F)
- [docs/SECURITY.md](docs/SECURITY.md) - Security features (spoof, sniff, firewall, IDS)
- [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) - Common issues and fixes
