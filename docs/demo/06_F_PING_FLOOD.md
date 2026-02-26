# Part F: IDS Ping Flood Detection

**Goal:** Demonstrate the router's IDS detecting and blocking a ping flood attack.

**Terminals used:** T4 (Router), T5 (Node1)

---

## Step 1: Configure IDS for flood detection

**On T4 (Router), type these 3 commands:**
```
ids on
ids thresholds pingflood 10 per 5
ids mode active
```

### Expected on T4 - Router
```
[...][ROUTER] IDS enabled
[...][ROUTER] IDS ping flood threshold set: max 10 pings per 5s
[...][ROUTER] IDS mode: ACTIVE (alert + block)
```

This means: if more than 10 ping requests from the same source IP arrive within a 5-second window, the IDS will alert and block that IP.

---

## Step 2: Node1 launches a ping flood

**On T5 (Node1), type:**
```
pingflood 0x22 rate 20 duration 5
```

This sends 20 pings per second for 5 seconds (100 total pings).

### Expected on T5 - Node1
```
[...][N1] Starting ping flood to 0x22 rate=20/s duration=5s
[...][N1] Sent frame: Ethernet [dst=R1, src=N1, 14 bytes, proto=PING]
... (many more sent frames)
[...][N1] Ping flood complete: sent 100 pings
```

---

## What to observe

### T4 - Router (IDS alerts and blocks!)

After the 11th ping, the IDS detects the flood:
```
[...][ROUTER][IDS] [ALERT] Ping flood: srcIP=0x12 count=11/5s
[...][ROUTER][IDS] [BLOCK] Dropping srcIP=0x12 for 10s
[...][ROUTER] Packet dropped by IDS (see IDS alert above)
```

All subsequent pings from Node1 are automatically dropped for 10 seconds:
```
[...][ROUTER][IDS] [BLOCK] Dropping srcIP=0x12 (currently blocked by IDS)
```

---

## Step 3: Verify block status

**On T4 (Router), type:**
```
ids status
```

### Expected on T4 - Router
```
[...][ROUTER] IDS status: ENABLED, mode=ACTIVE (alert + block), flood threshold=10 pings/5s
[...][ROUTER] IDS blocked IPs:
[...][ROUTER]   0x12 (<N>s remaining)
```

---

## Step 4: Wait for block to expire

After 10 seconds, the block expires automatically. Normal pings from Node1 will work again.

**On T5 (Node1), type (after waiting 10 seconds):**
```
ping 0x22 count 1
```

The ping should go through normally.

---

## Key Takeaway

The IDS uses a sliding window to count ping requests per source IP. When the threshold is exceeded, active mode automatically blocks the offending IP for a configurable duration. This provides automated defense against denial-of-service via ping flooding.

---

**Demo complete.** To shut down all processes, type `quit` in each terminal.
