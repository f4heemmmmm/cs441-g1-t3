# Part D: Sniffing

**Goal:** Demonstrate that Node1 can observe all traffic on LAN1 in promiscuous mode, including frames not addressed to it.

**Terminals used:** T5 (Node1), T6 (Node2)

---

## Step 1: Enable sniffing on Node1

**On T5 (Node1), type:**
```
sniff on
```

### Expected on T5 - Node1
```
[...][N1] Sniffing ON: will show all frames on LAN
```

---

## Step 2: Node2 sends a ping through LAN1

**On T6 (Node2), type:**
```
ping 0x22 count 1
```

Node2 (IP=0x13) sends a ping to Node3 (IP=0x22). The frame is addressed to R1 (the gateway) and broadcast on LAN1.

---

## What to observe

### T5 - Node1 (sniffing Node2's traffic)
Node1 sees Node2's frame even though it's addressed to R1 (not N1). The `[SNIFF]` log shows the full frame and IP details, followed by the normal drop:

```
[...][N1] Received frame: Ethernet [dst=R1, src=N2, 14 bytes, proto=PING]
[...][N1][SNIFF] Ethernet [dst=R1, src=N2, 14 bytes, proto=PING] | IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][N1] Dropped frame (MAC mismatch: dst=R1, mine=N1)
```

When Node3's reply comes back through LAN1 (addressed to N2), Node1 also sniffs it:
```
[...][N1] Received frame: Ethernet [dst=N2, src=R1, 14 bytes, proto=PING]
[...][N1][SNIFF] Ethernet [dst=N2, src=R1, 14 bytes, proto=PING] | IP [0x22 -> 0x13, proto=PING, 10 bytes]
[...][N1] Dropped frame (MAC mismatch: dst=N2, mine=N1)
```

### T6 - Node2 (normal operation)
Node2 sees its own ping go out and the reply come back normally:
```
[...][N2] Sent Ping REQUEST #0 to 0x22
[...][N2] Ping reply from 0x22: seq=0, RTT=<N>ms
```

---

## Step 3: Disable sniffing

**On T5 (Node1), type:**
```
sniff off
```

```
[...][N1] Sniffing OFF
```

---

## Key Takeaway

On a broadcast LAN (hub), every endpoint receives every frame. Normally endpoints drop frames not addressed to them. Sniffing mode (promiscuous mode) captures and logs all traffic before MAC filtering, allowing the attacker to observe other nodes' communications.

---

**Next:** [Part E: Firewall](05_E_FIREWALL.md)
