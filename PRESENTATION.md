# CS441 Network Emulator — Demo Presentation

**Duration:** 30 minutes (20 min demo + 10 min Q&A)
**Format:** Live terminal demo across 8 processes
**Audience:** CS441 instructor + graders evaluating a 20% team project

This script is designed to be **held in one hand during the demo**. Every
section lists exactly what to type, exactly what to say, and what to point at.
The flow is curated down to **9 scenarios** that cover every rubric item
without repetition. If you need the full 17-scenario reference (edge cases,
alternative framings, additional demos), see `GUIDE.md`.

---

## Table of Contents

- [Rubric Coverage Map](#rubric-coverage-map)
- [Pre-Demo Checklist](#pre-demo-checklist)
- [Cold Open (30 s)](#cold-open-30-s)
- [Boot the Network (1 min)](#boot-the-network-1-min)
- [PART 1 — Required Rubric (12 min)](#part-1--required-rubric-12-min)
  - [Demo 1 — Intra-LAN Ping (Ethernet + MAC filter)](#demo-1--intra-lan-ping--ethernet--mac-filter)
  - [Demo 2 — Cross-LAN Ping (IP + routing + ping)](#demo-2--cross-lan-ping--ip--routing--ping)
  - [Demo 3 — IP Spoofing Attack](#demo-3--ip-spoofing-attack)
  - [Demo 4 — Sniffing Attack](#demo-4--sniffing-attack)
  - [Demo 5 — Firewall Runtime Rules](#demo-5--firewall-runtime-rules)
- [PART 2 — Open Category (8 min)](#part-2--open-category-8-min)
  - [Demo 6 — IDS Cross-LAN Spoof Detection (Active)](#demo-6--ids-cross-lan-spoof-detection-active)
  - [Demo 7 — IDS Same-LAN Spoof via DHCP Snooping](#demo-7--ids-same-lan-spoof-via-dhcp-snooping)
  - [Demo 8 — IDS Ping-Flood Detection](#demo-8--ids-ping-flood-detection)
  - [Demo 9 — Encrypted Messaging vs Sniffing](#demo-9--encrypted-messaging-vs-sniffing)
- [Wrap-Up (1 min)](#wrap-up-1-min)
- [Q&A Cheat Sheet](#qa-cheat-sheet)
- [Panic Buttons](#panic-buttons)
- [15-Minute Fallback Script](#15-minute-fallback-script)

---

## Rubric Coverage Map

Keep this table open on your laptop during Q&A so you can point to exactly
which demo earned which rubric point.

| Rubric Item | Points | Covered By |
|-------------|:------:|------------|
| Ethernet (broadcast + MAC filter) | 2 | Demo 1, visible in every frame log |
| IP (packet encapsulation & decoding) | 3 | Demo 2 |
| Packet forwarding (router) | 2 | Demo 2 |
| Ping protocol (custom header + behaviour) | 1–2 | Demo 1, Demo 2 |
| IP spoofing (attack) | 2 | Demo 3 |
| Sniffing attack | 1 | Demo 4 |
| Firewall (runtime configurable) | 2 | Demo 5 |
| Open category — IDS + DHCP snooping + AES-ish encrypted messaging | 8 | Demos 6, 7, 8, 9 |

**Required total: 13 pts. Open category: 8 pts. Project total: 21 pts → 20%.**

---

## Pre-Demo Checklist

Do this **the night before** and again **10 minutes before you walk in**.

```bash
cd cs441-g1-t3
mvn clean package        # builds fat jar + runs 170 unit tests
```

Expected output tail:
```
[INFO] Tests run: 170, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

If any test fails or the build breaks: **stop**, fix it, re-run. Do not walk
in with a broken jar.

**Visual checks before starting:**
- Terminal font size ≥ 14 pt (ideally 16+ for a projector)
- Terminal theme has a dark background so the ANSI colors pop
- Terminal window can fit **at least 60 columns** wide (box-drawing art needs it)
- Close Slack / email / any popup that might interrupt
- Silence phone

**Have these open on a second screen / tab:**
- `GUIDE.md` (full reference, in case a grader asks about an edge case)
- This file, `PRESENTATION.md`
- The CS441 project spec PDF (for pointing at the rubric)

---

## Cold Open (30 s)

*Walk in, open one terminal, type nothing yet.*

> "Our project emulates an IP-over-Ethernet network from scratch in Java — no
> pcap, no real NIC, just UDP sockets between 8 processes. Four nodes, three
> LANs, one multi-interface router. On top of that we built the five security
> features from the rubric, plus an **Intrusion Detection System with DHCP
> snooping** for our open category. Every frame, packet, ARP message, and
> DHCP exchange you're about to see is serialized byte-for-byte in our code
> and round-tripped through `DatagramSocket`. Let's boot it."

*Do not linger on the pitch. Start the demo.*

---

## Boot the Network (1 min)

```bash
bash start-demo.sh
```

*While the 8 terminals are spawning (takes ~14 s), talk over it:*

> "`start-demo.sh` opens eight terminal tabs two seconds apart: three LAN
> emulators, then the router, then the four nodes. Each node runs a real
> DHCP handshake to acquire its IP — you'll see DISCOVER, OFFER, REQUEST,
> ACK roll by in every tab."

*When all 8 tabs are visible, switch to Node1's tab and point at:*

```
╔════════════════════════════════════════════════════════════╗
║  Node1 | MAC: N1 | IP: 0x12 | LAN1                         ║
╚════════════════════════════════════════════════════════════╝
```

> "Node1 got IP `0x12` — same as the project spec's topology. We have a
> **MAC-pinned DHCP server** so every boot lands on the spec addresses
> deterministically. Node2 is `0x13`, Node3 is `0x22`, Node4 is `0x32`.
> Flip through the tabs if you want to verify."

*Switch to the Router tab briefly and point at these lines:*

```
[Router] IDS snoop: learned N1 -> 0x12
[Router] IDS snoop: learned N2 -> 0x13
[Router] IDS snoop: learned N3 -> 0x22
[Router] IDS snoop: learned N4 -> 0x32
```

> "The IDS has already **snooped all four DHCPACK messages**. That's how it
> will catch same-LAN spoofing later — we'll come back to this."

*Transition:* "OK, let's walk the rubric."

---

# PART 1 — REQUIRED RUBRIC (12 min)

---

## Demo 1 — Intra-LAN Ping (Ethernet + MAC filter)

**Rubric:** Ethernet (2 pts) + Ping (1–2 pts)
**Proves:** Frames are broadcast via the LAN emulator, MAC filtering drops
frames not addressed to us, ICMP echo request/reply works end-to-end.

**Switch to Node2's tab** and type:

```
ping 0x12
```

*While the output rolls, narrate against what's on screen:*

> "Node2 just built a ping packet, wrapped it in an Ethernet frame with
> destination MAC `N1`, and sent it to LAN1's emulator over UDP. The
> emulator broadcasts the frame to **every registered endpoint** — Node1
> AND the router's R1 interface. Both receive it."

**Switch to Node1's tab.** You should see:

```
[Node1 ] RX Frame [N2 -> N1 | 14 bytes]
[Node1 ]   └─ Packet [0x13 -> 0x12 | ICMP | 10 bytes]
[Node1 ]      └─ Ping REQUEST seq=1
[Node1 ] RX Ping request from 0x13 (seq=1)
```

*Point at the three-line tree and the single-line interpretation below it:*

> "Three lines showing the frame, the packet inside it, and the ping inside
> that. Node1 accepted it because the destination MAC matches."

**Switch to Router's tab.** You should see the router also RX'd the frame
but without any follow-up processing.

> "The router also received this frame — remember, it's a broadcast — but
> the destination MAC was `N1`, not `R1`, so the router **dropped it** at
> the MAC layer. That's the filter the spec asks for in point 1b."

**Switch back to Node2.** Show the reply:

```
[Node2 ] RX Ping reply from 0x12 (seq=1, RTT=Xms)
```

> "Node1 replied, Node2 got it back, and we have an RTT. Intra-LAN ping works."

**✓ Rubric items earned:** Ethernet broadcast, MAC-level drop, Ping protocol.

---

## Demo 2 — Cross-LAN Ping (IP + routing + ping)

**Rubric:** IP (3 pts) + Packet forwarding (2 pts) + Ping (1 pt)
**Proves:** The router forwards an IP packet between LANs by re-encapsulating
it in a new Ethernet frame with different source/destination MACs.

**On Node2's tab:**

```
ping 0x22
```

*Narrate:*

> "Node2 is on LAN1. `0x22` is on LAN2 — the high nibble gives away which
> LAN. So Node2 knows it can't deliver directly; it sends the frame to the
> **local router interface R1** and lets the router do the heavy lifting."

**Switch to Router's tab.** You should see:

```
[Router] RX Frame [N2 -> R1 | 14 bytes] on R1
[Router]   └─ Packet [0x13 -> 0x22 | ICMP | 10 bytes]

[Router] Routing: 0x13 -> 0x22 via LAN2

[Router] TX Frame [R2 -> N3 | 14 bytes]
[Router]   └─ Packet [0x12 -> 0x22 | ICMP | 10 bytes]
```

*Point at each section:*

> "Look at the router log carefully. First, it receives the frame on **R1**.
> Then it extracts the destination IP, sees LAN2, and logs the routing
> decision. Then — and this is the important part — it **builds a brand new
> Ethernet frame** with `R2` as source and `N3` as destination, but it
> keeps the **exact same IP packet** inside. The Ethernet layer and the IP
> layer are properly separated."

**Switch to Node3's tab.** Reply is automatic.

**Switch back to Node2** and show RTT:

```
[Node2 ] RX Ping reply from 0x22 (seq=1, RTT=Xms)
```

> "Round trip from LAN1 to LAN2 and back. Routing works."

**✓ Rubric items earned:** IP packet format, router forwarding, cross-LAN ping.

---

## Demo 3 — IP Spoofing Attack

**Rubric:** IP Spoofing (2 pts)
**Proves:** Node1 can forge the source IP field in outgoing packets and
pass that forgery all the way through the router to a victim.

**On Node1's tab:**

```
spoof on 0x13
ping 0x32
spoof off
```

*Narrate while typing:*

> "Node1 is the attacker. `spoof on 0x13` tells it: from now on, rewrite
> the source IP of every outgoing packet to `0x13` — which is Node2's
> address. Then we ping Node4 across the router."

**Point at Node1's log:**

```
[Node1 ] WARN: Spoofing ON — now impersonating 0x13
[Node1 ] WARN: Spoofed packet: pretending to be 0x13 (real IP: 0x12)
[Node1 ]   └─ Packet [0x13 -> 0x32 | ICMP | 10 bytes]
```

> "Node1 openly logs the spoof in yellow — that's for the demo. In a real
> attack, this log wouldn't exist. Look at the packet header: source IP is
> `0x13`, but the frame MAC is still `N1` — we can't fake the MAC because
> we're sending from a real NIC."

**Switch to Node4's tab** — point at:

```
[Node4 ] RX Ping request from 0x13 (seq=1)
```

> "Node4 thinks the ping came from Node2. It will reply to Node2, not to
> Node1. The attacker is **invisible** to Node4 — perfect deniability, and
> Node2 gets an unsolicited reply it never asked for. That's IP spoofing
> working exactly as the spec asks."

**✓ Rubric item earned:** IP Spoofing.

---

## Demo 4 — Sniffing Attack

**Rubric:** Sniffing attack (1 pt)
**Proves:** Node1 can enable promiscuous mode and capture frames not
addressed to its MAC.

**On Node1's tab:**

```
sniff on
```

**Switch to Node2's tab:**

```
ping 0x11
```

*(We ping the router — any frame on LAN1 not addressed to Node1 works.)*

**Switch back to Node1.** Point at:

```
[Node1 ] [Sniffed] Frame [N2 -> R1 | 14 bytes]
[Node1 ]   └─ Packet [0x13 -> 0x11 | ICMP | 10 bytes]
[Node1 ]      └─ Ping REQUEST seq=1

[Node1 ] [Sniffed] Frame [R1 -> N2 | 14 bytes]
[Node1 ]   └─ Packet [0x11 -> 0x13 | ICMP | 10 bytes]
[Node1 ]      └─ Ping REPLY seq=1
```

> "Node1 just captured **both halves** of a ping between Node2 and the
> router. Notice the destination MACs: `R1` and `N2`. Neither is `N1`. In a
> real switched network Node1 wouldn't see these at all, but LAN Ethernet
> is a shared broadcast medium — our emulator faithfully models that — and
> Node1's promiscuous mode skips the normal MAC filter."

*Leave sniffing ON for Demo 9 if you want to save time, otherwise:*

```
sniff off
```

**✓ Rubric item earned:** Sniffing attack.

---

## Demo 5 — Firewall Runtime Rules

**Rubric:** Firewall (2 pts) — **must be runtime-configurable**.
**Proves:** Node3 has a packet filter whose rules can be added and removed
live from the CLI, blocking traffic by source IP.

**On Node3's tab:**

```
fw status
```

Point at:

```
┌─ Firewall Status ────────────────────
│  State      : ON  (filtering)
│  Blocked IPs: (none)
└──────────────────────────────────────
```

> "Empty blocklist — Node3 currently accepts everything."

**Still on Node3:**

```
fw add block src 0x13
```

> "Now Node3 drops every packet with source IP `0x13` — that's Node2."

**Switch to Node2's tab:**

```
ping 0x22
```

*Wait a few seconds.*

> "Node2 sends the ping, but no reply comes back. Why?"

**Switch to Node3.** Point at:

```
[Node3 ] SECURITY: Firewall dropped packet: 0x13 -> 0x22
```

> "Node3's firewall saw the source IP in the blocklist and dropped the
> packet **before** the ping handler ever ran. Node2 is deaf to Node3 now."

**On Node1's tab:**

```
ping 0x22
```

**Point at Node1:**

```
[Node1 ] RX Ping reply from 0x22 (seq=1, RTT=Xms)
```

> "But Node1 can still reach Node3 — only `0x13` is blocked. Default-allow
> policy, explicit blocks only. Now watch me remove the rule live."

**On Node3:**

```
fw remove block src 0x13
fw status
```

Point at the now-empty blocklist.

**On Node2:**

```
ping 0x22
```

> "And Node2 is back in business. No restart, no config file — pure
> runtime."

**✓ Rubric item earned:** Firewall with runtime add/remove.

---

> **PART 1 DONE.** Take a breath. You just earned 13 of the 21 points.
> Transition line: *"That covers the required rubric. Now let's talk about
> what we built on top of it for the open category."*

---

# PART 2 — OPEN CATEGORY (8 min)

Our open category is a three-pronged **Intrusion Detection System** that sits
on the router and inspects every forwarded packet, plus **confidentiality via
encrypted messaging** that defeats the sniffer from Demo 4.

The IDS uses **DHCP snooping** — it builds its ground-truth MAC↔IP binding
table by watching DHCPACK messages flow through the router. This is exactly
how real managed switches implement Dynamic ARP Inspection and DHCP Snooping
ACLs. It means we don't trust a static config file; we trust what the DHCP
server actually leased.

---

## Demo 6 — IDS Cross-LAN Spoof Detection (Active)

**Open-category feature:** Ingress-filtering IDS with active drop mode.
**Proves:** The IDS notices when a packet's source IP belongs to a different
LAN than the interface the packet arrived on.

**On Router's tab:**

```
ids on
ids mode active
```

Point at:

```
[Router] IDS ON — monitoring traffic
[Router] IDS mode: ACTIVE (drop suspicious)
```

**On Node1's tab:**

```
spoof on 0x22
ping 0x32
```

*Narrate:*

> "Node1 is on LAN1 but pretending to be `0x22` — which belongs to LAN2.
> The packet arrives at the router on interface R1, and the source IP says
> LAN2. That's physically impossible. Watch the router."

**Switch to Router.** Point at the atomic security event:

```
[Router] SECURITY: IDS Alert — IP spoof detected: 0x22 claims LAN2 but arrived on LAN1 (MAC: N1)
[Router] SECURITY: IDS Alert — MAC-IP mismatch: N1 sent packet with source IP 0x22 (snooped lease: 0x12)
[Router] SECURITY: IDS Action — Dropped packet from 0x22
```

> "Two independent checks fired on the same packet: the **cross-LAN check**
> caught the LAN-ID mismatch, and the **DHCP snooping check** noticed that
> MAC `N1` is supposed to be using `0x12` — not `0x22`. In active mode the
> router drops the packet before forwarding. Node4 never sees it."

**Switch to Node4's tab** briefly — nothing there.

**Back on Node1:**

```
spoof off
```

**✓ Open-category feature demonstrated:** IDS ingress filtering + active drop.

---

## Demo 7 — IDS Same-LAN Spoof via DHCP Snooping

**Open-category feature:** DHCP-snooping-backed MAC-IP verification.
**Proves:** The IDS catches spoofing that the cross-LAN check alone cannot —
specifically, an attacker impersonating another node on the **same** LAN.

*IDS should still be ON and active from Demo 6. If not, re-enable.*

**On Router's tab:**

```
ids status
```

Point at:

```
│  Snooped leases : 4
│    • N1  →  0x12
│    • N2  →  0x13
│    • N3  →  0x22
│    • N4  →  0x32
```

> "This binding table came from watching the four DHCPACKs during boot.
> Nothing hard-coded. Now watch what happens when Node1 steals Node2's
> identity **on the same LAN**."

**On Node1's tab:**

```
spoof on 0x13
ping 0x22
```

*Narrate:*

> "`0x13` is Node2, also on LAN1. So the cross-LAN check is going to pass —
> same LAN, no structural mismatch. But the snooping check knows that MAC
> `N1` was leased `0x12`, not `0x13`. Watch."

**Switch to Router.** Point at:

```
[Router] SECURITY: IDS Alert — MAC-IP mismatch: N1 sent packet with source IP 0x13 (snooped lease: 0x12)
[Router] SECURITY: IDS Action — Dropped packet from 0x13
```

> "Only the MAC-IP alert fired this time — the cross-LAN check was silent
> because `0x13` genuinely belongs to LAN1. But the snooping table says
> 'that MAC holds a different lease,' so we drop it anyway. This mirrors
> **Dynamic ARP Inspection** on a production switch."

**On Node1:**

```
spoof off
```

> "Without DHCP snooping, same-LAN spoofing is basically undetectable
> without deep packet inspection. This is the single strongest piece of
> our open category."

**✓ Open-category feature demonstrated:** DHCP snooping + MAC-IP binding verification.

---

## Demo 8 — IDS Ping-Flood Detection

**Open-category feature:** Rate-limiting IDS with sliding-window flood
detection.
**Proves:** Sustained ICMP bursts from a single source trip a threshold-based
alert and get dropped.

**On Node1's tab:**

```
flood 0x22 20
```

*Narrate:*

> "Node1 is about to fire 20 pings at Node3 as fast as possible — no
> 1-second delay. The IDS tracks ping counts per source IP in a 5-second
> sliding window. The threshold is 10. So the first nine pings go through
> normally, then starting with the tenth the IDS kicks in."

**Switch to Router.** Scroll up slightly if needed — point at the pattern:

```
[Router] SECURITY: IDS Alert — Ping flood detected: 10 pings from 0x12 in 5000ms (threshold: 10)
[Router] SECURITY: IDS Action — Dropped packet from 0x12

[Router] SECURITY: IDS Alert — Ping flood detected: 11 pings from 0x12 in 5000ms (threshold: 10)
[Router] SECURITY: IDS Action — Dropped packet from 0x12

…
```

> "Every ping past the 10th is logged AND dropped. Node3 only sees the
> first nine — we've successfully rate-limited a denial-of-service
> attempt."

**Optional — on Router:**

```
ids status
```

Point at the updated alert counters (flood alerts should be ≥ 11).

**✓ Open-category feature demonstrated:** DoS flood detection + active drop.

---

## Demo 9 — Encrypted Messaging vs Sniffing

**Open-category feature:** Application-layer confidentiality via XOR cipher.
**Proves:** Even when a sniffer captures every frame, encrypted payloads stay
unreadable on the wire — the legitimate receiver still decrypts normally.

*Turn the IDS off first so the flood demo's leftover flood counter doesn't
muddy this demo (optional, cosmetic).*

**On Router's tab:**

```
ids off
```

**On Node1's tab:**

```
sniff on
```

**On Node2's tab — first, a plaintext message to Node4:**

```
msg 0x32 Secret plans inside
```

**Switch to Node1** — point at:

```
[Node1 ] [Sniffed] Frame [N2 -> R1 | 25 bytes]
[Node1 ]   └─ Packet [0x13 -> 0x32 | DATA | 21 bytes]
[Node1 ]      └─ Message [PLAIN] "Secret plans inside"
```

> "Plaintext. Node1 reads the message in full. This is the confidentiality
> problem that sniffing causes."

**Back on Node2 — now encrypt the same message:**

```
emsg 0x32 Secret plans inside
```

**Switch to Node1** — point at:

```
[Node1 ] [Sniffed] Frame [N2 -> R1 | 25 bytes]
[Node1 ]   └─ Packet [0x13 -> 0x32 | DATA | 21 bytes]
[Node1 ]      └─ Message [ENCRYPTED] "	?9(?.z*6;4)z34)3>?" (cannot read — encrypted)
```

> "Exact same frame length, same source, same destination — but the
> message body is XOR-encrypted with a shared key. Node1's sniffer can see
> **that** a message was sent, but it cannot read **what** was sent."

**Switch to Node4** — point at:

```
[Node4 ] RX Encrypted message from 0x13: "Secret plans inside"
```

> "Meanwhile, Node4 — the legitimate receiver — shares the key, decrypts
> on arrival, and reads the plaintext normally. Confidentiality preserved."

**Back on Node1:**

```
sniff off
```

**✓ Open-category feature demonstrated:** Encryption as a defense against sniffing.

---

## Wrap-Up (1 min)

*Stand back from the keyboard. Deliver this as a closing statement, not
reading off the screen.*

> "To recap: we built IP over Ethernet from scratch in Java over UDP
> sockets — eight processes, three LANs, one multi-interface router, a
> real DHCP handshake for IP assignment. We implemented every required
> security feature — spoofing, sniffing, firewall — and then built an
> IDS with **three independent detection strategies** backed by DHCP
> snooping, plus payload encryption for confidentiality. Every packet
> you just saw was serialized byte-for-byte in our code, and every demo
> is reproducible from `start-demo.sh`.
>
> One hundred and seventy unit tests pass. The code lives under `src/`,
> the full scenario reference is in `GUIDE.md`, and I'm happy to take
> questions."

---

## Q&A Cheat Sheet

Questions a grader is likely to ask, and the shortest correct answer you
can give:

**Q: How does the frame format match the spec?**
> Source MAC 2 bytes, destination MAC 2 bytes, data length 1 byte, payload
> up to 256 bytes — max 261 bytes total. See `EthernetFrame.java`, and it's
> covered by `EthernetFrameTest` which round-trips every field.

**Q: What's in the IP packet header?**
> Source IP 1 byte, destination IP 1 byte, protocol 1 byte, data length 1
> byte, data up to 256 bytes. Protocol `0x01` is ICMP/ping per spec; we
> also defined `0x02` DATA, `0x03` DHCP, `0x04` ARP for the open-category
> work.

**Q: Is the router using a real routing protocol?**
> No — the spec explicitly allows hard-coded routing. We route by the high
> nibble of the destination IP: `0x1_` → LAN1, `0x2_` → LAN2, `0x3_` →
> LAN3.

**Q: How does a node know the destination MAC for cross-LAN traffic?**
> Cross-LAN traffic is always addressed to the local gateway (R1/R2/R3 on
> the sender's LAN). The router handles MAC rewriting on the egress
> interface. Same-LAN traffic uses our custom ARP implementation with a
> local cache.

**Q: Why DHCP instead of hard-coded IPs? The spec says hard-coded is fine.**
> DHCP was our open-category sandbox for the IDS. Real switches implement
> **DHCP Snooping ACLs** and **Dynamic ARP Inspection** — both require a
> DHCP server you trust. We wanted a faithful version of that integration.
> Even so, the DHCP server is **MAC-pinned** so each node always receives
> its spec-mandated IP deterministically.

**Q: What happens if a DHCP lease expires?**
> The server reaps it lazily on the next DHCP message, and — this is
> important — it calls `IDS.forgetDHCPLease(mac)` via a callback. So the
> IDS binding disappears at the same time as the lease, which means the
> MAC-IP check will correctly skip the now-unknown MAC instead of
> false-positiving on a re-issued lease.

**Q: How does the encryption work?**
> Each `TextMessage` has a 1-byte "encrypted" flag. In encrypted mode, the
> text is XOR'd with a 1-byte shared key before serialization. It's a
> **demonstration** cipher, not a production one — we chose it for
> clarity, not cryptographic strength. A sniffer sees the raw ciphertext
> because we use `decodeRaw()` which skips the XOR step.

**Q: Can I see the tests run?**
> `mvn test` — 170 pass. 14 test classes across common types, DHCP,
> firewall, IDS, and a DHCP client integration test.

**Q: What if I wanted to add a fifth node?**
> Add its MAC/IP pair to `AddressTable.PREFERRED_IP`, wire a new LAN
> emulator if it's on a new segment, and write a `Node5` class extending
> `Node`. The open-category FAQ explicitly says you can modify the
> topology.

---

## Panic Buttons

Things that can go wrong live, and the exact fix.

### A node has the wrong IP
*(e.g. Node1 shows `0x13` instead of `0x12`)*
Shouldn't happen with the MAC-pinned DHCP server, but if it does:
1. `quit` that node
2. Restart just that node: `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node1`
3. Watch for the second banner showing the corrected IP

### A ping gets no reply
Most common cause: you're still in **spoof mode** from a previous demo.
On the sender's tab:
```
spoof off
```

Second most common: **IDS is in active mode** and the packet tripped a
check. On the Router:
```
ids status
```
If the spoof/MAC-IP/flood counters incremented, you know why. Disable or
reset:
```
ids off
```

### Node3's firewall is still blocking from a previous demo
```
fw status                     # see what's blocked
fw remove block src 0x13      # remove a specific rule
fw off                        # or disable entirely
```

### A terminal died
Just relaunch it — each process is independent. The DHCP server will
re-lease the spec IP to the returning MAC.

### The whole thing is broken
1. `quit` every process (or kill the terminals)
2. `bash start-demo.sh` again
3. Wait for all four DHCP ACK banners before demoing

### You need to reset between demos
No explicit reset is required — the demos are designed to be independent.
The only per-demo cleanup you should do is:
- `spoof off` after Demos 3, 6, 7
- `sniff off` after Demos 4, 9
- `fw remove block src 0x13` (or `fw off`) after Demo 5
- `ids off` (optional) before Demo 9 to clear flood counters visually

---

## 15-Minute Fallback Script

If you run short on time, cut to this curated path. Every rubric item is
still covered; only the open-category demonstrations shrink.

| Time | Demo | Command |
|------|------|---------|
| 0:00 | Boot | `bash start-demo.sh` |
| 1:30 | Demo 1 (intra-LAN ping) | `ping 0x12` on Node2 |
| 3:00 | Demo 2 (cross-LAN ping) | `ping 0x22` on Node2 |
| 5:00 | Demo 3 (spoof) | `spoof on 0x13 ; ping 0x32 ; spoof off` on Node1 |
| 7:00 | Demo 4 (sniff) | `sniff on` Node1, `ping 0x11` Node2 |
| 8:30 | Demo 5 (firewall) | `fw add block src 0x13` Node3, `ping 0x22` Node2, `fw remove …` |
| 10:30 | Demo 7 (DHCP-snoop IDS) | `ids on ; ids mode active` Router, `spoof on 0x13 ; ping 0x22` Node1 |
| 12:30 | Demo 9 (encryption) | `msg 0x32 …` + `emsg 0x32 …` with Node1 sniffing |
| 14:30 | Wrap-up | Closing statement |

Skipped in this path: Demo 6 (redundant with Demo 7's cross-LAN alert
that also fires), Demo 8 (flood — optional if short on time).

---

*For the full 17-scenario reference including lease expiry, `--static`
mode, and protocol-level deep dives, see `GUIDE.md`. For the code, start
with `Router.java`, `Node.java`, and `IntrusionDetectionSystem.java`.*
