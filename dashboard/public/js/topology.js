(function () {
  'use strict';

  // Node metadata for tooltips
  var nodeData = {
    N1: { name: 'Node1', mac: 'N1', ip: '0x12', port: '6001', lan: 'LAN1', role: 'Attacker', roleColor: 'color:#f85149', desc: 'IP spoofing and promiscuous sniffing capabilities' },
    N2: { name: 'Node2', mac: 'N2', ip: '0x13', port: '6002', lan: 'LAN1', role: 'Normal', roleColor: 'color:#3fb950', desc: 'Standard network endpoint on LAN1' },
    N3: { name: 'Node3', mac: 'N3', ip: '0x22', port: '6003', lan: 'LAN2', role: 'Firewall', roleColor: 'color:#d29922', desc: 'Stateless firewall with dynamic block rules' },
    N4: { name: 'Node4', mac: 'N4', ip: '0x32', port: '6004', lan: 'LAN3', role: 'Normal', roleColor: 'color:#bc8cff', desc: 'Standard network endpoint on LAN3' },
    R1: { name: 'Router (R1)', mac: 'R1', ip: '0x11', port: '6011', lan: 'LAN1', role: 'Router Interface', roleColor: 'color:#58a6ff', desc: 'Gateway for LAN1 traffic' },
    R2: { name: 'Router (R2)', mac: 'R2', ip: '0x21', port: '6012', lan: 'LAN2', role: 'Router Interface', roleColor: 'color:#58a6ff', desc: 'Gateway for LAN2 traffic' },
    R3: { name: 'Router (R3)', mac: 'R3', ip: '0x31', port: '6013', lan: 'LAN3', role: 'Router Interface', roleColor: 'color:#58a6ff', desc: 'Gateway for LAN3 traffic' },
    ROUTER: { name: 'Router', mac: 'R1/R2/R3', ip: '0x11/0x21/0x31', port: '6011-6013', lan: 'All', role: 'IDS Engine', roleColor: 'color:#58a6ff', desc: 'Spoof detection + Ping flood mitigation. Forwards packets between all 3 LANs.' },
  };

  var tooltip = document.getElementById('nodeTooltip');
  var container = document.getElementById('topologyContainer');

  // Tooltip on node click
  document.querySelectorAll('[data-node]').forEach(function (el) {
    el.style.cursor = 'pointer';
    el.addEventListener('click', function (e) {
      e.stopPropagation();
      var id = el.getAttribute('data-node');
      var data = nodeData[id];
      if (!data) return;

      document.getElementById('ttName').textContent = data.name;
      document.getElementById('ttMAC').textContent = data.mac;
      document.getElementById('ttIP').textContent = data.ip;
      document.getElementById('ttPort').textContent = data.port;
      document.getElementById('ttLAN').textContent = data.lan;
      var roleEl = document.getElementById('ttRole');
      roleEl.textContent = data.role;
      roleEl.style.cssText = data.roleColor;
      document.getElementById('ttDesc').textContent = data.desc;

      // Position tooltip near click
      var rect = container.getBoundingClientRect();
      var x = e.clientX - rect.left + 10;
      var y = e.clientY - rect.top + 10;

      // Keep within container
      if (x + 224 > rect.width) x = x - 234;
      if (y + 180 > rect.height) y = y - 190;

      tooltip.style.left = x + 'px';
      tooltip.style.top = y + 'px';
      tooltip.classList.add('active');
    });
  });

  // Close tooltip on click outside
  document.addEventListener('click', function () {
    tooltip.classList.remove('active');
  });

  // Link animation
  function animateLink(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('link-animate');
    void el.offsetWidth; // force reflow
    el.classList.add('link-animate');
    setTimeout(function () { el.classList.remove('link-animate'); }, 600);
  }

  // Topology animation based on events (called from dashboard.js)
  window.animateTopology = function (evt) {
    var p = evt.process;
    var linkId = null;

    if (evt.type === 'FORWARD') {
      animateLink('link-lan1-router');
      animateLink('link-lan2-router');
      animateLink('link-lan3-router');
      return;
    }

    if (['FRAME_SENT', 'FRAME_RECV', 'PING_SENT', 'PING_RECV'].indexOf(evt.type) !== -1) {
      if (p === 'N1' || p === 'N2' || p === 'LAN1') {
        linkId = 'link-lan1-router';
      } else if (p === 'N3' || p === 'LAN2') {
        linkId = 'link-lan2-router';
      } else if (p === 'N4' || p === 'LAN3') {
        linkId = 'link-lan3-router';
      } else if (p === 'ROUTER') {
        var msg = evt.msg || '';
        if (msg.indexOf('R1') !== -1 || msg.indexOf('LAN1') !== -1) linkId = 'link-lan1-router';
        else if (msg.indexOf('R2') !== -1 || msg.indexOf('LAN2') !== -1) linkId = 'link-lan2-router';
        else if (msg.indexOf('R3') !== -1 || msg.indexOf('LAN3') !== -1) linkId = 'link-lan3-router';
      }
    }

    if (linkId) animateLink(linkId);
  };
})();
