# CS441 Network Emulator — Project Guide

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Network Topology](#2-network-topology)
- [3. Address Table](#3-address-table)
- [4. Protocol Formats](#4-protocol-formats)
- [5. Component Descriptions](#5-component-descriptions)
- [6. How to Build](#6-how-to-build)
- [7. How to Run (Step by Step)](#7-how-to-run-step-by-step)
- [8. CLI Command Reference](#8-cli-command-reference)
- [9. Demo Scenarios](#9-demo-scenarios)
  - [Demo 1 — Intra-LAN Ping](#demo-1--intra-lan-ping-node1--node2-same-lan)
  - [Demo 2 — Cross-LAN Ping](#demo-2--cross-lan-ping-node2--node3-through-router)
  - [Demo 3 — Multi-Hop Cross-LAN Ping](#demo-3--multi-hop-cross-lan-ping-node1--node4-lan1--lan3)
  - [Demo 4 — IP Spoofing Attack (Same LAN)](#demo-4--ip-spoofing-attack-same-lan)
  - [Demo 5 — IP Spoofing Attack (Cross-LAN)](#demo-5--ip-spoofing-attack-cross-lan)
  - [Demo 6 — Sniffing Attack](#demo-6--sniffing-attack-promiscuous-mode)
  - [Demo 7 — Firewall Blocking](#demo-7--firewall-blocking)
  - [Demo 8 — IDS Spoof Detection (Passive)](#demo-8--ids-spoof-detection-passive-mode)
  - [Demo 9 — IDS Spoof Detection (Active)](#demo-9--ids-spoof-detection-active-mode)
  - [Demo 10 — IDS Ping Flood Detection](#demo-10--ids-ping-flood-detection)
- [10. Log Format Reference](#10-log-format-reference)
- [11. Unit Tests](#11-unit-tests)

---

## 1. Project Overview

This project emulates an **IP-over-Ethernet network** using Java UDP sockets. It simulates:

- **Ethernet frame transmission** with MAC-based filtering on broadcast LANs
- **IP packet routing** across multiple LAN segments through a router
- **ICMP ping** (echo request/reply) with RTT measurement
- **Security attacks**: IP spoofing, network sniffing (promiscuous mode), and ping flooding
- **Security defenses**: a packet-filtering firewall and an Intrusion Detection System (IDS)

Each network device (node, router, LAN emulator) runs as a **separate Java process** and communicates via localhost UDP sockets, faithfully emulating real Ethernet broadcast behavior.

---

## 2. Network Topology

```
                          ┌───────────────────────────────────────────┐
                          │               ROUTER                      │
                          │   ┌─────┐   ┌─────┐   ┌─────┐           │
                          │   │  R1 │   │  R2 │   │  R3 │           │
                          │   │0x11 │   │0x21 │   │0x31 │           │
                          │   │:6011│   │:6012│   │:6013│           │
                          │   └──┬──┘   └──┬──┘   └──┬──┘           │
                          │      │         │         │    [IDS]      │
                          └──────┼─────────┼─────────┼───────────────┘
                                 │         │         │
              ┌──────────────────┘         │         └──────────────────┐
              │                            │                            │
     ─────────┼────────────       ─────────┼────────────       ─────────┼────────────
    │  LAN1 Emulator :5001 │    │  LAN2 Emulator :5002 │    │  LAN3 Emulator :5003 │
     ─────────┬────────────       ─────────┬────────────       ─────────┬────────────
              │                            │                            │
       ┌──────┴──────┐             ┌───────┴──────┐             ┌───────┴──────┐
       │             │             │              │             │              │
  ┌────┴────┐  ┌─────┴───┐  ┌─────┴───┐          │       ┌─────┴───┐          │
  │  Node1  │  │  Node2  │  │  Node3  │          │       │  Node4  │          │
  │  MAC=N1 │  │  MAC=N2 │  │  MAC=N3 │          │       │  MAC=N4 │          │
  │  IP=0x12│  │  IP=0x13│  │  IP=0x22│          │       │  IP=0x32│          │
  │  :6001  │  │  :6002  │  │  :6003  │          │       │  :6004  │          │
  │ATTACKER │  │ NORMAL  │  │FIREWALL │          │       │ NORMAL  │          │
  └─────────┘  └─────────┘  └─────────┘          │       └─────────┘          │
                                                  │                            │
                                         LAN 2 (segment)             LAN 3 (segment)
```

**How it works:**

- There are **3 LAN segments**, each managed by a LAN emulator process that simulates Ethernet broadcast behavior.
- The **Router** has 3 interfaces (R1, R2, R3), one plugged into each LAN, and forwards IP packets between them.
- **Node1** is the attacker on LAN1 (can spoof IPs, sniff traffic, and flood pings).
- **Node2** is a normal node on LAN1.
- **Node3** is on LAN2 and has a **firewall** that can block packets by source IP.
- **Node4** is a normal node on LAN3.

---

## 3. Address Table

| Device | Role | MAC Address | IP Address | UDP Port | LAN |
|--------|------|-------------|------------|----------|-----|
| Node1 | Attacker (spoof/sniff/flood) | `N1` | `0x12` | 6001 | LAN1 |
| Node2 | Normal node | `N2` | `0x13` | 6002 | LAN1 |
| Node3 | Firewall node | `N3` | `0x22` | 6003 | LAN2 |
| Node4 | Normal node | `N4` | `0x32` | 6004 | LAN3 |
| Router R1 | Router interface on LAN1 | `R1` | `0x11` | 6011 | LAN1 |
| Router R2 | Router interface on LAN2 | `R2` | `0x21` | 6012 | LAN2 |
| Router R3 | Router interface on LAN3 | `R3` | `0x31` | 6013 | LAN3 |
| LAN1 Emu | Broadcast emulator | — | — | 5001 | LAN1 |
| LAN2 Emu | Broadcast emulator | — | — | 5002 | LAN2 |
| LAN3 Emu | Broadcast emulator | — | — | 5003 | LAN3 |

**Routing rule:** The high nibble of the destination IP determines the target LAN (`0x1_` = LAN1, `0x2_` = LAN2, `0x3_` = LAN3). The router uses this to decide which egress interface to forward packets through.

---

## 4. Protocol Formats

### Ethernet Frame

```
 0      1      2      3      4      5 ...    4+N
┌──────┬──────┬──────┬──────┬──────┬──────────────┐
│ Src MAC (2B)│ Dst MAC (2B)│ Len  │  Data (0-256)│
│  ASCII char │  ASCII char │ 1 B  │              │
└──────┴──────┴──────┴──────┴──────┴──────────────┘
```

- **Source MAC**: 2 ASCII characters (e.g., `N1`, `R2`)
- **Destination MAC**: 2 ASCII characters (broadcast = `FF`)
- **Data Length**: 1 unsigned byte (0–255)
- **Data**: 0 to 256 bytes (the IP packet payload)
- **Max frame size**: 261 bytes

### IP Packet

```
 0      1      2      3      4 ...    3+N
┌──────┬──────┬──────┬──────┬──────────────┐
│SrcIP │DstIP │Proto │ Len  │  Data (0-256)│
│ 1 B  │ 1 B  │ 1 B  │ 1 B  │              │
└──────┴──────┴──────┴──────┴──────────────┘
```

- **Source IP**: 1 byte (e.g., `0x12`)
- **Destination IP**: 1 byte (e.g., `0x22`)
- **Protocol**: 1 byte (`0x01` = ICMP)
- **Data Length**: 1 unsigned byte
- **Data**: 0 to 256 bytes (the ICMP/ping payload)

### Ping (ICMP Echo) Message

```
 0      1      2                           9
┌──────┬──────┬────────────────────────────┐
│ Type │ Seq  │     Timestamp (8 bytes)    │
│ 1 B  │ 1 B  │     milliseconds (long)    │
└──────┴──────┴────────────────────────────┘
```

- **Type**: `0x08` = Echo Request, `0x00` = Echo Reply
- **Sequence**: 1 unsigned byte (0–255)
- **Timestamp**: 8 bytes, big-endian `System.currentTimeMillis()` — used for RTT calculation
- **Total**: 10 bytes fixed

---

## 5. Component Descriptions

### LAN Emulator (`netemu.lan.LAN`)

Simulates an Ethernet broadcast medium. When a node sends a frame to the LAN emulator, it **fans the frame out** to every other registered endpoint on that LAN (excluding the sender). This is how real Ethernet works — all devices on a segment see all frames.

- **Registration protocol**: On startup, each node sends `REGISTER <MAC> <port>` to its LAN emulator. The emulator stores the MAC-to-port mapping.
- **Frame forwarding**: Any non-registration UDP packet is treated as an Ethernet frame and broadcast to all other registered endpoints.

### Node (Abstract Base — `netemu.device.Node`)

The base class for all network nodes. Provides:

- **Socket binding** and **LAN emulator registration**
- **Receiver thread**: Listens for incoming UDP packets, decodes Ethernet frames
- **MAC filtering**: Drops frames where the destination MAC does not match this node's MAC (and is not broadcast `FF`)
- **IP packet handling**: Decodes IP packets from accepted frames
- **Ping handling**: Automatically replies to ICMP echo requests; logs echo replies with RTT
- **Interactive CLI**: Command loop for sending pings and inspecting node state

### Node1 — Attacker (`netemu.device.Node1`)

Extends Node with three attack capabilities:

- **IP Spoofing**: Replaces the source IP in outgoing packets with an attacker-chosen IP, making the victim appear to be the sender.
- **Sniffing (Promiscuous Mode)**: Overrides MAC filtering to log all frames on the LAN, even those addressed to other nodes. Decodes and prints IP and ping payloads.
- **Ping Flood**: Sends a large burst of ICMP echo requests to overwhelm a target.

### Node3 — Firewall Node (`netemu.device.Node3`)

Extends Node with a packet-filtering firewall:

- Maintains a **blocklist of source IP addresses**
- Before processing any incoming IP packet, the firewall checks if the source IP is blocked
- Blocked packets are silently dropped with a security log entry
- Rules can be **added and removed at runtime** via the CLI

### Router (`netemu.device.Router`)

A multi-interface IP router connecting all three LANs:

- Has 3 `NetworkInterface` records (R1/LAN1, R2/LAN2, R3/LAN3), each with its own UDP socket
- **Registers** each interface with its respective LAN emulator
- **Receives** frames on any interface, applies MAC filtering
- **Forwards** IP packets: determines the destination LAN from the high nibble of the destination IP, resolves the destination MAC, re-encapsulates into a new Ethernet frame, and sends it out the correct egress interface
- **Handles local packets**: If the destination IP is one of the router's own IPs, it processes the packet locally (e.g., replies to pings)
- **Integrates the IDS** for security inspection of forwarded traffic

### Firewall (`netemu.device.Firewall`)

A source-IP-based packet filter:

- Thread-safe (`CopyOnWriteArraySet` for the blocklist, `volatile` enabled flag)
- Enabled by default
- Returns `true` from `shouldBlock()` if the packet's source IP is in the blocklist

### Intrusion Detection System (`netemu.device.IntrusionDetectionSystem`)

The open-category extension. Monitors router-forwarded traffic for:

1. **IP Spoof Detection**: If a packet's source IP belongs to a different LAN than the interface it arrived on, it's flagged as a potential spoof. (e.g., source IP `0x22`/LAN2 arriving on the LAN1 interface)
2. **Ping Flood Detection**: Tracks ICMP echo requests per source IP in a sliding 5-second window. If more than 10 requests arrive within the window, it flags a flood.

Two operating modes:
- **Passive**: Logs alerts only (packets are still forwarded)
- **Active**: Logs alerts AND drops suspicious packets

---

## 6. How to Build

**Prerequisites:** Java 17+, Maven 3.6+

```bash
cd cs441-g1-t3
mvn clean package
```

This compiles the project, runs all 109 unit tests, and produces a fat JAR at:
```
target/netemu-1.0-SNAPSHOT.jar
```

To run tests only:
```bash
mvn test
```

---

## 7. How to Run (Step by Step)

You need **8 separate terminal windows** (or tabs). Start the processes in the order below.

### Step 1 — Start the three LAN Emulators

These must be running before any nodes or the router start, because nodes send registration messages on startup.

**Terminal 1 — LAN1 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.LAN1
```

**Terminal 2 — LAN2 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.LAN2
```

**Terminal 3 — LAN3 Emulator:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.LAN3
```

You should see each emulator print a startup banner like:
```
==================================================
  LAN1 Emulator (port 5001)
==================================================
HH:mm:ss.SSS [LAN1] Listening on port 5001
```

### Step 2 — Start the Router

The router registers its three interfaces (R1, R2, R3) with all three LAN emulators.

**Terminal 4 — Router:**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Router
```

Expected output:
```
==================================================
  Router [R1/R2/R3]
==================================================
HH:mm:ss.SSS [Router] Registered interface R1 on LAN1
HH:mm:ss.SSS [Router] Registered interface R2 on LAN2
HH:mm:ss.SSS [Router] Registered interface R3 on LAN3
HH:mm:ss.SSS [Router] Router started with 3 interfaces (R1, R2 and R3)

--- Router Commands ---
  ping <destIP> [count <n>]      - Send ping(s)
  ids on|off                     - Enable/disable IDS
  ids mode active|passive        - Set IDS mode
  ids status                     - Show IDS status
  info                           - Show interface info
  help                           - Show this help
  quit                           - Exit
```

Each LAN emulator terminal will also show the router's registration:
```
HH:mm:ss.SSS [LAN1] Device R1 joined (port 6011)
```

### Step 3 — Start the Nodes

**Terminal 5 — Node1 (Attacker):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node1
```

**Terminal 6 — Node2 (Normal):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node2
```

**Terminal 7 — Node3 (Firewall):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node3
```

**Terminal 8 — Node4 (Normal):**
```bash
java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node4
```

Each node prints a startup banner, for example Node1:
```
==================================================
  Node1 | MAC: N1 | IP: 0x12 | LAN1
==================================================
HH:mm:ss.SSS [Node1] Listening on port 6001
HH:mm:ss.SSS [Node1] Connected to LAN1 emulator

--- Node1 Commands ---
  ping <destIP> [count <n>]  - Send ping(s)
  info                       - Show interface info
  help                       - Show this help
  quit                       - Exit
  spoof on <IP> | off        - Enable/disable IP spoofing
  sniff on | off             - Enable/disable promiscuous sniffing
  flood <destIP> <count>     - Send ping flood
```

### Step 4 — You are ready

All 8 processes are now running. Type commands in any node's terminal to interact with the network. Use `quit` or Ctrl+C to stop any process.

---

## 8. CLI Command Reference

### All Nodes + Router

| Command | Description |
|---------|-------------|
| `ping <destIP> [count <n>]` | Send `n` ICMP echo requests to `destIP` (hex, e.g., `0x22`). Default count is 1. |
| `info` | Print this device's MAC, IP, LAN, and port number. |
| `help` | Show available commands. |
| `quit` | Gracefully exit the process. |

### Node1 Only (Attacker)

| Command | Description |
|---------|-------------|
| `spoof on <IP>` | Enable IP spoofing — all outgoing packets will use `<IP>` as source IP instead of `0x12`. |
| `spoof off` | Disable IP spoofing — resume using the real source IP. |
| `sniff on` | Enable promiscuous mode — log all LAN1 frames, even those addressed to other MACs. |
| `sniff off` | Disable promiscuous mode — resume normal MAC filtering. |
| `flood <destIP> <count>` | Send `<count>` ping requests to `<destIP>` as fast as possible (no 1-second delay between pings). |

### Node3 Only (Firewall)

| Command | Description |
|---------|-------------|
| `fw add block src <IP>` | Add a firewall rule to drop all incoming packets from source `<IP>`. |
| `fw remove block src <IP>` | Remove the block rule for source `<IP>`. |
| `fw on` | Enable the firewall (enabled by default). |
| `fw off` | Disable the firewall (all packets pass through). |
| `fw status` | Show whether the firewall is enabled and list all blocked source IPs. |

### Router Only (IDS)

| Command | Description |
|---------|-------------|
| `ids on` | Enable the Intrusion Detection System (disabled by default). |
| `ids off` | Disable the IDS. |
| `ids mode active` | Set IDS to active mode — suspicious packets are logged AND dropped. |
| `ids mode passive` | Set IDS to passive mode — suspicious packets are logged only (still forwarded). |
| `ids status` | Show IDS enabled/disabled state, mode, and alert counters. |

---

## 9. Demo Scenarios

---

### Demo 1 — Intra-LAN Ping (Node1 <-> Node2, Same LAN)

#### What we are simulating

Two nodes on the **same LAN segment** exchanging ICMP echo request/reply packets. This demonstrates the most basic network communication: a frame is broadcast on the LAN, the destination node accepts it (MAC filter match), decodes the IP packet and ping message, and sends a reply back.

#### Why this matters

This is the foundational test that proves the entire protocol stack works end-to-end: Ethernet frame encoding/decoding, LAN broadcast emulation, MAC-based filtering, IP packet encapsulation, and ICMP ping request/reply handling.

#### Steps

**On Node2's terminal, type:**
```
ping 0x12
```

#### What happens (step by step)

1. **Node2** creates a `PingMessage` (type=REQUEST, seq=1, timestamp=now).
2. Node2 wraps it in an `IPPacket` (src=`0x13`, dst=`0x12`, proto=ICMP).
3. Node2 sees `0x12` is on LAN1 (same LAN), resolves `0x12` -> MAC `N1`.
4. Node2 wraps the IP packet in an `EthernetFrame` (src=`N2`, dst=`N1`).
5. Node2 sends the frame via UDP to the **LAN1 emulator** (port 5001).
6. **LAN1 emulator** receives the frame and **broadcasts** it to all registered endpoints except Node2 (i.e., to Node1 and Router R1).
7. **Router R1** receives the frame but the destination MAC is `N1`, not `R1` — the router **drops** it (MAC filter mismatch).
8. **Node1** receives the frame. Destination MAC `N1` matches its own MAC — it **accepts** the frame.
9. Node1 decodes the IP packet, sees it's ICMP, decodes the ping, and logs: `RX Ping request from 0x13 (seq=1)`.
10. Node1 creates a reply (type=REPLY, same seq and timestamp), wraps it in IP (src=`0x12`, dst=`0x13`) and Ethernet (src=`N1`, dst=`N2`), and sends it to LAN1 emu.
11. LAN1 emu broadcasts the reply to Node2 and Router R1.
12. **Node2** receives the reply, matches MAC, decodes, and logs: `RX Ping reply from 0x12 (seq=1, RTT=Xms)`.

#### Expected output

**Node2 terminal:**
```
HH:mm:ss.SSS [Node2] Pinging 0x12 with 1 packets...
HH:mm:ss.SSS [Node2] TX Frame [N2 -> N1 | 14 bytes]
HH:mm:ss.SSS [Node2]   └─ Packet [0x13 -> 0x12 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node2] RX Frame [N1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Node2]   └─ Packet [0x12 -> 0x13 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node2] RX Ping reply from 0x12 (seq=1, RTT=Xms)
```

**Node1 terminal:**
```
HH:mm:ss.SSS [Node1] RX Frame [N2 -> N1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x13 -> 0x12 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node1] RX Ping request from 0x13 (seq=1)
HH:mm:ss.SSS [Node1] TX Frame [N1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x12 -> 0x13 | ICMP | 10 bytes]
```

---

### Demo 2 — Cross-LAN Ping (Node2 -> Node3, Through Router)

#### What we are simulating

A ping from **LAN1 to LAN2**, which requires the **router to forward** the packet. This demonstrates IP routing: the source node sends to the router's local interface, the router determines the correct egress LAN, re-encapsulates the IP packet in a new Ethernet frame with different MAC addresses, and sends it out the other interface.

#### Why this matters

This proves that the router correctly performs IP forwarding: receiving on one interface, looking up the destination LAN from the IP address, and re-wrapping the packet in a new Ethernet frame for the destination LAN. It also shows that the Ethernet layer and IP layer are properly separated — the same IP packet gets two different Ethernet frames (one on each LAN).

#### Steps

**On Node2's terminal, type:**
```
ping 0x22
```

#### What happens (step by step)

1. **Node2** creates the ping and IP packet (src=`0x13`, dst=`0x22`).
2. Node2 sees `0x22` is LAN2 (different from its own LAN1), so it sends to the **local router interface** — MAC destination = `R1`.
3. The frame `[N2 -> R1]` is sent to LAN1 emu, which broadcasts it on LAN1.
4. **Router** (R1 interface) receives the frame. MAC `R1` matches — it accepts.
5. Router decodes the IP packet. Destination `0x22` has LAN ID = 2.
6. Router logs: `Routing: 0x13 -> 0x22 via LAN2`.
7. Router resolves `0x22` -> MAC `N3`, creates a new Ethernet frame `[R2 -> N3]`, and sends it to **LAN2 emulator** via socket2.
8. LAN2 emu broadcasts the frame to Node3 (and any other LAN2 devices).
9. **Node3** receives the frame. MAC `N3` matches. It decodes the IP packet, checks the firewall (no rules set, passes), and processes the ping.
10. Node3 creates a reply (src=`0x22`, dst=`0x13`), sends to Router R2 `[N3 -> R2]`.
11. Router receives on R2, forwards to LAN1 as `[R1 -> N2]`.
12. **Node2** receives the reply and logs RTT.

#### Expected output

**Node2 terminal:**
```
HH:mm:ss.SSS [Node2] Pinging 0x22 with 1 packets...
HH:mm:ss.SSS [Node2] TX Frame [N2 -> R1 | 14 bytes]
HH:mm:ss.SSS [Node2]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node2] RX Frame [R1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Node2]   └─ Packet [0x22 -> 0x13 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node2] RX Ping reply from 0x22 (seq=1, RTT=Xms)
```

**Router terminal:**
```
HH:mm:ss.SSS [Router] RX Frame [N2 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] Routing: 0x13 -> 0x22 via LAN2
HH:mm:ss.SSS [Router] TX Frame [R2 -> N3 | 14 bytes]
HH:mm:ss.SSS [Router]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] RX Frame [N3 -> R2 | 14 bytes] on R2
HH:mm:ss.SSS [Router]   └─ Packet [0x22 -> 0x13 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] Routing: 0x22 -> 0x13 via LAN1
HH:mm:ss.SSS [Router] TX Frame [R1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Router]   └─ Packet [0x22 -> 0x13 | ICMP | 10 bytes]
```

**Node3 terminal:**
```
HH:mm:ss.SSS [Node3] RX Frame [R2 -> N3 | 14 bytes]
HH:mm:ss.SSS [Node3]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node3] RX Ping request from 0x13 (seq=1)
HH:mm:ss.SSS [Node3] TX Frame [N3 -> R2 | 14 bytes]
HH:mm:ss.SSS [Node3]   └─ Packet [0x22 -> 0x13 | ICMP | 10 bytes]
```

---

### Demo 3 — Multi-Hop Cross-LAN Ping (Node1 -> Node4, LAN1 -> LAN3)

#### What we are simulating

A ping traversing from LAN1 all the way to LAN3. This is the longest path in our topology: Node1 (LAN1) -> Router R1 -> Router R3 -> Node4 (LAN3). It verifies that routing works for all three LAN segments.

#### Why this matters

Confirms the router can forward between non-adjacent LANs (LAN1 to LAN3). Since the router has a direct interface on every LAN, "multi-hop" here means the packet enters on R1 and exits on R3 within the same router process.

#### Steps

**On Node1's terminal, type:**
```
ping 0x32 count 3
```

#### What happens

1. Node1 sends 3 ping requests to `0x32` (Node4), 1 second apart.
2. Each ping goes: Node1 `[N1->R1]` via LAN1 -> Router forwards `[R3->N4]` via LAN3 -> Node4 replies `[N4->R3]` via LAN3 -> Router forwards `[R1->N1]` via LAN1 -> Node1 receives reply.
3. Node1 logs 3 RTT values.

#### Expected output

**Node1 terminal:**
```
HH:mm:ss.SSS [Node1] Pinging 0x32 with 3 packets...
HH:mm:ss.SSS [Node1] TX Frame [N1 -> R1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x12 -> 0x32 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node1] RX Frame [R1 -> N1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x32 -> 0x12 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node1] RX Ping reply from 0x32 (seq=1, RTT=Xms)
  (... repeats for seq=2, seq=3 ...)
```

**Router terminal:**
```
(For each of the 3 pings, shows the full RX/Routing/TX cycle:)
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x12 -> 0x32 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] Routing: 0x12 -> 0x32 via LAN3
HH:mm:ss.SSS [Router] TX Frame [R3 -> N4 | 14 bytes]
HH:mm:ss.SSS [Router]   └─ Packet [0x12 -> 0x32 | ICMP | 10 bytes]
  (... then the reply from Node4 routed back via LAN1 ...)
```

---

### Demo 4 — IP Spoofing Attack (Same LAN)

#### What we are simulating

Node1 (the attacker on LAN1) **impersonates Node2** by spoofing its source IP address (`0x13`) and sends a ping to Node3. From Node3's perspective, the ping appears to come from Node2 — but Node2 never sent it. This is a classic **IP spoofing attack**.

#### Why this matters

IP spoofing is one of the most fundamental network attacks. In a real network, the receiver trusts the source IP field in the packet header. By forging this field, an attacker can:
- Hide their identity
- Frame another host as the source of malicious traffic
- Bypass IP-based access controls

This demo also shows a **limitation of the router's IDS**: since both Node1 (`0x12`) and Node2 (`0x13`) are on the same LAN (LAN1), the IDS cannot distinguish the spoof — the source IP `0x13` legitimately belongs to LAN1, matching the ingress interface.

#### Steps

**On Node1's terminal:**
```
spoof on 0x13
ping 0x22
```

#### What happens

1. Node1 enables spoofing with victim IP `0x13` (Node2's IP).
2. Node1 creates a ping to `0x22` (Node3). Normally the IP packet would have src=`0x12`, but the spoof intercepts and replaces it with `0x13`.
3. Node1 logs: `Spoofed packet: pretending to be 0x13 (real IP: 0x12)`.
4. The frame goes to Router R1 (because `0x22` is on LAN2), which forwards to Node3.
5. **Node3** receives a ping that appears to be from `0x13` (Node2). Node3 logs: `RX Ping request from 0x13 (seq=1)`.
6. Node3 sends its reply to `0x13` — which goes to **Node2**, not Node1. Node1 never sees the reply.
7. **Node2** receives an unsolicited ping reply that it never requested.

#### Expected output

**Node1 terminal:**
```
HH:mm:ss.SSS [Node1] WARN: Spoofing ON — now impersonating 0x13
HH:mm:ss.SSS [Node1] WARN: Spoofed packet: pretending to be 0x13 (real IP: 0x12)
HH:mm:ss.SSS [Node1] TX Frame [N1 -> R1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]   ← note: source IP is 0x13, not 0x12!
(No ping reply received — the reply goes to Node2)
```

**Node3 terminal:**
```
HH:mm:ss.SSS [Node3] RX Frame [R2 -> N3 | 14 bytes]
HH:mm:ss.SSS [Node3]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]   ← Node3 sees source as Node2
HH:mm:ss.SSS [Node3] RX Ping request from 0x13 (seq=1)
(Node3 thinks this is from Node2)
```

**Node2 terminal:**
```
HH:mm:ss.SSS [Node2] RX Frame [R1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Node2]   └─ Packet [0x22 -> 0x13 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node2] RX Ping reply from 0x22 (seq=1, RTT=Xms)
(Node2 receives an unexpected reply it never requested)
```

**After the demo, disable spoofing:**
```
spoof off
```

---

### Demo 5 — IP Spoofing Attack (Cross-LAN)

#### What we are simulating

Node1 spoofs a source IP from a **different LAN** (`0x22`, which belongs to LAN2) and sends a ping to Node4 on LAN3. This is detectable by the router's IDS because the source IP claims to be from LAN2 but the packet arrived on the LAN1 interface.

#### Why this matters

This demonstrates a more detectable form of spoofing and sets up the IDS demos (Demos 8 and 9). The IDS uses **ingress filtering** — it checks whether the source IP's LAN matches the interface the packet arrived on. Cross-LAN spoofing violates this invariant.

#### Steps

**On Node1's terminal:**
```
spoof on 0x22
ping 0x32
spoof off
```

#### What happens

1. Node1 sends a ping to Node4 (`0x32`) with spoofed source IP `0x22` (Node3's IP on LAN2).
2. The packet arrives at Router's R1 interface (LAN1). The source IP `0x22` has LAN ID = 2, but R1 is LAN1. If IDS is enabled, this mismatch would trigger a spoof alert.
3. Without IDS enabled, the router forwards it to Node4, which sees a ping "from `0x22`".
4. Node4 replies to `0x22`, which goes to Node3 — not to Node1.

---

### Demo 6 — Sniffing Attack (Promiscuous Mode)

#### What we are simulating

Node1 puts its network interface into **promiscuous mode**, allowing it to see all traffic on LAN1 — even frames addressed to other MAC addresses. Normally, a node's NIC drops frames not addressed to its MAC. Sniffing bypasses this filter.

#### Why this matters

On a real shared Ethernet segment (hub, not switch), all frames are visible to all hosts. A malicious host can capture and inspect all traffic on the LAN by disabling MAC filtering. This is how tools like Wireshark and tcpdump work when set to promiscuous mode. In our emulation, the LAN emulator broadcasts all frames to all registered hosts (simulating a hub), and Node1's sniffer reads frames it would normally drop.

#### Steps

**On Node1's terminal, enable sniffing first:**
```
sniff on
```

**Then, on Node2's terminal, send a ping to the router:**
```
ping 0x11
```

#### What happens

1. Node1 enables sniffing (promiscuous mode).
2. Node2 sends a ping to Router R1 (`0x11`). The Ethernet frame is `[N2 -> R1]`.
3. LAN1 emulator broadcasts this frame to Node1 and Router R1.
4. **Normally**, Node1 would drop this frame because the destination MAC is `R1`, not `N1`.
5. **With sniffing enabled**, Node1 intercepts the frame before MAC filtering and logs it:
   - The Ethernet frame headers (source MAC, destination MAC)
   - The decoded IP packet (source IP `0x13`, destination IP `0x11`, protocol ICMP)
   - The decoded ping message (type, sequence, timestamp)
6. Node1 still applies normal MAC filtering for its own processing (it won't try to reply to the ping), but the sniffer has captured the data.

#### Expected output

**Node1 terminal (with sniffing on):**
```
HH:mm:ss.SSS [Node1] WARN: Sniffing ON — capturing all LAN1 traffic
HH:mm:ss.SSS [Node1] [Sniffed] Frame [N2 -> R1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x13 -> 0x11 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node1]      └─ Ping REQUEST seq=1
```

Node1 also sees the router's reply frame `[R1 -> N2]`:
```
HH:mm:ss.SSS [Node1] [Sniffed] Frame [R1 -> N2 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x11 -> 0x13 | ICMP | 10 bytes]
HH:mm:ss.SSS [Node1]      └─ Ping REPLY seq=1
```

**After the demo, disable sniffing:**
```
sniff off
```

---

### Demo 7 — Firewall Blocking

#### What we are simulating

Node3's **packet-filtering firewall** blocking all traffic from a specific source IP. We add a rule to block Node2 (`0x13`), verify that pings from Node2 are dropped, then remove the rule and verify that pings resume.

#### Why this matters

Firewalls are the most common network defense mechanism. This demonstrates:
- **Runtime rule management**: adding and removing rules without restarting
- **Source-IP filtering**: dropping packets based on the source IP in the IP header
- **Defense effectiveness**: blocked packets are silently dropped, and the sender receives no reply
- **Default-allow policy**: only explicitly blocked IPs are dropped; everything else passes

#### Steps

**Step 1 — Verify normal connectivity:**

On Node2's terminal:
```
ping 0x22
```
Node3 receives the ping and replies. Node2 sees the RTT.

**Step 2 — Check firewall status:**

On Node3's terminal:
```
fw status
```
Output:
```
  Firewall:    ON (filtering)
  Blocked IPs: (none)
```

**Step 3 — Add a block rule for Node2's IP:**

On Node3's terminal:
```
fw add block src 0x13
```
Output:
```
HH:mm:ss.SSS [Node3] Firewall rule added: block all packets from 0x13
```

**Step 4 — Try pinging again from Node2:**

On Node2's terminal:
```
ping 0x22
```
Node2 sends the ping, but Node3's firewall blocks it. Node2 receives **no reply**.

**Node3 terminal shows:**
```
HH:mm:ss.SSS [Node3] SECURITY: Firewall dropped packet: 0x13 -> 0x22
```

**Step 5 — Verify other nodes can still reach Node3:**

On Node1's terminal:
```
ping 0x22
```
Node3 receives and replies (only `0x13` is blocked, not `0x12`).

**Step 6 — Check firewall status again:**

On Node3's terminal:
```
fw status
```
Output:
```
  Firewall:    ON (filtering)
  Blocked IPs:
    - 0x13
```

**Step 7 — Remove the block rule:**

On Node3's terminal:
```
fw remove block src 0x13
```

**Step 8 — Verify Node2 can reach Node3 again:**

On Node2's terminal:
```
ping 0x22
```
Node3 receives the ping and replies successfully.

---

### Demo 8 — IDS Spoof Detection (Passive Mode)

#### What we are simulating

The router's **Intrusion Detection System** detecting a cross-LAN IP spoofing attack in **passive mode**. Passive mode logs alerts but does **not** drop packets, allowing network administrators to monitor suspicious activity without disrupting traffic.

#### Why this matters

IDS systems are critical for detecting attacks that firewalls cannot prevent. The IDS uses **ingress filtering**: it checks whether the source IP address in a packet is consistent with the LAN interface it arrived on. If Node1 on LAN1 sends a packet with source IP `0x22` (which belongs to LAN2), the IDS flags it because the LAN ID doesn't match. In passive mode, the alert is logged for analysis, but the packet is still forwarded — this is useful for monitoring without risking false-positive disruptions.

#### Steps

**On the Router's terminal, enable IDS in passive mode:**
```
ids on
ids mode passive
```

**On Node1's terminal, spoof as a LAN2 address and ping Node4:**
```
spoof on 0x22
ping 0x32
```

#### What happens

1. Node1 sends a ping with spoofed source IP `0x22` to Node4 (`0x32`).
2. The packet arrives at Router's R1 interface (LAN1).
3. The IDS inspects the packet: source IP `0x22` has LAN ID = 2, but the packet arrived on LAN1 (interface R1).
4. **Mismatch detected** — the IDS logs a spoof alert.
5. Because the IDS is in **passive mode**, the packet is still forwarded to Node4.
6. Node4 receives the (spoofed) ping and replies.

#### Expected output

**Router terminal:**
```
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x22 -> 0x32 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] SECURITY: IDS Alert — IP spoof detected: 0x22 claims LAN2 but arrived on LAN1 (MAC: N1)
HH:mm:ss.SSS [Router] Routing: 0x22 -> 0x32 via LAN3
HH:mm:ss.SSS [Router] TX Frame [R3 -> N4 | 14 bytes]
HH:mm:ss.SSS [Router]   └─ Packet [0x22 -> 0x32 | ICMP | 10 bytes]
```

The alert is logged, but the packet was still forwarded. Check IDS statistics:
```
ids status
```
Output:
```
  IDS:          ON (monitoring)
  Mode:         PASSIVE (log only)
  Spoof alerts: 1
  Flood alerts: 0
```

**Clean up on Node1:**
```
spoof off
```

---

### Demo 9 — IDS Spoof Detection (Active Mode)

#### What we are simulating

The same cross-LAN spoofing attack as Demo 8, but now the IDS is in **active mode**. In active mode, the IDS not only logs the alert but also **drops the suspicious packet**, preventing it from reaching its destination.

#### Why this matters

Active IDS (also called IPS — Intrusion Prevention System) can stop attacks in real time. The trade-off is that false positives would also block legitimate traffic. This demo shows the IDS successfully preventing a spoofed packet from being delivered.

#### Steps

**On the Router's terminal, switch to active mode:**
```
ids on
ids mode active
```

**On Node1's terminal:**
```
spoof on 0x22
ping 0x32
```

#### What happens

1. Same as Demo 8: Node1 sends spoofed packet from LAN1 with source IP `0x22`.
2. IDS detects the mismatch.
3. Because the IDS is in **active mode**, it **drops the packet**.
4. Node4 **never receives** the ping. No reply is sent.

#### Expected output

**Router terminal:**
```
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x22 -> 0x32 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] SECURITY: IDS Alert — IP spoof detected: 0x22 claims LAN2 but arrived on LAN1 (MAC: N1)
HH:mm:ss.SSS [Router] SECURITY: IDS Action — Dropped packet from 0x22
```

Note: No "Routing:" or "TX" message appears — the packet was dropped after the RX.

**Node4 terminal:** (nothing — the ping never arrives)

**Node1 terminal:** (no reply received)

**Clean up on Node1:**
```
spoof off
```

---

### Demo 10 — IDS Ping Flood Detection

#### What we are simulating

Node1 launches a **ping flood** — sending a large burst of ICMP echo requests as fast as possible. The router's IDS detects the flood when the ping rate exceeds the threshold (more than 10 pings from the same source within a 5-second window) and drops subsequent pings in active mode.

#### Why this matters

Ping floods (a type of Denial of Service attack) can overwhelm a target's network interface or processing capacity. The IDS implements **rate limiting** by tracking the number of ICMP requests from each source within a sliding time window. This demonstrates:
- How flood attacks work
- How IDS rate-limiting detection works
- The difference between the first few pings passing (below threshold) and subsequent pings being dropped (above threshold)

#### Steps

**On the Router's terminal, ensure IDS is in active mode:**
```
ids on
ids mode active
```

**On Node1's terminal (make sure spoofing is off):**
```
spoof off
flood 0x22 20
```

#### What happens

1. Node1 sends 20 ping requests to Node3 (`0x22`) as fast as possible (no delay between pings).
2. The router receives each ping on R1 and inspects it with the IDS.
3. Pings 1–9: Below the threshold (10). The IDS allows them through. Node3 receives and replies.
4. **Ping 10**: The flood tracker records the 10th ping within the 5-second window. The IDS triggers a flood alert and **drops** this ping (active mode).
5. Pings 11–20: Each additional ping also triggers a flood alert and is dropped.
6. Node3 only receives roughly 9 pings (the ones before the threshold was reached).

#### Expected output

**Node1 terminal:**
```
HH:mm:ss.SSS [Node1] WARN: Flood attack: sending 20 rapid pings to 0x22
HH:mm:ss.SSS [Node1] TX Frame [N1 -> R1 | 14 bytes]
HH:mm:ss.SSS [Node1]   └─ Packet [0x12 -> 0x22 | ICMP | 10 bytes]
(... 20 TX pairs ...)
HH:mm:ss.SSS [Node1] WARN: Flood attack complete: 20 pings sent
```

**Router terminal:**
```
(First 9 pings pass through normally:)
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x12 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] Routing: 0x12 -> 0x22 via LAN2
...
(Starting from the 10th ping — IDS kicks in:)
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x12 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] SECURITY: IDS Alert — Ping flood detected: 10 pings from 0x12 in 5000ms (threshold: 10)
HH:mm:ss.SSS [Router] SECURITY: IDS Action — Dropped packet from 0x12
HH:mm:ss.SSS [Router] RX Frame [N1 -> R1 | 14 bytes] on R1
HH:mm:ss.SSS [Router]   └─ Packet [0x12 -> 0x22 | ICMP | 10 bytes]
HH:mm:ss.SSS [Router] SECURITY: IDS Alert — Ping flood detected: 11 pings from 0x12 in 5000ms (threshold: 10)
HH:mm:ss.SSS [Router] SECURITY: IDS Action — Dropped packet from 0x12
(... repeats for each remaining ping ...)
```

**Check IDS status on Router:**
```
ids status
```
Output:
```
  IDS:          ON (monitoring)
  Mode:         ACTIVE (log + drop)
  Spoof alerts: 0
  Flood alerts: 11
```

---

## 10. Log Format Reference

All log messages follow this format:

```
HH:mm:ss.SSS [ComponentName] [LEVEL:] message
```

| Level | Color | Format | When used |
|-------|-------|--------|-----------|
| INFO | Component default | `[Name] message` | Normal operations |
| WARN | Yellow | `[Name] WARN: message` | Suspicious actions (spoofing, flooding) |
| ERROR | Red | `[Name] ERROR: message` | Failures and exceptions |
| SECURITY | Red + Bold | `[Name] SECURITY: message` | Security events (IDS alerts, firewall blocks) |
| RX | Component default | `[Name] RX message` | Receiving frames/packets |
| TX | Component default | `[Name] TX message` | Transmitting frames/packets |

**Component colors:**
- Node1 (Attacker): Red
- Node2, Node4 (Normal): Green
- Node3 (Firewall): Blue
- Router: Purple
- LAN Emulators: Cyan

---

## 11. Unit Tests

Run all 109 tests with:
```bash
mvn test
```

| Test Class | Tests | What it covers |
|-----------|-------|----------------|
| `EthernetFrameTest` | 10 | Frame encode/decode, round-trip fidelity, max data size, empty data, oversized rejection, unsigned length field, IP packet payload integration |
| `IPPacketTest` | 12 | Packet encode/decode, ICMP factory method, round-trip fidelity, empty/max data, oversized rejection, unsigned length field, full-stack test (Ping inside IP inside Ethernet) |
| `PingMessageTest` | 12 | Request/reply creation, timestamp preservation, encode/decode round-trip, unsigned sequence, 8-byte timestamp range, RTT calculation, ICMP type constants |
| `MACAddressTest` | 12 | Constructor validation (null, too short, too long), ASCII encode/decode, writeTo offset, round-trip, broadcast constant, equality/hashCode |
| `IPAddressTest` | 13 | Valid range (0x00–0xFF), boundary rejection, hex parsing (0x prefix, 0X prefix, no prefix), toByte, fromByte, LAN ID extraction, equality, hex string formatting |
| `ByteUtilTest` | 11 | Big-endian short read/write, round-trip, hex parse/format, array slice (sub-array, from start, zero-length, source immutability) |
| `AddressTableTest` | 17 | Port distinctness, all expected port/IP/MAC values, IP-to-MAC resolution, LAN lookup, router MAC/IP by LAN, unknown-input error cases |
| `FirewallTest` | 9 | Enabled by default, block/unblock rules, disabled bypasses rules, re-enable restores rules, multiple blocked sources, snapshot immutability, idempotent block |
| `IntrusionDetectionSystemTest` | 9 | Disabled by default, passive spoof detection (log only), active spoof detection (drop), legitimate packets pass, same-LAN spoof undetectable, flood threshold trigger, passive flood (no drop), alert counter accumulation |
| `NetworkInterfaceTest` | 4 | Record field access, toString content, equality, inequality |
