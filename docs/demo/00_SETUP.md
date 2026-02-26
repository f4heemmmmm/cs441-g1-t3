# Demo Setup

## Prerequisites

- Java 17+ installed
- Maven 3.8+ installed
- 8 terminal windows available (+ 2 optional for dashboards)
- Node.js 18+ (optional, for enhanced NestJS dashboard)

## Build

```bash
mvn clean package -q
```

## Terminal Layout

You need 8 terminals (+ 2 optional for dashboards). Label them as follows:

| Terminal | Process | Description |
|----------|---------|-------------|
| **T1** | LAN1 Emulator | Broadcast hub for LAN1 (Node1, Node2, Router R1) |
| **T2** | LAN2 Emulator | Broadcast hub for LAN2 (Node3, Router R2) |
| **T3** | LAN3 Emulator | Broadcast hub for LAN3 (Node4, Router R3) |
| **T4** | Router | Forwards packets between LANs; runs IDS |
| **T5** | Node1 (Attacker) | LAN1 node with spoof + sniff capabilities |
| **T6** | Node2 | Normal LAN1 node |
| **T7** | Node3 (Firewall) | LAN2 node with firewall |
| **T8** | Node4 | Normal LAN3 node |
| **T9** | Dashboard (Java) | _(optional)_ Event collector + SSE on port 8080 |
| **T10** | Dashboard (NestJS) | _(optional)_ Enhanced frontend on port 3000 |

## Start Order (important!)

Start processes in this exact order. Each command runs in its own terminal.

### Step 1: Start LAN Emulators (T1, T2, T3)

**T1 - LAN1 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan1Emu
```
Expected output:
```
[HH:mm:ss.SSS][LAN1] LAN1 emulator started, listening on UDP port 5001
Type 'list' to show registered endpoints, 'quit' to exit.
```

**T2 - LAN2 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan2Emu
```
Expected output:
```
[HH:mm:ss.SSS][LAN2] LAN2 emulator started, listening on UDP port 5002
Type 'list' to show registered endpoints, 'quit' to exit.
```

**T3 - LAN3 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan3Emu
```
Expected output:
```
[HH:mm:ss.SSS][LAN3] LAN3 emulator started, listening on UDP port 5003
Type 'list' to show registered endpoints, 'quit' to exit.
```

### Step 2: Start Router (T4)

**T4 - Router:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.RouterR
```
Expected output:
```
[HH:mm:ss.SSS][ROUTER] Router started with 3 interfaces:
[HH:mm:ss.SSS][ROUTER]   R1 -> Interface R1 (IP=0x11, LAN1, emuPort=5001, listenPort=6011)
[HH:mm:ss.SSS][ROUTER]   R2 -> Interface R2 (IP=0x21, LAN2, emuPort=5002, listenPort=6012)
[HH:mm:ss.SSS][ROUTER]   R3 -> Interface R3 (IP=0x31, LAN3, emuPort=5003, listenPort=6013)
[HH:mm:ss.SSS][ROUTER] Registered R1 with LAN1 emulator (port 5001)
[HH:mm:ss.SSS][ROUTER] Registered R2 with LAN2 emulator (port 5002)
[HH:mm:ss.SSS][ROUTER] Registered R3 with LAN3 emulator (port 5003)
Router CLI. Type 'help' for commands.
ROUTER>
```

You should also see registrations on the LAN emulators:
- **T1**: `[...][LAN1] Registered endpoint: MAC=R1, UDP port=6011`
- **T2**: `[...][LAN2] Registered endpoint: MAC=R2, UDP port=6012`
- **T3**: `[...][LAN3] Registered endpoint: MAC=R3, UDP port=6013`

### Step 3: Start Nodes (T5, T6, T7, T8)

**T5 - Node1 (Attacker):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node1
```
Expected output:
```
[HH:mm:ss.SSS][N1] Node started: Interface N1 (IP=0x12, LAN1, emuPort=5001, listenPort=6001)
[HH:mm:ss.SSS][N1] Registered with LAN1 emulator (port 5001)
Type 'help' for commands, 'quit' to exit.
N1>
```

**T6 - Node2:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node2
```

**T7 - Node3 (Firewall):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node3
```

**T8 - Node4:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node4
```

### Verification

On **T1** (LAN1 Emulator), type `list`:
```
list
```
Expected:
```
[...][LAN1] Registered endpoint: MAC=R1, UDP port=6011
[...][LAN1] Registered endpoint: MAC=N1, UDP port=6001
[...][LAN1] Registered endpoint: MAC=N2, UDP port=6002
```

All 3 LAN1 endpoints (R1, N1, N2) should be registered. Similarly check T2 and T3.

---

### Optional: Start Dashboards (T9, T10)

If you want a live web dashboard to visualize all network activity:

**T9 - Java Dashboard Server (SSE backend):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.dashboard.DashboardServer
```
Expected output:
```
[Dashboard] HTTP server started on http://localhost:8080
[Dashboard] Open the URL above in your browser.
[Dashboard] UDP collector listening on port 9000
[Dashboard] Type 'quit' to exit.
```

Open [http://localhost:8080](http://localhost:8080) for the basic dashboard.

**T10 - Enhanced NestJS Dashboard (optional, requires Node.js 18+):**
```bash
# First-time setup:
cd dashboard && npm install

# Run:
npm run start:dev
```
Expected output:
```
[Dashboard-NestJS] Running on http://localhost:3000
```

Open [http://localhost:3000](http://localhost:3000) for the enhanced dashboard with interactive topology, scenario guide, event filtering, and process status tracking.

> **Note:** T10 requires T9 to be running (it proxies events from port 8080). T9 collects events from all emulator processes via UDP on port 9000.

---

**Setup is complete.** Proceed to [Part A: Ethernet Proof](01_A_ETHERNET_PROOF.md).
