# Protocol Specifications

## Ethernet Frame Format

```
Offset  Size  Field    Description
0       2     dstMAC   Destination MAC (2 ASCII bytes, e.g. "N1")
2       2     srcMAC   Source MAC (2 ASCII bytes, e.g. "R1")
4       1     dataLen  Length of data payload (0-255)
5       0-255 data     Payload (typically an IP packet)
```

**Total size**: 5 + dataLen bytes (max 260 bytes)

### Example

Frame from R1 to N1 carrying 14 bytes of data:
```
Bytes: [4E 31] [52 31] [0E] [data...]
         N  1    R  1   14
```

### Broadcast MAC

`FF` is the broadcast MAC address. Frames with dstMAC=`FF` are accepted by all endpoints.

---

## IP Packet Format

Carried inside the Ethernet frame's data field.

```
Offset  Size  Field     Description
0       1     srcIP     Source IP address (1 byte)
1       1     dstIP     Destination IP address (1 byte)
2       1     protocol  Protocol number (0=data, 1=ping)
3       1     dataLen   Length of data payload (0-252)
4       0-252 data      Payload
```

**Total size**: 4 + dataLen bytes

### IP Address Scheme

The high nibble determines the LAN:
- `0x1_` → LAN1 (e.g., 0x11=R1, 0x12=N1, 0x13=N2)
- `0x2_` → LAN2 (e.g., 0x21=R2, 0x22=N3)
- `0x3_` → LAN3 (e.g., 0x31=R3, 0x32=N4)

### Example

IP packet from Node1 (0x12) to Node3 (0x22), ping protocol:
```
Bytes: [12] [22] [01] [0A] [ping data...]
       src  dst  proto len
```

---

## Ping Protocol (protocol = 1)

Carried inside IP packet data field.

### Ping Request (type = 0x01)

```
Offset  Size  Field      Description
0       1     type       0x01 (request)
1       1     seq        Sequence number (0-255)
2       8     timestamp  System.nanoTime() of sender (big-endian long)
```

### Ping Reply (type = 0x02)

```
Offset  Size  Field      Description
0       1     type       0x02 (reply)
1       1     seq        Echoed sequence number
2       8     timestamp  Echoed sender timestamp
```

**Total size**: 10 bytes

### RTT Calculation

The sender records `System.nanoTime()` in the timestamp field. When the reply arrives, RTT is computed as:
```
rtt_ms = (System.nanoTime() - echoed_timestamp) / 1_000_000
```

---

## LAN Emulator Registration Protocol

Each endpoint registers with its LAN emulator on startup.

### Registration Message (plain text)
```
REGISTER <MAC> <port>
```

Example: `REGISTER N1 6001`

The LAN emulator records the MAC-to-port mapping and uses it for broadcasting frames.

---

## Complete Wire Example

### Node1 pings Node3 (cross-LAN)

**Step 1: Node1 → LAN1 emulator**
```
Ethernet: dst=R1 src=N1 dataLen=14
  IP: src=0x12 dst=0x22 proto=1 dataLen=10
    Ping: type=REQUEST seq=0 timestamp=<nanoTime>
```

**Step 2: LAN1 emulator broadcasts to all LAN1 endpoints**
- R1 accepts (dstMAC matches)
- N1 receives and drops (own frame echoed back, dstMAC=R1 != N1)
- N2 receives and drops (dstMAC=R1 != N2)

**Step 3: Router forwards R1 → R2**
```
Ethernet: dst=N3 src=R2 dataLen=14
  IP: src=0x12 dst=0x22 proto=1 dataLen=10
    Ping: type=REQUEST seq=0 timestamp=<same>
```

**Step 4: LAN2 emulator broadcasts**
- N3 accepts (dstMAC matches)
- R2 receives and drops (own frame echoed back)

**Step 5: Node3 sends reply via R2**
```
Ethernet: dst=R2 src=N3 dataLen=14
  IP: src=0x22 dst=0x12 proto=1 dataLen=10
    Ping: type=REPLY seq=0 timestamp=<echoed>
```

**Step 6: Router forwards R2 → R1**
```
Ethernet: dst=N1 src=R1 dataLen=14
  IP: src=0x22 dst=0x12 proto=1 dataLen=10
    Ping: type=REPLY seq=0 timestamp=<echoed>
```

**Step 7: Node1 receives reply, prints RTT**
