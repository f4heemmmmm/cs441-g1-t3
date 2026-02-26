(function () {
  'use strict';

  var MAX_EVENTS = 500;
  var HEARTBEAT_TIMEOUT = 10000;

  var stats = { frames: 0, pings: 0, forwards: 0, ids: 0, firewall: 0, drops: 0 };
  var lastSeen = {};
  var processNames = ['LAN1', 'LAN2', 'LAN3', 'ROUTER', 'N1', 'N2', 'N3', 'N4'];
  var allEvents = [];
  var currentFilter = 'all';
  var eventCount = 0;

  var filterMap = {
    all: null,
    frames: ['FRAME_SENT', 'FRAME_RECV', 'FRAME_DROP'],
    pings: ['PING_SENT', 'PING_RECV'],
    security: ['IDS_ALERT', 'IDS_BLOCK', 'FIREWALL', 'SNIFF', 'SPOOF'],
    system: ['REGISTER', 'STARTUP', 'INFO', 'FORWARD', 'CONNECTED'],
  };

  var processColors = {
    N1: 'text-red-400', N2: 'text-green-400', N3: 'text-yellow-400', N4: 'text-purple-400',
    ROUTER: 'text-blue-400', LAN1: 'text-gray-500', LAN2: 'text-gray-500', LAN3: 'text-gray-500',
  };

  var typeStyles = {
    FRAME_SENT: 'background:#1f2937;color:#8b949e',
    FRAME_RECV: 'background:#1f2937;color:#8b949e',
    FRAME_DROP: 'background:#3d1f20;color:#f85149',
    PING_SENT: 'background:#0c2d1a;color:#3fb950',
    PING_RECV: 'background:#0c2d1a;color:#3fb950',
    FORWARD: 'background:#1a2d3d;color:#79c0ff',
    IDS_ALERT: 'background:#5a1e1e;color:#ff7b72',
    IDS_BLOCK: 'background:#5a1e1e;color:#ff7b72',
    FIREWALL: 'background:#3d2e00;color:#d29922',
    SNIFF: 'background:#0d2440;color:#58a6ff',
    SPOOF: 'background:#2d1a3d;color:#bc8cff',
    REGISTER: 'background:#0c2d1a;color:#56d364',
    STARTUP: 'background:#1f2937;color:#8b949e',
    INFO: 'background:#1f2937;color:#8b949e',
    CONNECTED: 'background:#0c2d1a;color:#3fb950',
  };

  var feedEl = document.getElementById('eventFeed');
  var connBadge = document.getElementById('connBadge');
  var emptyState = document.getElementById('emptyState');
  var eventCountEl = document.getElementById('eventCount');

  // --- SSE Connection ---
  function connect() {
    var es = new EventSource('/api/events');

    es.onopen = function () {
      connBadge.textContent = 'Connected';
      connBadge.className = 'px-3 py-1 rounded-full text-xs font-bold bg-green-600 text-white';
    };

    es.onmessage = function (e) {
      try {
        var evt = JSON.parse(e.data);
        if (evt.type === 'CONNECTED') return;
        handleEvent(evt);
      } catch (err) { /* ignore parse errors */ }
    };

    es.onerror = function () {
      connBadge.textContent = 'Disconnected';
      connBadge.className = 'px-3 py-1 rounded-full text-xs font-bold bg-red-600 text-white';
      es.close();
      setTimeout(connect, 3000);
    };
  }

  function handleEvent(evt) {
    if (evt.process) {
      lastSeen[evt.process] = Date.now();
      updateProcessStatus();
    }
    updateStats(evt);
    if (typeof animateTopology === 'function') {
      animateTopology(evt);
    }
    storeAndRender(evt);
  }

  // --- Stats ---
  function updateStats(evt) {
    switch (evt.type) {
      case 'FRAME_SENT': case 'FRAME_RECV':
        stats.frames++;
        document.getElementById('stat-frames').textContent = stats.frames;
        break;
      case 'PING_SENT': case 'PING_RECV':
        stats.pings++;
        document.getElementById('stat-pings').textContent = stats.pings;
        break;
      case 'FORWARD':
        stats.forwards++;
        document.getElementById('stat-forwards').textContent = stats.forwards;
        break;
      case 'IDS_ALERT': case 'IDS_BLOCK':
        stats.ids++;
        document.getElementById('stat-ids').textContent = stats.ids;
        break;
      case 'FIREWALL':
        stats.firewall++;
        document.getElementById('stat-firewall').textContent = stats.firewall;
        break;
      case 'FRAME_DROP':
        stats.drops++;
        document.getElementById('stat-drops').textContent = stats.drops;
        break;
    }
  }

  // --- Process Status ---
  function updateProcessStatus() {
    var now = Date.now();
    processNames.forEach(function (name) {
      var online = lastSeen[name] && (now - lastSeen[name]) < HEARTBEAT_TIMEOUT;

      var dot = document.getElementById('dot-' + name);
      if (dot) {
        dot.className = online
          ? 'w-2 h-2 rounded-full bg-green-400'
          : 'w-2 h-2 rounded-full bg-gray-600';
      }

      var svgNode = document.getElementById('svg-' + name);
      if (svgNode) {
        if (online) {
          svgNode.setAttribute('class', svgNode.getAttribute('class')
            .replace(/fill-gray-600/g, 'fill-green-600')
            .replace(/stroke-gray-500/g, 'stroke-green-400'));
        } else {
          svgNode.setAttribute('class', svgNode.getAttribute('class')
            .replace(/fill-green-600/g, 'fill-gray-600')
            .replace(/stroke-green-400/g, 'stroke-gray-500'));
        }
      }
    });
  }

  // --- Event Feed ---
  function storeAndRender(evt) {
    allEvents.push(evt);
    if (allEvents.length > MAX_EVENTS) {
      allEvents.shift();
    }
    eventCount++;
    eventCountEl.textContent = eventCount + ' events';

    if (emptyState) {
      emptyState.style.display = 'none';
    }

    if (matchesFilter(evt, currentFilter)) {
      appendEventLine(evt);
    }
  }

  function matchesFilter(evt, filter) {
    if (filter === 'all') return true;
    var types = filterMap[filter];
    return types && types.indexOf(evt.type) !== -1;
  }

  function appendEventLine(evt) {
    var line = document.createElement('div');
    line.className = 'px-3 py-0.5 text-[11px] hover:bg-gray-800/50 whitespace-nowrap overflow-hidden text-ellipsis';

    var ts = evt.ts
      ? new Date(evt.ts).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })
      : '';

    var pClass = processColors[evt.process] || 'text-gray-400';
    var tStyle = typeStyles[evt.type] || 'background:#1f2937;color:#8b949e';

    line.innerHTML =
      '<span class="text-gray-600 mr-1.5">' + escapeHtml(ts) + '</span>' +
      '<span class="font-bold mr-1.5 ' + pClass + '">' + escapeHtml(evt.process || '?') + '</span>' +
      '<span class="px-1.5 py-px rounded text-[9px] font-bold mr-1.5" style="' + tStyle + '">' + escapeHtml(evt.type || '?') + '</span>' +
      '<span class="text-gray-400">' + escapeHtml(evt.msg || '') + '</span>';

    feedEl.appendChild(line);

    while (feedEl.children.length > MAX_EVENTS + 1) {
      feedEl.removeChild(feedEl.firstChild);
    }

    feedEl.scrollTop = feedEl.scrollHeight;
  }

  function rebuildFeed() {
    // Remove all event lines but keep empty state
    var children = feedEl.querySelectorAll('div:not(#emptyState)');
    children.forEach(function (c) { if (c.id !== 'emptyState') feedEl.removeChild(c); });

    allEvents.forEach(function (evt) {
      if (matchesFilter(evt, currentFilter)) {
        appendEventLine(evt);
      }
    });
  }

  function escapeHtml(s) {
    var d = document.createElement('div');
    d.appendChild(document.createTextNode(s));
    return d.innerHTML;
  }

  // --- Filter Buttons ---
  document.querySelectorAll('.filter-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      currentFilter = btn.getAttribute('data-filter');

      document.querySelectorAll('.filter-btn').forEach(function (b) {
        b.className = 'filter-btn px-2 py-0.5 rounded text-[10px] font-bold text-muted border border-border hover:bg-gray-800';
      });
      btn.className = 'filter-btn px-2 py-0.5 rounded text-[10px] font-bold bg-accent/20 text-accent border border-accent';

      rebuildFeed();
    });
  });

  // --- Scenario Guide Toggle ---
  var scenarioGuide = document.getElementById('scenarioGuide');
  var scenarioToggle = document.getElementById('scenarioToggle');
  scenarioToggle.addEventListener('click', function () {
    scenarioGuide.classList.toggle('hidden');
    scenarioToggle.textContent = scenarioGuide.classList.contains('hidden') ? 'Scenario Guide' : 'Hide Guide';
  });

  // Scenario tabs
  document.querySelectorAll('.scenario-tab').forEach(function (tab) {
    tab.addEventListener('click', function () {
      var id = tab.getAttribute('data-scenario');

      document.querySelectorAll('.scenario-tab').forEach(function (t) {
        t.className = 'scenario-tab px-3 py-1 rounded text-xs font-bold border border-border hover:bg-gray-800 transition-colors text-muted';
      });
      tab.className = 'scenario-tab px-3 py-1 rounded text-xs font-bold border transition-colors bg-accent/20 text-accent border-accent';

      document.querySelectorAll('.scenario-content').forEach(function (c) {
        c.classList.remove('active');
      });
      var content = document.querySelector('.scenario-content[data-scenario="' + id + '"]');
      if (content) content.classList.add('active');
    });
  });

  // --- Periodic status check ---
  setInterval(updateProcessStatus, 3000);

  // --- Start ---
  connect();
})();
