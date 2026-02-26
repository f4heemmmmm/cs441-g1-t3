# Demo Runbook

This runbook walks through a complete demonstration of the network emulator. Each scenario is in its own file for easy reference.

## Terminal Reference

| Terminal | Process | Command |
|----------|---------|---------|
| **T1** | LAN1 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan1Emu` |
| **T2** | LAN2 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan2Emu` |
| **T3** | LAN3 Emulator | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.lan.Lan3Emu` |
| **T4** | Router | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.RouterR` |
| **T5** | Node1 (Attacker) | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node1` |
| **T6** | Node2 | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node2` |
| **T7** | Node3 (Firewall) | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node3` |
| **T8** | Node4 | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.device.Node4` |
| **T9** | Dashboard (Java) | `java -cp target/netemu-1.0-SNAPSHOT.jar netemu.dashboard.DashboardServer` |
| **T10** | Dashboard (NestJS) | `cd dashboard && npm run start:dev` |

T9 and T10 are optional. T9 collects events from all processes via UDP (port 9000) and serves SSE on port 8080. T10 is an enhanced frontend (port 3000) that proxies T9's SSE stream and adds interactive topology, scenario guide, and event filtering.

## Demo Scenarios

Follow these in order:

1. **[Setup](demo/00_SETUP.md)** - Build, start all 8 processes, verify registration
2. **[Part A: Ethernet Proof](demo/01_A_ETHERNET_PROOF.md)** - Broadcast + MAC filtering demo
3. **[Part B: Cross-LAN Ping](demo/02_B_CROSS_LAN_PING.md)** - IP routing via the router
4. **[Part C: Spoofing + IDS](demo/03_C_SPOOFING_IDS.md)** - IP spoofing and IDS detection
5. **[Part D: Sniffing](demo/04_D_SNIFFING.md)** - Promiscuous mode sniffing on LAN1
6. **[Part E: Firewall](demo/05_E_FIREWALL.md)** - Node3 firewall blocking by source IP
7. **[Part F: Ping Flood](demo/06_F_PING_FLOOD.md)** - IDS ping flood detection and auto-block

## Shutdown

In each terminal (T1-T9), type `quit` to cleanly shut down the process. For T10 (NestJS dashboard), press `Ctrl+C`.
