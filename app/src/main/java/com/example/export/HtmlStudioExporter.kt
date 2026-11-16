package com.example.export

import com.example.model.DocumentaryScene

object HtmlStudioExporter {
    private fun quoteJson(value: String): String {
        val sb = StringBuilder(value.length + 4)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '<' -> sb.append("\\u003C")
                '>' -> sb.append("\\u003E")
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun buildStandaloneHtml(
        projectTitle: String,
        channelName: String,
        scenes: List<DocumentaryScene>
    ): String {
        val scenesJson = scenes.joinToString(prefix = "[", postfix = "]") { s ->
            val accent = String.format("#%06X", 0xFFFFFF and s.palette.accentHex.toInt())
            val bgTop = String.format("#%06X", 0xFFFFFF and s.palette.bgTopHex.toInt())
            val bgBottom = String.format("#%06X", 0xFFFFFF and s.palette.bgBottomHex.toInt())
            """{"badge":${quoteJson(s.badge)},"headline":${quoteJson(s.headline)},"subtext":${quoteJson(s.subtext)},"ticker":${quoteJson(s.tickerText)},"reporter":${quoteJson(s.reporterTag)},"duration":${s.durationSec},"accent":${quoteJson(accent)},"bgTop":${quoteJson(bgTop)},"bgBottom":${quoteJson(bgBottom)}}"""
        }

        val escapedTitle = projectTitle.replace("<", "&lt;").replace(">", "&gt;")
        val escapedChannel = channelName.replace("<", "&lt;").replace(">", "&gt;")

        return """<!DOCTYPE html>
<html lang="bn">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<title>$escapedTitle — NewsCut HTML5 Studio</title>
<style>
  :root { --bg:#070a12; --card:#121826; --border:#263248; --red:#e53935; --amber:#f59e0b; --text:#f1f5f9; --muted:#94a3b8; }
  * { box-sizing:border-box; margin:0; padding:0; font-family:system-ui,-apple-system,"Noto Sans Bengali",sans-serif; }
  body { background:var(--bg); color:var(--text); padding:14px; display:flex; flex-direction:column; gap:12px; max-width:520px; margin:0 auto; }
  .header { display:flex; justify-content:space-between; align-items:center; background:var(--card); border:1px solid var(--border); padding:10px 14px; border-radius:14px; }
  .header h1 { font-size:15px; font-weight:700; }
  .pill { background:var(--red); color:#fff; border:none; padding:8px 12px; border-radius:10px; font-weight:700; font-size:12px; cursor:pointer; }
  .stage { width:100%; aspect-ratio:9/16; background:#000; border-radius:16px; overflow:hidden; border:1px solid var(--border); position:relative; }
  canvas { width:100%; height:100%; display:block; }
  .controls { background:var(--card); border:1px solid var(--border); border-radius:14px; padding:12px; display:flex; flex-direction:column; gap:10px; }
  .row { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
  button, select, input, textarea { background:#1a2234; color:var(--text); border:1px solid var(--border); border-radius:10px; padding:8px 10px; font-size:13px; }
  button { cursor:pointer; font-weight:600; }
  input[type=range] { flex:1; padding:0; accent-color:var(--amber); }
  .scene-list { display:flex; gap:8px; overflow-x:auto; padding-bottom:4px; }
  .scene-chip { min-width:130px; padding:8px; border-radius:10px; background:#1a2234; border:1px solid var(--border); cursor:pointer; font-size:12px; }
  .scene-chip.active { border-color:var(--amber); background:#272116; }
  textarea { width:100%; min-height:70px; }
</style>
</head>
<body>
  <div class="header">
    <h1>$escapedTitle</h1>
    <div class="row">
      <select id="resSelect">
        <option value="240x426">240p</option>
        <option value="360x640">360p</option>
        <option value="480x854">480p</option>
        <option value="720x1280" selected>720p</option>
        <option value="1080x1920">1080p</option>
      </select>
      <button class="pill" id="exportBtn">Export WebM</button>
    </div>
  </div>
  <div class="stage"><canvas id="previewCanvas" width="720" height="1280"></canvas></div>
  <div class="controls">
    <div class="row">
      <button id="playBtn">Play / Pause</button>
      <input type="range" id="scrubber" min="0" max="100" step="0.05" value="0" />
      <span id="timeLabel" style="font-family:monospace;font-size:12px;">00:00</span>
    </div>
    <div class="scene-list" id="sceneList"></div>
    <input id="editHeadline" placeholder="Edit headline..." />
    <textarea id="editSubtext" placeholder="Edit subtext..."></textarea>
  </div>
<script>
const CHANNEL = ${quoteJson(escapedChannel)};
let scenes = $scenesJson;
let currentTime = 0, playing = true, selectedIdx = 0, lastTs = performance.now();
const canvas = document.getElementById('previewCanvas');
const ctx = canvas.getContext('2d');
const scrubber = document.getElementById('scrubber');
const timeLabel = document.getElementById('timeLabel');
const sceneList = document.getElementById('sceneList');
const editHeadline = document.getElementById('editHeadline');
const editSubtext = document.getElementById('editSubtext');

function totalDuration() { return scenes.reduce((a,s) => a + (Number(s.duration)||5), 0); }
function resolveScene(t) {
  let acc = 0;
  for (let i=0;i<scenes.length;i++) {
    const d = Number(scenes[i].duration)||5;
    if (t <= acc + d || i === scenes.length - 1) return { idx:i, scene:scenes[i], p:Math.min(1, Math.max(0,(t-acc)/d)) };
    acc += d;
  }
  return { idx:0, scene:scenes[0], p:0 };
}
function wrapText(text, maxW) {
  const words = (text||'').split(' '), lines = [];
  let line = '';
  for (const w of words) {
    const test = line ? line + ' ' + w : w;
    if (ctx.measureText(test).width > maxW && line) { lines.push(line); line = w; } else line = test;
  }
  if (line) lines.push(line);
  return lines;
}
function render(targetCtx, w, h, t) {
  const { idx, scene, p } = resolveScene(t);
  const u = h / 1920;
  const g = targetCtx.createLinearGradient(0,0,0,h);
  g.addColorStop(0, scene.bgTop || '#28080D');
  g.addColorStop(1, scene.bgBottom || '#070A12');
  targetCtx.fillStyle = g;
  targetCtx.fillRect(0,0,w,h);
  targetCtx.fillStyle = 'rgba(10,15,27,0.86)';
  targetCtx.fillRect(54*u, 74*u, 220*u, 54*u);
  targetCtx.fillStyle = '#ff3b30';
  targetCtx.beginPath(); targetCtx.arc(84*u, 101*u, 10*u, 0, Math.PI*2); targetCtx.fill();
  targetCtx.fillStyle = '#fff'; targetCtx.font = 'bold '+(26*u)+'px sans-serif';
  targetCtx.fillText('LIVE • REC', 108*u, 110*u);
  targetCtx.fillStyle = 'rgba(203,213,225,0.85)'; targetCtx.font = 'bold '+(22*u)+'px sans-serif';
  targetCtx.fillText(CHANNEL, 58*u, 165*u);

  const cardX = 56*u, cardW = w - 112*u, cardY = h*0.42;
  targetCtx.fillStyle = 'rgba(10,15,27,0.86)';
  targetCtx.fillRect(cardX, cardY, cardW, h*0.42);
  targetCtx.fillStyle = scene.accent || '#e53935';
  targetCtx.fillRect(cardX, cardY, 10*u, h*0.42);
  targetCtx.fillRect(cardX+36*u, cardY+32*u, 280*u, 44*u);
  targetCtx.fillStyle = '#fff'; targetCtx.font = 'bold '+(24*u)+'px sans-serif';
  targetCtx.fillText(scene.badge || '', cardX+50*u, cardY+62*u);

  targetCtx.font = 'bold '+(50*u)+'px sans-serif';
  let ty = cardY + 130*u;
  for (const ln of wrapText(scene.headline, cardW - 80*u)) { targetCtx.fillText(ln, cardX+36*u, ty); ty += 62*u; }
  targetCtx.fillStyle = '#e2e8f0'; targetCtx.font = (32*u)+'px sans-serif';
  ty += 16*u;
  for (const ln of wrapText(scene.subtext, cardW - 80*u)) { targetCtx.fillText(ln, cardX+36*u, ty); ty += 44*u; }

  targetCtx.fillStyle = '#d32f2f'; targetCtx.fillRect(0, h-160*u, w, 80*u);
  targetCtx.fillStyle = '#fff'; targetCtx.font = (30*u)+'px sans-serif';
  const offset = (t * 140 * u) % (w + 900*u);
  targetCtx.fillText(scene.ticker || '', w - offset, h - 108*u);
}
function syncUi() {
  sceneList.innerHTML = '';
  scenes.forEach((s, i) => {
    const el = document.createElement('div');
    el.className = 'scene-chip' + (i===selectedIdx?' active':'');
    el.textContent = (i+1)+'. '+s.headline.slice(0,28);
    el.onclick = () => { selectedIdx = i; editHeadline.value = s.headline; editSubtext.value = s.subtext; syncUi(); };
    sceneList.appendChild(el);
  });
  if (scenes[selectedIdx]) { editHeadline.value = scenes[selectedIdx].headline; editSubtext.value = scenes[selectedIdx].subtext; }
  scrubber.max = totalDuration();
}
editHeadline.oninput = () => { if (scenes[selectedIdx]) { scenes[selectedIdx].headline = editHeadline.value; syncUi(); } };
editSubtext.oninput = () => { if (scenes[selectedIdx]) { scenes[selectedIdx].subtext = editSubtext.value; } };
document.getElementById('playBtn').onclick = () => { playing = !playing; };
scrubber.oninput = () => { playing = false; currentTime = Number(scrubber.value); };
document.getElementById('exportBtn').onclick = async () => {
  const [ew, eh] = document.getElementById('resSelect').value.split('x').map(Number);
  canvas.width = ew; canvas.height = eh;
  const stream = canvas.captureStream(24);
  const rec = new MediaRecorder(stream, { mimeType: 'video/webm' });
  const chunks = [];
  rec.ondataavailable = e => { if (e.data.size) chunks.push(e.data); };
  rec.onstop = () => {
    const blob = new Blob(chunks, { type:'video/webm' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'newscut_studio.webm'; a.click();
    canvas.width = 720; canvas.height = 1280;
  };
  playing = false; currentTime = 0; rec.start();
  const total = totalDuration();
  const step = 1/24;
  for (let t=0; t<=total; t+=step) {
    render(ctx, ew, eh, t);
    await new Promise(r => setTimeout(r, 40));
  }
  rec.stop();
};
function loop(now) {
  const dt = (now - lastTs)/1000; lastTs = now;
  const total = totalDuration();
  if (playing && total > 0) { currentTime = (currentTime + dt) % total; scrubber.value = currentTime; }
  timeLabel.textContent = currentTime.toFixed(1)+'s';
  render(ctx, canvas.width, canvas.height, currentTime);
  requestAnimationFrame(loop);
}
syncUi();
requestAnimationFrame(loop);
</script>
</body>
</html>"""
    }
}
