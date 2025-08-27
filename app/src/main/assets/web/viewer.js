(async function(){
  const btn = document.getElementById('connect');
  const pin = document.getElementById('pin');
  const video = document.getElementById('v');
  const statsEl = document.getElementById('stats');

  let pc, ws;

  function log(s){ console.log(s); statsEl.textContent = s; }

  function connectWS() {
    const loc = window.location;
    const host = loc.host;
    ws = new WebSocket(`ws://${host}/signal?pin=${encodeURIComponent(pin.value)}`);
    ws.onopen = async () => {
      log("WS connected, awaiting offer...");
    };
    ws.onmessage = async (e) => {
      const msg = JSON.parse(e.data);
      if (msg.type === 'offer') {
        await handleOffer(msg.sdp);
      } else if (msg.type === 'ice') {
        try { await pc.addIceCandidate(msg.candidate); } catch(e){ console.warn(e) }
      }
    };
    ws.onclose = () => log("WS closed");
  }

  async function handleOffer(sdp) {
    pc = new RTCPeerConnection({ iceServers: [] /* LAN only */ });

    pc.onicecandidate = e => {
      if (e.candidate) ws.send(JSON.stringify({type:'ice', candidate:e.candidate}));
    };
    pc.ontrack = e => { video.srcObject = e.streams[0]; };

    await pc.setRemoteDescription({ type: 'offer', sdp });
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);
    ws.send(JSON.stringify({ type:'answer', sdp: answer.sdp }));

    setInterval(async () => {
      const stats = await pc.getStats();
      stats.forEach(report => {
        if (report.type === 'inbound-rtp' && report.kind === 'video') {
          log(`fps:${report.framesPerSecond||'-'} bitrate(kbps):${((report.bytesReceived||0)*8/1000).toFixed(0)} packetsLost:${report.packetsLost||0}`);
        }
      });
    }, 1000);
  }

  btn.onclick = connectWS;
})();
