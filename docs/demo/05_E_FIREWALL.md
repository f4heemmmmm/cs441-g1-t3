# Part E: Firewall

**Goal:** Demonstrate Node3's firewall blocking incoming packets by source IP.

**Terminals used:** T6 (Node2), T7 (Node3)

---

## Step 1: Verify firewall is empty

**On T7 (Node3), type:**
```
fw list
```

### Expected on T7 - Node3
```
[...][N3] Firewall rules: (none)
```

---

## Step 2: Node2 pings Node3 (should succeed)

**On T6 (Node2), type:**
```
ping 0x22 count 1
```

### Expected on T7 - Node3
The ping is accepted and replied to:
```
[...][N3] Received frame: Ethernet [dst=N3, src=R2, 14 bytes, proto=PING]
[...][N3] Accepted packet: IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][N3] Received Ping REQUEST [seq=0] from 0x13
[...][N3] Sent Ping REPLY to 0x13
```

### Expected on T6 - Node2
```
[...][N2] Ping reply from 0x22: seq=0, RTT=<N>ms
```

---

## Step 3: Add a firewall rule to block Node2

**On T7 (Node3), type:**
```
fw add block src 0x13
```

### Expected on T7 - Node3
```
[...][N3] Firewall rule added: BLOCK packets from src=0x13
```

---

## Step 4: Node2 pings Node3 again (should be blocked)

**On T6 (Node2), type:**
```
ping 0x22 count 1
```

### Expected on T7 - Node3
The frame arrives and MAC filtering passes, but the firewall blocks it:
```
[...][N3] Received frame: Ethernet [dst=N3, src=R2, 14 bytes, proto=PING]
[...][N3] Accepted packet: IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][N3][FIREWALL] Blocked packet: src=0x13, dst=0x22, proto=1
```

No reply is sent. Node2 will not receive a response.

---

## Step 5: Remove the firewall rule

**On T7 (Node3), type:**
```
fw del block src 0x13
```

### Expected on T7 - Node3
```
[...][N3] Firewall rule removed: BLOCK packets from src=0x13
```

---

## Step 6: Node2 pings Node3 again (should succeed)

**On T6 (Node2), type:**
```
ping 0x22 count 1
```

### Expected on T6 - Node2
The reply comes through again:
```
[...][N2] Ping reply from 0x22: seq=0, RTT=<N>ms
```

---

## Key Takeaway

The firewall operates at the IP layer, after Ethernet MAC filtering. It blocks packets based on source IP address. Rules can be added and removed dynamically via CLI commands. When a packet is blocked, it is silently dropped (no reply sent to the sender).

---

**Next:** [Part F: IDS Ping Flood](06_F_PING_FLOOD.md)
