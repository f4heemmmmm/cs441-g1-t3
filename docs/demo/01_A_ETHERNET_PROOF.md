# Part A: Ethernet Proof (Broadcast + MAC Drop)

**Goal:** Demonstrate that the LAN emulator broadcasts frames to all endpoints, and that each endpoint drops frames not addressed to its MAC.

**Terminals used:** T4 (Router), T5 (Node1), T6 (Node2)

---

## Step 1: Router sends a raw Ethernet frame to Node1

**On T4 (Router), type:**
```
ethsend lan1 dst N1 msg "hello"
```

---

## What to observe

### T4 - Router
```
[...][ROUTER] Sent frame on R1: Ethernet [dst=N1, src=R1, 9 bytes, proto=DATA]
[...][ROUTER] Sent raw Ethernet frame to MAC=N1 via LAN1
```

### T5 - Node1 (Attacker)
Node1's MAC is N1, so the frame is **accepted**:
```
[...][N1] Received frame: Ethernet [dst=N1, src=R1, 9 bytes, proto=DATA]
[...][N1] Accepted packet: IP [0x11 -> 0x00, proto=DATA, 5 bytes]
[...][N1] Received data message: "hello"
```

### T6 - Node2
Node2's MAC is N2, but the frame is addressed to N1. It is **dropped** after broadcast:
```
[...][N2] Received frame: Ethernet [dst=N1, src=R1, 9 bytes, proto=DATA]
[...][N2] Dropped frame (MAC mismatch: dst=N1, mine=N2)
```

---

## Key Takeaway

Both Node1 and Node2 received the frame (the LAN1 emulator broadcasts to all registered endpoints). However, only Node1 accepted it because the destination MAC matched. Node2 correctly dropped it after logging the mismatch. This proves the broadcast-and-filter behavior.

---

**Next:** [Part B: Cross-LAN Ping](02_B_CROSS_LAN_PING.md)
