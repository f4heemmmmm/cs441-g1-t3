# Part B: Cross-LAN Ping (Node1 -> Node3)

**Goal:** Demonstrate IP routing across LANs. Node1 (LAN1, IP=0x12) pings Node3 (LAN2, IP=0x22) via the Router.

**Terminals used:** T4 (Router), T5 (Node1), T7 (Node3)

---

## Step 1: Node1 pings Node3

**On T5 (Node1), type:**
```
ping 0x22 count 1
```

---

## What to observe (in chronological order)

### T5 - Node1 (sends request)
Node1 addresses the frame to R1 (its gateway for off-LAN traffic):
```
[...][N1] Sent frame: Ethernet [dst=R1, src=N1, 14 bytes, proto=PING]
[...][N1] Sent Ping REQUEST #0 to 0x22
```

### T4 - Router (receives on R1, forwards out R2)
The router receives the frame on R1, decodes the IP packet, and forwards it out R2 to Node3:
```
[...][ROUTER] Received frame on R1: Ethernet [dst=R1, src=N1, 14 bytes, proto=PING]
[...][ROUTER] Accepted packet on R1: IP [0x12 -> 0x22, proto=PING, 10 bytes]
[...][ROUTER] Forwarding: R1 -> R2 (dstMAC=N3) IP [0x12 -> 0x22, proto=PING, 10 bytes]
[...][ROUTER] Sent frame on R2: Ethernet [dst=N3, src=R2, 14 bytes, proto=PING]
```

### T7 - Node3 (receives request, sends reply)
Node3 receives the ping request and sends a reply back through R2:
```
[...][N3] Received frame: Ethernet [dst=N3, src=R2, 14 bytes, proto=PING]
[...][N3] Accepted packet: IP [0x12 -> 0x22, proto=PING, 10 bytes]
[...][N3] Received Ping REQUEST [seq=0] from 0x12
[...][N3] Sent frame: Ethernet [dst=R2, src=N3, 14 bytes, proto=PING]
[...][N3] Sent Ping REPLY to 0x12
```

### T4 - Router (receives reply on R2, forwards out R1)
The router forwards the reply back to LAN1:
```
[...][ROUTER] Received frame on R2: Ethernet [dst=R2, src=N3, 14 bytes, proto=PING]
[...][ROUTER] Accepted packet on R2: IP [0x22 -> 0x12, proto=PING, 10 bytes]
[...][ROUTER] Forwarding: R2 -> R1 (dstMAC=N1) IP [0x22 -> 0x12, proto=PING, 10 bytes]
[...][ROUTER] Sent frame on R1: Ethernet [dst=N1, src=R1, 14 bytes, proto=PING]
```

### T5 - Node1 (receives reply)
Node1 receives the ping reply and calculates RTT:
```
[...][N1] Received frame: Ethernet [dst=N1, src=R1, 14 bytes, proto=PING]
[...][N1] Accepted packet: IP [0x22 -> 0x12, proto=PING, 10 bytes]
[...][N1] Received Ping REPLY [seq=0] from 0x22
[...][N1] Ping reply from 0x22: seq=0, RTT=<N>ms
```

---

## Key Takeaway

The packet traverses: Node1 -> LAN1 -> R1 (Router) -> R2 -> LAN2 -> Node3, and the reply takes the reverse path. The Router correctly identifies the egress interface based on the destination IP prefix.

---

**Next:** [Part C: Spoofing + IDS](03_C_SPOOFING_IDS.md)
