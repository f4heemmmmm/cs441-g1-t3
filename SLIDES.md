---
marp: true
theme: default
paginate: true
style: |
  section {
    font-family: 'Segoe UI', sans-serif;
    font-size: 26px;
  }
  h1 { color: #1a3c6e; font-size: 44px; }
  h2 { color: #1a3c6e; font-size: 32px; border-bottom: 2px solid #1a3c6e; padding-bottom: 6px; }
  h3 { color: #2563a8; font-size: 24px; }
  code { background: #f0f4ff; border-radius: 4px; padding: 2px 6px; }
  pre { background: #1e1e2e; color: #cdd6f4; border-radius: 8px; padding: 14px; font-size: 20px; }
  .tag { display: inline-block; background: #e8f0fe; color: #1a3c6e; border-radius: 4px; padding: 2px 10px; font-size: 18px; font-weight: bold; margin: 2px; }
  .required { background: #dcfce7; color: #166534; }
  .open { background: #fef3c7; color: #92400e; }
  section.divider { background: #1a3c6e; color: white; text-align: center; }
  section.divider h1 { color: white; font-size: 72px; }
  section.divider h2 { color: #cbd5e1; border: none; }
---

# CS441 Network Emulator

## IP-over-Ethernet with Security

**Team:** G1 – T3
**Format:** 20 min live demo + 10 min Q&A

---

## What We Built

- **8 processes** — 3 LAN emulators, 1 router, 4 nodes
- **From scratch** in Java over UDP sockets
- Real DHCP handshake for IP assignment
- **170 unit tests** passing
- All spec requirements + **IDS with DHCP snooping** for open category

---

## Rubric Coverage

| Item | Pts | Demo |
|------|:--:|------|
| Ethernet (broadcast + MAC filter) | 2 | 1 |
| IP packet format | 3 | 2 |
| Packet forwarding | 2 | 2 |
| Ping protocol | 1 | 1, 2 |
| IP spoofing | 2 | 3 |
| Sniffing attack | 1 | 4 |
| Firewall (runtime) | 2 | 5 |
| **Open category** | **8** | **6, 7, 8, 9** |

**Total: 21 pts**

---

## Network Topology

```
    Node1     Node2           Node3           Node4
    (N1)      (N2)            (N3)            (N4)
    0x12      0x13            0x22            0x32
     │         │               │               │
     └────┬────┘               │               │
          │                    │               │
        LAN1                 LAN2            LAN3
          │                    │               │
          R1 ─── Router ──── R2                R3
         0x11                0x21              0x31
                                └───────┬───────┘
                                    (Router)
```

- Node1 = **attacker** | Node3 = **firewall** | Router = **IDS**

---

## Boot

```bash
bash start-demo.sh
```

- Opens 8 terminals (~14 s)
- Every node acquires its IP via DHCP
- Router's IDS **snoops every DHCPACK** → builds MAC↔IP table
- Wait for all 4 banners before starting demos

---

<!-- _class: divider -->

# PART 1

## Required Rubric — 13 pts

---

## Demo 1 — Intra-LAN Ping

<span class="tag required">Ethernet 2 pts</span> <span class="tag required">Ping 1 pt</span>

**Proves:** LAN broadcast + MAC filter + ICMP request/reply

```
# on Node2
ping 0x12
```

- Frame `[N2 → N1]` broadcast to all LAN1 endpoints
- Node1 accepts (MAC matches), Router drops (MAC mismatch)
- Round-trip with RTT reported

---

## Demo 2 — Cross-LAN Ping

<span class="tag required">IP 3 pts</span> <span class="tag required">Forwarding 2 pts</span>

**Proves:** Router re-encapsulates the same IP packet into a new frame

```
# on Node2
ping 0x22
```

- Node2 sends `[N2 → R1]` (frame) with IP `0x13 → 0x22`
- Router extracts packet, logs **`Routing: 0x13 → 0x22 via LAN2`**
- New frame `[R2 → N3]` with **same IP packet inside**

---

## Demo 3 — IP Spoofing Attack

<span class="tag required">Spoofing 2 pts</span>

**Proves:** Node1 forges source IP — victim never knows

```
# on Node1
spoof on 0x13
ping 0x32
spoof off
```

- Node1 rewrites source IP to `0x13` (Node2's)
- Node4 sees a ping *from Node2*
- Reply goes to Node2 — attacker is **invisible**

---

## Demo 4 — Sniffing Attack

<span class="tag required">Sniffing 1 pt</span>

**Proves:** Promiscuous mode bypasses MAC filter

```
# on Node1
sniff on

# on Node2
ping 0x11
```

- Node1 captures frames addressed to `R1` and `N2`
- Full decode: frame → packet → ping payload
- Confidentiality is broken — setup for Demo 9

---

## Demo 5 — Firewall (Runtime)

<span class="tag required">Firewall 2 pts</span>

**Proves:** Add/remove rules live — no restart

```
# on Node3
fw add block src 0x13

# on Node2  → no reply
ping 0x22

# on Node3  → remove live
fw remove block src 0x13
```

- Blocked source IPs dropped before ping handler runs
- Default-allow: only explicit blocks drop
- `fw status` shows the rule box live

---

<!-- _class: divider -->

# PART 2

## Open Category — 8 pts

---

## Our Open Category

**Intrusion Detection System** on the Router:

1. **Cross-LAN spoof detection** — LAN-ID mismatch
2. **MAC-IP binding check** — via **DHCP snooping**
3. **Ping flood detection** — sliding-window rate limit

Plus: **Encrypted messaging** (XOR cipher) to defeat sniffing

**Why DHCP snooping?** Real managed switches do exactly this.
Ground truth comes from observed DHCPACKs, not static config.

---

## Demo 6 — IDS Cross-LAN Spoof (Active)

<span class="tag open">Open Cat</span>

```
# Router
ids on
ids mode active

# Node1
spoof on 0x22
ping 0x32
```

- `0x22` claims LAN2 but arrives on LAN1 interface → **impossible**
- Two alerts fire: cross-LAN + MAC-IP mismatch
- **Dropped** in active mode — Node4 never sees it

---

## Demo 7 — Same-LAN Spoof via DHCP Snooping

<span class="tag open">Headline</span>

**The demo that justifies the open category.**

```
# Node1 (still on LAN1)
spoof on 0x13
ping 0x22
```

- Cross-LAN check **passes** (both `0x12` and `0x13` are LAN1)
- Snooping check: MAC `N1` holds lease `0x12`, not `0x13` → **DROP**
- This mirrors **Dynamic ARP Inspection** on production switches
- Same-LAN spoofing is undetectable without this

---

## Demo 8 — IDS Ping Flood Detection

<span class="tag open">Open Cat</span>

```
# Node1
flood 0x22 20
```

- Threshold: 10 pings / 5-second window
- First 9 pass, pings 10-20 trigger alerts + drops
- `ids status` shows incremented flood counter
- Mitigates simple DoS attempts

---

## Demo 9 — Encryption vs Sniffing

<span class="tag open">Open Cat</span>

```
# Node1
sniff on

# Node2 — plaintext
msg 0x32 Secret plans inside

# Node2 — encrypted (same message)
emsg 0x32 Secret plans inside
```

- **Plaintext** → sniffer reads the message in full
- **Encrypted** → sniffer sees `Message [ENCRYPTED] "	?9(?.z*…"`
- Node4 (receiver) decrypts with shared key on arrival

---

## Recap

| Feature | Status |
|---------|:-:|
| Ethernet + MAC filter | ✓ |
| IP + routing | ✓ |
| Ping protocol | ✓ |
| IP spoofing attack | ✓ |
| Sniffing attack | ✓ |
| Runtime firewall | ✓ |
| IDS — 3 detection strategies | ✓ |
| DHCP snooping integration | ✓ |
| Encrypted messaging | ✓ |

**170/170 unit tests passing**

---

## Architecture Highlights

- **Every byte on the wire** is serialized by our code (no pcap)
- **Thread-safe atomic logging** — tree-style frame/packet/inner-message
- **DHCP server** is MAC-pinned for deterministic topology
- **DHCP expiry callback** keeps the IDS snoop table in sync
- **Open-category extras not shown**: ARP protocol, ARP spoofing + MITM, Dynamic ARP Inspection on the LAN side

---

<!-- _class: divider -->

# Q & A

## Ask us anything

*Full reference: `GUIDE.md` · Script: `PRESENTATION.md`*

---

## Thank You

**CS441 Network Emulator**
Group 1 – Team 3

- `GUIDE.md` — 17-scenario reference
- `PRESENTATION.md` — full demo script
- `start-demo.sh` — one-command boot
- `mvn test` — 170 tests
