# Security Features

## A) IP Spoofing (Node1)

### Description
Node1 acts as an attacker that can forge the source IP in outgoing packets. When spoofing is enabled, Node1 replaces its real IP (0x12) with a chosen IP (e.g., 0x13 to impersonate Node2).

The Ethernet source MAC remains Node1's real MAC ("N1"), which is how the IDS can detect the spoof.

### Commands
```
spoof on 0x13    # Pretend to be IP 0x13 (Node2)
spoof off        # Disable spoofing, use real IP
```

### Behavior
- When ON: outgoing IP packets use the spoofed source IP
- Ethernet srcMAC stays "N1" (real MAC)
- Registration with LAN emulator is unchanged
- The mismatch between MAC and IP is detectable by the router IDS

### Example
```
N1> spoof on 0x13
[...][N1] Spoofing ON: pretending to be IP 0x13

N1> ping 0x22 count 1
# Sends: Ethernet [dst=R1, src=N1, 14 bytes, proto=PING]
#         IP [0x13 -> 0x22, proto=PING, 10 bytes]
# Node3 sees the ping as coming from 0x13 (Node2's IP)
```

---

## B) Sniffing (Node1)

### Description
Node1 can enable promiscuous mode to observe ALL traffic on its LAN, including frames not addressed to it. Normally, MAC filtering would drop frames with mismatched destination MACs, but sniffing mode logs everything before filtering.

### Commands
```
sniff on     # Enable promiscuous sniffing
sniff off    # Disable sniffing
```

### Behavior
- When ON: every received frame is logged with `[SNIFF]` tag, including full Ethernet and IP headers
- MAC filtering still happens afterward (accepted/dropped is also shown)
- This simulates a NIC in promiscuous mode on a shared medium (hub/broadcast LAN)

### Example
```
N1> sniff on
[...][N1] Sniffing ON: will show all frames on LAN

# When Node2 sends a ping through LAN1:
[...][N1][SNIFF] Ethernet [dst=R1, src=N2, 14 bytes, proto=PING] | IP [0x13 -> 0x22, proto=PING, 10 bytes]
[...][N1] Dropped frame (MAC mismatch: dst=R1, mine=N1)
```

---

## C) Firewall (Node3)

### Description
Node3 has an IP-layer firewall that filters incoming packets by source IP address. When a packet matches a block rule, it is dropped before being processed.

### Commands
```
fw add block src 0x13    # Block packets from IP 0x13
fw del block src 0x13    # Remove block rule
fw list                  # List all active rules
```

### Behavior
- Firewall is checked AFTER MAC filtering but BEFORE IP processing
- Only incoming packets are filtered (not outgoing)
- Blocked packets generate a `[FIREWALL]` log entry
- Multiple rules can coexist

### Example
```
N3> fw add block src 0x13
[...][N3] Firewall rule added: BLOCK packets from src=0x13

# When Node2 (0x13) pings Node3:
[...][N3][FIREWALL] Blocked packet: src=0x13, dst=0x22, proto=1

N3> fw list
[...][N3] Firewall rules (1 active):
[...][N3]   BLOCK src=0x13

N3> fw del block src 0x13
[...][N3] Firewall rule removed: BLOCK packets from src=0x13
```

---

## D) Intrusion Detection System (Router)

### Description
The router includes an IDS engine that inspects forwarded traffic for suspicious activity.

### IDS Rule 1: Spoof Detection (Mandatory)

The router maintains a known MAC-to-IP mapping. When a frame arrives where the Ethernet source MAC doesn't match the expected IP for that MAC, the IDS raises a spoof alert.

**Known mapping:**
| MAC | Expected IP |
|-----|-------------|
| N1 | 0x12 |
| N2 | 0x13 |
| N3 | 0x22 |
| N4 | 0x32 |

**Alert format:**
```
[IDS] [ALERT] Spoof suspected: ethSrc=N1 expectedIP=0x12 but ipSrc=0x13 dst=0x22
```

### IDS Rule 2: Ping Flood Detection

Uses a sliding time window to count ping requests per source IP. If the count exceeds a configurable threshold, alerts are generated.

**Alert format:**
```
[IDS] [ALERT] Ping flood: srcIP=0x12 count=18/5s
```

### IDS Modes

**Passive mode** (default): Alerts are logged but traffic is NOT dropped.

**Active mode**: Alerts trigger automatic blocking of the source IP for 10 seconds.
```
[IDS] [BLOCK] Dropping srcIP=0x12 for 10s
```

### Commands
```
ids on                              # Enable IDS
ids off                             # Disable IDS
ids mode passive                    # Alert only
ids mode active                     # Alert + block
ids status                          # Show current configuration
ids thresholds pingflood 10 per 5   # Set flood threshold
```

### Example
```
ROUTER> ids on
ROUTER> ids mode passive
ROUTER> ids status
[...][ROUTER] IDS status: ENABLED, mode=PASSIVE (alert only), flood threshold=10 pings/5s

# Node1 spoofs as 0x13 and pings Node3:
[...][ROUTER][IDS] [ALERT] Spoof suspected: ethSrc=N1 expectedIP=0x12 but ipSrc=0x13 dst=0x22

# Switch to active mode:
ROUTER> ids mode active
# Repeat spoofed ping:
[...][ROUTER][IDS] [ALERT] Spoof suspected: ethSrc=N1 expectedIP=0x12 but ipSrc=0x13 dst=0x22
[...][ROUTER][IDS] [BLOCK] Dropping srcIP=0x13 for 10s
[...][ROUTER] Packet dropped by IDS (see IDS alert above)
```
