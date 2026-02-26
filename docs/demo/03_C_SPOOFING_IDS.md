# Part C: Spoofing + IDS Detection

**Goal:** Demonstrate Node1 spoofing its IP address, and the Router's IDS detecting the spoof.

**Terminals used:** T4 (Router), T5 (Node1), T7 (Node3)

---

## Step 1: Enable IDS on the Router (passive mode)

**On T4 (Router), type these 3 commands:**
```
ids on
ids mode passive
ids status
```

### Expected on T4 - Router
```
[...][ROUTER] IDS enabled
[...][ROUTER] IDS mode: PASSIVE (alert only)
[...][ROUTER] IDS status: ENABLED, mode=PASSIVE (alert only), flood threshold=10 pings/5s
```

---

## Step 2: Enable spoofing on Node1

**On T5 (Node1), type:**
```
spoof on 0x13
```

### Expected on T5 - Node1
```
[...][N1] Spoofing ON: pretending to be IP 0x13
```

Node1's real IP is 0x12, but it will now claim to be 0x13 (Node2's IP).

---

## Step 3: Send a spoofed ping

**On T5 (Node1), type:**
```
ping 0x22 count 1
```

### Expected on T4 - Router (IDS alert!)
The router detects that MAC=N1 should have IP=0x12 but the packet claims IP=0x13:
```
[...][ROUTER] Received frame on R1: Ethernet [dst=R1, src=N1, 14 bytes, proto=PING]
[...][ROUTER] Accepted packet on R1: IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][ROUTER][IDS] [ALERT] Spoof suspected: ethSrc=N1 expectedIP=0x12 but ipSrc=0x13 dst=0x22
[...][ROUTER] Forwarding: R1 -> R2 (dstMAC=N3) IP [0x13 -> 0x22, proto=PING, 10 bytes]
```

In passive mode, the packet is still forwarded despite the alert.

### Expected on T7 - Node3
Node3 sees the ping as coming from 0x13 (Node2's IP), not 0x12 (Node1's real IP):
```
[...][N3] Accepted packet: IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][N3] Received Ping REQUEST [seq=0] from 0x13
```

---

## Step 4 (Optional): Active mode blocks spoofed traffic

**On T4 (Router), type:**
```
ids mode active
```

**On T5 (Node1), type:**
```
ping 0x22 count 1
```

### Expected on T4 - Router (IDS blocks the packet!)
```
[...][ROUTER] Accepted packet on R1: IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][ROUTER][IDS] [ALERT] Spoof suspected: ethSrc=N1 expectedIP=0x12 but ipSrc=0x13 dst=0x22
[...][ROUTER][IDS] [BLOCK] Dropping srcIP=0x13 for 10s
[...][ROUTER] Packet dropped by IDS (see IDS alert above)
```

The packet is now dropped and never reaches Node3.

---

## Step 5: Disable spoofing

**On T5 (Node1), type:**
```
spoof off
```

```
[...][N1] Spoofing OFF
```

---

## Key Takeaway

IP spoofing changes the source IP in the IP header but not the Ethernet MAC. The IDS compares MAC vs IP and detects the mismatch. In passive mode it alerts only; in active mode it also blocks the offending traffic.

---

**Next:** [Part D: Sniffing](04_D_SNIFFING.md)
