import { Controller, Get, Render } from '@nestjs/common';

@Controller()
export class AppController {
  @Get()
  @Render('index')
  root() {
    return {
      title: 'CS441 Network Emulator Dashboard',
      nodes: [
        { id: 'N1', mac: 'N1', ip: '0x12', port: 6001, lan: 'LAN1', role: 'Attacker', color: '#f85149', desc: 'Spoofing + Sniffing capabilities' },
        { id: 'N2', mac: 'N2', ip: '0x13', port: 6002, lan: 'LAN1', role: 'Normal', color: '#3fb950', desc: 'Standard network node' },
        { id: 'N3', mac: 'N3', ip: '0x22', port: 6003, lan: 'LAN2', role: 'Firewall', color: '#d29922', desc: 'Stateless IP firewall rules' },
        { id: 'N4', mac: 'N4', ip: '0x32', port: 6004, lan: 'LAN3', role: 'Normal', color: '#bc8cff', desc: 'Standard network node' },
      ],
      router: {
        interfaces: [
          { id: 'R1', mac: 'R1', ip: '0x11', port: 6011, lan: 'LAN1' },
          { id: 'R2', mac: 'R2', ip: '0x21', port: 6012, lan: 'LAN2' },
          { id: 'R3', mac: 'R3', ip: '0x31', port: 6013, lan: 'LAN3' },
        ],
        role: 'IDS Engine',
        desc: 'Spoof detection + Ping flood mitigation',
      },
      lans: [
        { id: 'LAN1', port: 5001 },
        { id: 'LAN2', port: 5002 },
        { id: 'LAN3', port: 5003 },
      ],
      scenarios: [
        {
          id: 'A', title: 'Ethernet Proof',
          goal: 'Prove broadcast + MAC filtering. LAN emulator broadcasts to all endpoints; only the addressed node accepts.',
          terminal: 'T4 (Router)',
          command: 'ethsend lan1 dst N1 msg "hello"',
          watch: 'Node1 accepts (MAC match), Node2 drops (MAC mismatch). Both receive the frame via broadcast.',
        },
        {
          id: 'B', title: 'Cross-LAN Ping',
          goal: 'IP routing across LANs. Node1 (LAN1) pings Node3 (LAN2) via Router.',
          terminal: 'T5 (Node1)',
          command: 'ping 0x22 count 1',
          watch: 'PING_SENT on N1, FORWARD on Router (R1->R2), PING_RECV on N3, then reply traces back.',
        },
        {
          id: 'C', title: 'Spoofing + IDS',
          goal: 'Node1 spoofs IP (claims 0x13). Router IDS detects MAC/IP mismatch.',
          terminal: 'T4: ids on / ids mode passive | T5: spoof on 0x13 | T5: ping 0x22 count 1',
          command: 'spoof on 0x13',
          watch: 'IDS_ALERT on Router. In active mode, IDS_BLOCK drops the packet.',
        },
        {
          id: 'D', title: 'Sniffing',
          goal: 'Node1 captures all LAN1 traffic in promiscuous mode.',
          terminal: 'T5 (Node1)',
          command: 'sniff on',
          watch: 'SNIFF events on N1 for every frame on LAN1, even those addressed to other nodes.',
        },
        {
          id: 'E', title: 'Firewall',
          goal: 'Node3 blocks packets from Node2 (IP=0x13) with a firewall rule.',
          terminal: 'T7 (Node3)',
          command: 'fw add block src 0x13',
          watch: 'FIREWALL event on N3. Node2\'s pings are silently dropped; no reply sent.',
        },
        {
          id: 'F', title: 'Ping Flood',
          goal: 'IDS detects ping flood (>10 pings/5s) and auto-blocks the source.',
          terminal: 'T4: ids on / ids mode active | T5: pingflood 0x22 rate 20 duration 5',
          command: 'pingflood 0x22 rate 20 duration 5',
          watch: 'IDS_ALERT after 11th ping, then IDS_BLOCK. All subsequent pings dropped for 10s.',
        },
      ],
      eventTypes: [
        { type: 'FRAME_SENT', color: '#8b949e', bg: '#1f2937', desc: 'Ethernet frame transmitted onto LAN' },
        { type: 'FRAME_RECV', color: '#8b949e', bg: '#1f2937', desc: 'Ethernet frame received from LAN' },
        { type: 'FRAME_DROP', color: '#f85149', bg: '#3d1f20', desc: 'Frame dropped (MAC mismatch)' },
        { type: 'PING_SENT', color: '#3fb950', bg: '#0c2d1a', desc: 'ICMP ping request sent' },
        { type: 'PING_RECV', color: '#3fb950', bg: '#0c2d1a', desc: 'ICMP ping request/reply received' },
        { type: 'FORWARD', color: '#79c0ff', bg: '#1a2d3d', desc: 'Router forwarded packet between interfaces' },
        { type: 'IDS_ALERT', color: '#ff7b72', bg: '#5a1e1e', desc: 'IDS detected suspicious activity' },
        { type: 'IDS_BLOCK', color: '#ff7b72', bg: '#5a1e1e', desc: 'IDS blocked traffic from source' },
        { type: 'FIREWALL', color: '#d29922', bg: '#3d2e00', desc: 'Firewall rule blocked a packet' },
        { type: 'SNIFF', color: '#58a6ff', bg: '#0d2440', desc: 'Promiscuous mode captured a frame' },
        { type: 'SPOOF', color: '#bc8cff', bg: '#2d1a3d', desc: 'Spoofed packet sent with fake IP' },
        { type: 'REGISTER', color: '#56d364', bg: '#0c2d1a', desc: 'Node registered with LAN emulator' },
        { type: 'STARTUP', color: '#8b949e', bg: '#1f2937', desc: 'Process started up' },
        { type: 'INFO', color: '#8b949e', bg: '#1f2937', desc: 'General informational message' },
        { type: 'CONNECTED', color: '#3fb950', bg: '#0c2d1a', desc: 'Dashboard SSE connection established' },
      ],
    };
  }
}
