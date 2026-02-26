# Troubleshooting

## Port Conflicts

### Symptom
```
java.net.BindException: Address already in use
```

### Cause
Another process is already using the required UDP port.

### Fix
1. Kill any leftover processes:
   ```bash
   # Find processes using our ports
   lsof -i :5001 -i :5002 -i :5003 -i :6001 -i :6002 -i :6003 -i :6004 -i :6011 -i :6012 -i :6013 -i :8080 -i :9000 -i :3000

   # Kill them
   kill <PID>
   ```

2. Wait a few seconds for the OS to release the ports, then restart.

### Port Assignments
| Port | Process |
|------|---------|
| 5001 | LAN1 Emulator |
| 5002 | LAN2 Emulator |
| 5003 | LAN3 Emulator |
| 6001 | Node1 |
| 6002 | Node2 |
| 6003 | Node3 |
| 6004 | Node4 |
| 6011 | Router R1 interface |
| 6012 | Router R2 interface |
| 6013 | Router R3 interface |
| 8080 | Dashboard HTTP/SSE server (Java) |
| 9000 | Dashboard UDP event collector |
| 3000 | Enhanced Dashboard (NestJS) |

---

## Startup Order

### Problem
Nodes fail to register or frames are lost.

### Fix
Always start in this order:
1. **T1, T2, T3** - LAN emulators first (Lan1Emu, Lan2Emu, Lan3Emu)
2. **T4** - Router second (RouterR)
3. **T5, T6, T7, T8** - Nodes last (Node1, Node2, Node3, Node4)

If a node starts before its LAN emulator, the registration UDP packet will be lost.

---

## No Ping Reply

### Possible causes
1. **Wrong startup order**: LAN emulator must be running before the node registers
2. **Firewall blocking**: Check `fw list` on Node3
3. **IDS blocking**: Check `ids status` on Router; blocked IPs expire after 10s
4. **Spoofing active**: The reply goes to the spoofed IP's node, not the sender

### Debug steps
1. Check LAN emulator (T1/T2/T3): type `list` to see registered endpoints
2. Check router logs (T4): look for "Forwarding:" messages
3. Check destination node logs (T5-T8): look for "Received frame" and "Accepted packet" messages

---

## Build Issues

### Maven not found
```bash
# Install Maven
brew install maven   # macOS
sudo apt install maven   # Ubuntu
```

### Java version mismatch
This project requires Java 17+.
```bash
java -version  # Check version
```

### Compilation warning about system modules
The warning about `--release 17` is harmless and can be ignored.

---

## Frame Not Received

### Check LAN emulator registration
On the LAN emulator terminal, type `list`:
```
list
```
Expected output should show all registered MACs and their ports.

If a device is missing, restart it (it re-registers on startup).

---

## Sniffing Shows Nothing

### Check
- Is sniffing enabled? (`sniff on` on T5 - Node1)
- Is there traffic on LAN1? Sniffing only sees LAN1 traffic.
- Cross-LAN traffic won't appear on LAN1 unless it originates from or is destined for a LAN1 device.

---

## IDS Not Alerting

### Check
1. Is IDS enabled? Run `ids status` on T4 (Router)
2. Is the threshold too high? Try `ids thresholds pingflood 3 per 5` for testing
3. Spoof detection requires the MAC-IP mismatch. If you're not spoofing, no spoof alert will fire.

---

## Dashboard Not Showing Events

### Check
1. Is the Java DashboardServer running (T9)? It must be started before the emulator processes for them to register.
2. Are the emulator processes (T1-T8) running? Events only appear when processes are active.
3. For the enhanced dashboard (T10): Is the Java DashboardServer (T9) running? T10 proxies events from T9 on port 8080.
4. Check browser console for SSE connection errors.

### NestJS Dashboard Won't Start
```bash
# Ensure Node.js 18+ is installed
node --version

# Reinstall dependencies
cd dashboard && rm -rf node_modules && npm install

# Rebuild
npm run build
```

---

## Common Mistakes

1. **Forgetting to build**: Run `mvn clean package -q` before starting processes
2. **Starting in wrong order**: LAN emulators must start first
3. **Using wrong port**: Each process has a specific port assignment
4. **Firewall still active**: Check and clear rules with `fw list` / `fw del`
5. **IDS block not expired**: Active mode blocks last 10 seconds; wait or restart router
6. **Dashboard shows "Disconnected"**: Java DashboardServer (T9) is not running or port 8080/9000 is in use
7. **NestJS dashboard blank**: Ensure T9 is running first; T10 proxies from `http://localhost:8080/events`
