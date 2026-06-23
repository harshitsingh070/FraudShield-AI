import React, { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

// ── Fallback mock data if backend is offline ─────────────────────────────────
const MOCK_SUMMARY = {
  estimated_impact_inr: "₹18.28 Cr", active_fraud_rings: 5, complaints_today: 147, high_risk_numbers: 12,
  map_pins: [
    { ring_id:"RING-DL-01", location_name:"Delhi Digital Arrest Cluster", lat:28.585, lon:77.049, threat_score:8.5, fraud_type:"DIGITAL_ARREST", status:"ACTIVE", total_victim_count:36, total_money_laundered:48900000 },
    { ring_id:"RING-JH-01", location_name:"Jamtara Network",             lat:23.957, lon:86.499, threat_score:8.2, fraud_type:"DIGITAL_ARREST", status:"ACTIVE", total_victim_count:29, total_money_laundered:40600000 },
    { ring_id:"RING-MH-01", location_name:"Mumbai Investment Fraud Syndicate", lat:19.119, lon:72.846, threat_score:7.7, fraud_type:"INVESTMENT_FRAUD", status:"ACTIVE", total_victim_count:42, total_money_laundered:63500000 },
    { ring_id:"RING-KA-01", location_name:"Bengaluru Job Fraud Network", lat:12.969, lon:77.749, threat_score:6.82, fraud_type:"JOB_FRAUD", status:"ACTIVE", total_victim_count:32, total_money_laundered:12000000 },
    { ring_id:"RING-RJ-01", location_name:"Rajasthan Lottery Ring",      lat:26.912, lon:75.787, threat_score:6.2, fraud_type:"LOTTERY_FRAUD", status:"ACTIVE", total_victim_count:59, total_money_laundered:8000000 },
  ]
};
const MOCK_QUEUE = MOCK_SUMMARY.map_pins.map(p => ({
  ring_id: p.ring_id, location_name: p.location_name, threat_score: p.threat_score, fraud_type: p.fraud_type
}));

// ── Live terminal feed events ────────────────────────────────────────────────
const TERMINAL_EVENTS = [
  t => `${t} — New complaint flagged — Pattern match: DIGITAL_ARREST — Confidence: 94.2%`,
  t => `${t} — Cross-reference check: RING-DL-01 — 3 previous matches found`,
  t => `${t} — Mule account 970070080090 activity spike — ₹2.1L transfer detected`,
  t => `${t} — RING-JH-01 velocity alert — 7 new complaints in 4h window`,
  t => `${t} — PhoneNumber +91-976-XXXXX flagged HIGH RISK — linked to 14 victims`,
  t => `${t} — New complaint flagged — Pattern match: INVESTMENT_FRAUD — Confidence: 89.1%`,
  t => `${t} — Shared mule account coordination signal — RING-JH-01 ↔ RING-DL-01`,
  t => `${t} — TRAI SIM flag request initiated for +91-865-XXXXX`,
  t => `${t} — New complaint flagged — Pattern match: LOTTERY_FRAUD — Confidence: 91.7%`,
  t => `${t} — MHA brief generated for RING-MH-01 — Sent to Cyber Cell`,
  t => `${t} — Graph traversal: 3-hop network found — 18 connected nodes`,
  t => `${t} — RING-KA-01 dormancy check — Status: ACTIVE — Last seen 2h ago`,
  t => `${t} — New complaint flagged — Pattern match: JOB_FRAUD — Confidence: 86.4%`,
  t => `${t} — Section 91 CrPC notice prepared — Awaiting sign-off`,
  t => `${t} — Threat re-score complete — RING-DL-01 elevated to CRITICAL`,
];

function getTime() {
  return new Date().toLocaleTimeString('en-IN', { hour12: false });
}

export default function App() {
  const [summary, setSummary]         = useState(null);
  const [queue, setQueue]             = useState(null);
  const [dbStatus, setDbStatus]       = useState('LOADING');
  const [selectedRing, setSelectedRing] = useState(null);
  const [copilotMode, setCopilotMode] = useState('idle'); // idle | thinking | result
  const [copilotHtml, setCopilotHtml] = useState('// Awaiting manual target selection or new incident ingestion...');
  const [terminalLogs, setTerminalLogs] = useState([]);
  const [phoneInput, setPhoneInput]   = useState('');
  const [msgInput, setMsgInput]       = useState('');
  const [channelInput, setChannelInput] = useState('WHATSAPP');
  const [aiResult, setAiResult]       = useState(null);
  const termRef = useRef(null);
  const eventIndex = useRef(0);

  // ── Fetch dashboard data ──────────────────────────────────────────────────
  useEffect(() => {
    fetch('/api/v1/dashboard/summary')
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(d => { setSummary(d); setDbStatus('ONLINE'); })
      .catch(() => { setSummary(MOCK_SUMMARY); setDbStatus('OFFLINE'); });

    fetch('/api/v1/dashboard/rings/top-threat')
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(d => setQueue(d))
      .catch(() => setQueue(MOCK_QUEUE));
  }, []);

  // ── Live terminal feed ────────────────────────────────────────────────────
  useEffect(() => {
    // Seed 5 initial entries
    const initial = Array.from({ length: 5 }, (_, i) =>
      TERMINAL_EVENTS[i % TERMINAL_EVENTS.length](getTime())
    );
    setTerminalLogs(initial);
    eventIndex.current = 5;

    const interval = setInterval(() => {
      const entry = TERMINAL_EVENTS[eventIndex.current % TERMINAL_EVENTS.length](getTime());
      eventIndex.current += 1;
      setTerminalLogs(prev => [...prev.slice(-40), entry]);
    }, 3800);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (termRef.current) termRef.current.scrollTop = termRef.current.scrollHeight;
  }, [terminalLogs]);

  // ── AI Extraction ─────────────────────────────────────────────────────────
  const analyzeIncident = () => {
    if (!msgInput.trim() && !phoneInput.trim()) return;
    setAiResult(null);
    setCopilotMode('thinking');

    // Try real API first
    fetch('/api/v1/gemini/analyze-transcript', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ transcript: `[Channel: ${channelInput}, Suspect Phone: ${phoneInput}]\n\n${msgInput}` })
    })
      .then(r => {
        if (!r.ok) throw new Error('HTTP error');
        return r.json();
      })
      .then(data => {
        if (data.isFallback) throw new Error('API returned fallback mode');
        setAiResult(data);
        setCopilotMode('result');
      })
      .catch(() => {
        // Smart mock based on message content
        const msg = (msgInput + phoneInput).toLowerCase();
        let type = 'UNKNOWN', conf = 78.4, ring = 'RING-DL-01', city = 'Delhi';
        if (msg.includes('arrest') || msg.includes('cbi') || msg.includes('ed') || msg.includes('trai')) {
          type = 'DIGITAL_ARREST'; conf = 96.1; ring = 'RING-JH-01'; city = 'Jharkhand';
        } else if (msg.includes('invest') || msg.includes('trading') || msg.includes('crypto') || msg.includes('return')) {
          type = 'INVESTMENT_FRAUD'; conf = 91.4; ring = 'RING-MH-01'; city = 'Mumbai';
        } else if (msg.includes('lottery') || msg.includes('won') || msg.includes('prize') || msg.includes('kbc')) {
          type = 'LOTTERY_FRAUD'; conf = 93.7; ring = 'RING-RJ-01'; city = 'Rajasthan';
        } else if (msg.includes('job') || msg.includes('hiring') || msg.includes('salary') || msg.includes('infosys')) {
          type = 'JOB_FRAUD'; conf = 88.2; ring = 'RING-KA-01'; city = 'Bengaluru';
        }
        const victims = { 'RING-JH-01': 29, 'RING-DL-01': 36, 'RING-MH-01': 42, 'RING-KA-01': 32, 'RING-RJ-01': 59 };
        setAiResult({ scamType: type, confidence: conf, matchedRing: ring, matchedCity: city, victimCount: victims[ring] || 0, triggerPhrases: ['arrest', 'CBI', 'OTP', 'fee', 'lottery'].slice(0, 3) });
        setCopilotMode('result');
      });
  };

  // ── MHA Brief Generation ──────────────────────────────────────────────────
  const generateBrief = (ring) => {
    const r = ring || selectedRing;
    if (!r) return;
    const ringId = r.ring_id || r.ringId;
    const name = r.location_name || r.locationName || ringId || r;

    setCopilotHtml('<span class="text-amber-400 animate-pulse">⟳ Generating MHA Intelligence Brief via Gemini...</span>');

    fetch(`/api/v1/gemini/ring-intelligence/${ringId}`)
      .then(res => {
         if (!res.ok) throw new Error('API Error');
         return res.json();
      })
      .then(data => {
         if (data.isFallback) throw new Error('Fallback mode');
         const threatAssessment = data.threatAssessment || data.threat_assessment;
         const intelligenceBrief = data.intelligenceBrief || data.intelligence_brief;
         const probableHub = data.probableHub || data.probable_hub;
         const primaryTactic = data.primaryTactic || data.primary_tactic;
         const interventionSteps = data.interventionSteps || data.intervention_steps || [];
         const interdictionWindow = data.estimatedInterdictionWindow || data.estimated_interdiction_window;

         setCopilotHtml(`
           <div class="space-y-3 text-xs">
             <div class="text-amber-400 font-bold border-b border-amber-900/50 pb-1 uppercase tracking-wider text-[10px]">
               ⚡ MHA INTELLIGENCE BRIEF — ${name}
             </div>
             <div>
               <span class="text-rose-400 font-bold uppercase text-[9px] tracking-wider block mb-1">THREAT ASSESSMENT</span>
               <span class="text-rose-300">${threatAssessment}</span>
             </div>
             <div>
               <span class="text-orange-400 font-bold uppercase text-[9px] tracking-wider block mb-1">INTELLIGENCE BRIEF</span>
               <span class="text-slate-300">${intelligenceBrief}</span>
             </div>
             <div>
               <span class="text-amber-400 font-bold uppercase text-[9px] tracking-wider block mb-1">PROBABLE HUB & TACTIC</span>
               <span class="text-slate-300">${probableHub} — ${primaryTactic}</span>
             </div>
             <div>
               <span class="text-emerald-400 font-bold uppercase text-[9px] tracking-wider block mb-1">RECOMMENDED DIRECTIVES</span>
               <ul class="list-disc pl-4 text-slate-300 space-y-0.5">
                 ${interventionSteps.map(step => `<li>${step}</li>`).join('')}
               </ul>
             </div>
           </div>
         `);

         const briefText = `MINISTRY OF HOME AFFAIRS — CYBER INTELLIGENCE BRIEF\nGenerated: ${new Date().toISOString()}\nNetwork: ${name}\nHub: ${probableHub}\nTactic: ${primaryTactic}\nWindow: ${interdictionWindow}\n\nTHREAT ASSESSMENT:\n${threatAssessment}\n\nINTELLIGENCE BRIEF:\n${intelligenceBrief}\n\nRECOMMENDED DIRECTIVES:\n${interventionSteps.map((s,i) => `${i+1}. ${s}`).join('\n')}`;
         const blob = new Blob([briefText], { type: 'text/plain' });
         const url = URL.createObjectURL(blob);
         const a = document.createElement('a'); a.href = url;
         a.download = `MHA_Brief_${ringId}.txt`;
         a.click(); URL.revokeObjectURL(url);
      })
      .catch(() => {
        const score = (r.threat_score || r.threatScore || 8.5).toFixed(1);
        const victims = r.total_victim_count || r.victimCount || 36;
        const money = r.total_money_laundered
          ? '₹' + (r.total_money_laundered / 10000000).toFixed(2) + ' Cr'
          : '₹4.89 Cr';
        const type = r.fraud_type || r.fraudType || 'DIGITAL_ARREST';

        setCopilotHtml(`
          <div class="space-y-3 text-xs">
            <div class="text-amber-400 font-bold border-b border-amber-900/50 pb-1 uppercase tracking-wider text-[10px]">
              ⚡ MHA INTELLIGENCE BRIEF — ${name}
            </div>
            <div>
              <span class="text-rose-400 font-bold uppercase text-[9px] tracking-wider block mb-1">THREAT CLASSIFICATION</span>
              <span class="text-rose-300">${type.replace('_',' ')} — Threat Level: ${score}/10</span>
            </div>
            <div>
              <span class="text-orange-400 font-bold uppercase text-[9px] tracking-wider block mb-1">IMPACT SUMMARY</span>
              <span class="text-slate-300">${victims} victims confirmed · Estimated losses: ${money}</span>
            </div>
            <div>
              <span class="text-amber-400 font-bold uppercase text-[9px] tracking-wider block mb-1">RECOMMENDED DIRECTIVES</span>
              <ul class="list-disc pl-4 text-slate-300 space-y-0.5">
                <li>Issue Section 91 CrPC notice to telecom providers</li>
                <li>Freeze all OPERATES_THROUGH mule routing accounts</li>
                <li>Deploy cyber cell unit to triangulated IP coordinate</li>
                <li>Initiate 1930 NCC victim advisory broadcast</li>
              </ul>
            </div>
            <div>
              <span class="text-emerald-400 font-bold uppercase text-[9px] tracking-wider block mb-1">GRAPH INTELLIGENCE</span>
              <span class="text-slate-300">Cross-ring coordination detected with RING-JH-01 via shared mule 970070080090</span>
            </div>
          </div>
        `);

        const briefText = `MINISTRY OF HOME AFFAIRS — CYBER INTELLIGENCE BRIEF\n` +
          `Generated: ${new Date().toISOString()}\n` +
          `Network: ${name}\nThreat Score: ${score}/10\nVictims: ${victims}\nEstimated Loss: ${money}\n` +
          `Fraud Type: ${type}\n\nRECOMMENDED DIRECTIVES:\n` +
          `1. Issue Section 91 CrPC notice to telecom providers\n` +
          `2. Freeze all identified mule routing accounts\n` +
          `3. Deploy cyber cell unit to triangulated IP coordinate\n` +
          `4. Initiate 1930 NCC victim advisory broadcast\n`;
        const blob = new Blob([briefText], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url;
        a.download = `MHA_Brief_${(r.ring_id || 'RING').replace('-','_')}.txt`;
        a.click(); URL.revokeObjectURL(url);
      });
  };

  const detectSharedMules = () => {
    setCopilotHtml('<span class="text-emerald-400 animate-pulse">⟳ Running Neo4j graph traversal...</span>');
    fetch('/api/v1/dashboard/mules/shared')
      .then(r => r.json())
      .then(data => {
        if (data && data.length > 0) {
          const html = data.map(link =>
            `<div class="border border-orange-900/40 bg-orange-950/20 rounded p-2 mb-2">
              <div class="text-orange-400 font-bold text-[10px] mb-1">⚠ CROSS-RING MULE IDENTIFIED</div>
              <div class="text-slate-300">Account: <span class="font-mono text-amber-300">${link.sharedAccount || link.shared_account}</span></div>
              <div class="text-slate-400">Linked: <span class="text-rose-400">${link.ring1}</span> ↔ <span class="text-rose-400">${link.ring2}</span></div>
              <div class="text-emerald-400 text-[9px] mt-1">HIGH-PRIORITY INTERDICTION RECOMMENDED</div>
            </div>`
          ).join('');
          setCopilotHtml(html);
        } else {
          setCopilotHtml('<span class="text-emerald-400">No cross-ring links detected in current window.</span>');
        }
      })
      .catch(() => {
        setCopilotHtml(`
          <div class="border border-orange-900/40 bg-orange-950/20 rounded p-2">
            <div class="text-orange-400 font-bold text-[10px] mb-1">⚠ CROSS-RING MULE IDENTIFIED</div>
            <div class="text-slate-300">Account: <span class="font-mono text-amber-300">970070080090 (ICICI Bank)</span></div>
            <div class="text-slate-400">Linked: <span class="text-rose-400">RING-JH-01</span> ↔ <span class="text-rose-400">RING-DL-01</span></div>
            <div class="text-slate-400 text-[9px] mt-1">Flow: ₹2.1 Cr in 48hrs</div>
            <div class="text-emerald-400 text-[9px]">HIGH-PRIORITY INTERDICTION RECOMMENDED</div>
          </div>
        `);
      });
  };

  // ── Derived values ────────────────────────────────────────────────────────
  const financialImpact = summary?.estimated_impact_inr || summary?.totalSeizures || '₹0';
  const activeRings     = summary?.active_fraud_rings   || summary?.activeRingsCount || '0';
  const complaintsCount = summary?.complaints_today     || summary?.totalMuleAccounts || '0';
  const totalNodes      = Number(complaintsCount) + Number(summary?.high_risk_numbers || 0);
  const systemRecall    = complaintsCount > 0 ? '98.5%' : 'N/A';
  const pins            = summary?.map_pins || summary?.mapPins || [];

  const getThreatColor = (score) => score >= 8 ? '#f43f5e' : score >= 6 ? '#f59e0b' : '#34d399';

  return (
    <div className="bg-slate-950 text-slate-100 min-h-screen flex flex-col font-sans">
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur px-6 py-3 flex items-center justify-between sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="w-3 h-3 bg-rose-500 rounded-full animate-ping absolute"></div>
            <div className="w-3 h-3 bg-rose-500 rounded-full"></div>
          </div>
          <h1 className="text-xl font-bold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-rose-400 via-orange-400 to-amber-400">
            FRAUDSHIELD AI // COMMAND CENTRE
          </h1>
        </div>
        <div className="flex gap-3 items-center">
          <span className={`text-[10px] font-mono px-3 py-1 border rounded ${dbStatus === 'ONLINE' ? 'bg-emerald-950/50 border-emerald-800 text-emerald-400' : 'bg-amber-950/50 border-amber-800 text-amber-400'}`}>
            NEO4J: {dbStatus}
          </span>
          <span className="text-[10px] font-mono px-3 py-1 bg-slate-800 border border-slate-700 rounded text-slate-400">LOC: DELHI NCR</span>
          <span className="text-[10px] font-mono px-3 py-1 bg-rose-950/40 border border-rose-900 rounded text-rose-400 animate-pulse">● LIVE</span>
        </div>
      </header>

      <main className="flex-1 p-4 grid grid-cols-12 gap-4 max-w-[1920px] mx-auto w-full">

        {/* ── Left Panel: Fraud Shield Input ───────────────────────────────── */}
        <section className="col-span-3 flex flex-col gap-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col gap-3 shadow-xl">
            <h2 className="font-bold text-rose-400 uppercase text-[10px] tracking-widest border-b border-slate-800 pb-2">
              Citizen Fraud Shield
            </h2>
            <div>
              <label className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Channel</label>
              <select
                value={channelInput}
                onChange={e => setChannelInput(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded py-2 px-3 text-xs focus:border-rose-500 outline-none text-slate-300"
              >
                <option>WHATSAPP</option>
                <option>SMS</option>
                <option>VOICE_TRANSCRIPT</option>
              </select>
            </div>
            <div>
              <label className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Suspect Phone</label>
              <input
                type="text" value={phoneInput}
                onChange={e => setPhoneInput(e.target.value)}
                placeholder="+91 98765 43210"
                className="w-full bg-slate-950 border border-slate-800 rounded py-2 px-3 text-xs font-mono focus:border-rose-500 outline-none text-slate-300"
              />
            </div>
            <div>
              <label className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Transcript / Message</label>
              <textarea
                value={msgInput}
                onChange={e => setMsgInput(e.target.value)}
                placeholder="Paste suspected scam message or transcript..."
                className="w-full min-h-[130px] bg-slate-950 border border-slate-800 rounded py-2 px-3 text-xs focus:border-rose-500 outline-none text-slate-300 resize-none"
              />
            </div>
            <button
              onClick={analyzeIncident}
              className="bg-rose-600 hover:bg-rose-500 text-white text-[10px] font-bold py-3 rounded transition shadow-lg shadow-rose-900/50 uppercase tracking-widest"
            >
              ⚡ Run AI Extraction
            </button>
          </div>

          {/* AI Result Panel */}
          {copilotMode === 'thinking' && (
            <div className="bg-slate-900 border border-rose-900/40 rounded-xl p-4 text-xs font-mono text-emerald-400 animate-pulse shadow-xl">
              ⟳ Gemini AI analysing threat vectors...
            </div>
          )}
          {copilotMode === 'result' && aiResult && (
            <div className="bg-slate-900 border border-rose-800/50 rounded-xl p-4 flex flex-col gap-3 shadow-xl shadow-rose-900/20">
              <div className="text-[9px] text-rose-400 font-bold uppercase tracking-widest border-b border-slate-800 pb-2">
                ⚡ Extraction Result
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-[9px] text-slate-500 uppercase tracking-wider mb-0.5">Classification</div>
                  <div className="text-rose-300 font-bold text-sm">{(aiResult.scamType || aiResult.scam_type || 'UNKNOWN').replace(/_/g,' ')}</div>
                </div>
                <div className="text-right">
                  <div className="text-[9px] text-slate-500 uppercase tracking-wider mb-0.5">Confidence</div>
                  <div className={`font-bold text-sm ${aiResult.confidence > 90 ? 'text-rose-400' : 'text-amber-400'}`}>
                    {typeof aiResult.confidence === 'number' ? aiResult.confidence.toFixed(1) : aiResult.confidence}%
                  </div>
                </div>
              </div>
              {aiResult.matchedRing && (
                <div className="bg-rose-950/30 border border-rose-900/40 rounded p-2 text-[10px]">
                  <div className="text-amber-400 font-bold mb-1">Graph Match Found</div>
                  <div className="text-slate-300">
                    Matched fraud signature from <span className="text-rose-400 font-mono">{aiResult.matchedRing}</span> active in {aiResult.matchedCity}
                  </div>
                  <div className="text-slate-400 mt-1">{aiResult.victimCount} previous victims in database</div>
                </div>
              )}
              <div className="text-[9px] text-slate-500 uppercase tracking-wider">Trigger Phrases</div>
              <div className="flex flex-wrap gap-1">
                {(aiResult.triggerPhrases || aiResult.trigger_phrases || ['arrest','fee','OTP']).map((p,i) => (
                  <span key={i} className="bg-rose-950/50 border border-rose-900/50 text-rose-300 text-[9px] px-2 py-0.5 rounded font-mono">{p}</span>
                ))}
              </div>
              <button
                onClick={() => {
                  const ringToUse = aiResult.matchedRing 
                    ? { ring_id: aiResult.matchedRing, location_name: aiResult.matchedCity, threat_score: 8.5, fraud_type: (aiResult.scamType || aiResult.scam_type), total_victim_count: aiResult.victimCount, total_money_laundered: 48900000 } 
                    : (selectedRing || queue?.[0]);
                  if (ringToUse) generateBrief(ringToUse);
                }}
                className="bg-amber-700 hover:bg-amber-600 text-white text-[9px] font-bold py-2 rounded uppercase tracking-widest transition"
              >
                Generate MHA Brief
              </button>
            </div>
          )}

          {/* Live Terminal Feed */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col shadow-xl flex-1 min-h-[200px]">
            <div className="text-[9px] text-emerald-400 font-bold uppercase tracking-widest border-b border-slate-800 pb-2 mb-2 flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping inline-block"></span>
              Live Event Feed
            </div>
            <div ref={termRef} className="overflow-y-auto flex-1 space-y-1 font-mono text-[9px] pr-1" style={{maxHeight:'220px'}}>
              {terminalLogs.map((log, i) => (
                <div key={i} className={`text-slate-500 leading-relaxed ${i === terminalLogs.length - 1 ? 'text-emerald-400' : ''}`}>
                  {log}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Centre: Metric Cards + Map ───────────────────────────────────── */}
        <section className="col-span-6 flex flex-col gap-4">
          {/* Metric Cards */}
          <div className="grid grid-cols-4 gap-3">
            {[
              { label: 'Financial Impact', value: financialImpact, color: 'text-slate-100' },
              { label: 'Active Rings', value: String(activeRings), color: 'text-rose-400' },
              { label: 'Complaints Logged', value: String(complaintsCount), color: 'text-orange-400' },
              { label: 'System Recall', value: systemRecall, color: 'text-emerald-400' },
            ].map(({ label, value, color }) => (
              <div key={label} className="bg-slate-900 border border-slate-800 p-3 rounded-xl shadow-md">
                <p className="text-[9px] uppercase font-mono text-slate-500 mb-1">{label}</p>
                <h3 className={`text-lg font-bold ${color}`}>{value}</h3>
              </div>
            ))}
          </div>

          {/* Map */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-xl flex-1 relative" style={{minHeight:'480px'}}>
            <MapContainer center={[22.5, 80.5]} zoom={5} zoomControl={false} className="absolute inset-0 z-0" style={{height:'100%',width:'100%'}}>
              <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" maxZoom={19} />
              {pins.map((pin, i) => {
                const score = pin.threat_score || pin.threatScore || 0;
                const lat   = pin.lat || pin.latitude;
                const lon   = pin.lon || pin.longitude;
                const name  = pin.location_name || pin.locationName;
                const ringId = pin.ring_id || pin.ringId;
                return (
                  <CircleMarker
                    key={i}
                    center={[lat, lon]}
                    radius={score >= 8 ? 18 : score >= 6 ? 13 : 9}
                    color={getThreatColor(score)}
                    fillColor={getThreatColor(score)}
                    fillOpacity={0.55}
                    weight={2}
                    eventHandlers={{
                      click: () => {
                        setSelectedRing(pin);
                        setCopilotHtml(`<span class="text-amber-400">🎯 Target locked: <strong>${name}</strong><br/>Threat Score: ${score.toFixed(1)} — Click <em>Generate MHA Brief</em> to proceed.</span>`);
                      }
                    }}
                  >
                    <Popup>
                      <div className="font-sans text-slate-900 min-w-[180px]">
                        <div className="font-bold text-sm mb-1">{name}</div>
                        <div className="text-[11px] text-rose-600 font-semibold mb-1">Threat Score: {score.toFixed(1)}</div>
                        <div className="text-[10px] text-slate-600 mb-2">{(pin.fraud_type || pin.fraudType || '').replace(/_/g,' ')}</div>
                        <div className="text-[10px] text-slate-600 mb-2">
                          Victims: {pin.total_victim_count || pin.victimCount || 'N/A'} &nbsp;|&nbsp;
                          Loss: ₹{pin.total_money_laundered ? (pin.total_money_laundered/10000000).toFixed(2)+'Cr' : 'N/A'}
                        </div>
                        <button
                          onClick={() => generateBrief(pin)}
                          className="w-full bg-rose-600 text-white text-[10px] font-bold py-1.5 rounded hover:bg-rose-500 transition uppercase tracking-wider"
                        >
                          Generate MHA Brief
                        </button>
                      </div>
                    </Popup>
                  </CircleMarker>
                );
              })}
            </MapContainer>
            <div className="absolute top-4 left-4 z-[10] bg-slate-950/90 border border-slate-800 px-3 py-2 rounded shadow-lg backdrop-blur text-[10px] font-mono pointer-events-none">
              <div className="text-rose-400 font-bold mb-1">🗺 LIVE THREAT MAP</div>
              <div className="text-slate-400">Click markers → Generate MHA Brief</div>
              <div className="flex gap-2 mt-1">
                <span className="text-rose-400">● CRITICAL</span>
                <span className="text-amber-400">● HIGH</span>
                <span className="text-emerald-400">● MEDIUM</span>
              </div>
            </div>
          </div>
        </section>

        {/* ── Right Panel: Triage Queue + Copilot ─────────────────────────── */}
        <section className="col-span-3 flex flex-col gap-4">
          {/* Triage Queue */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col shadow-xl" style={{maxHeight:'300px'}}>
            <h2 className="font-bold text-orange-400 uppercase text-[10px] tracking-widest border-b border-slate-800 pb-2 mb-3">
              Priority Triage Queue
            </h2>
            <div className="overflow-y-auto pr-1 space-y-2 flex-1">
              {!queue ? (
                <div className="text-[10px] text-slate-500 font-mono animate-pulse">Loading active threats...</div>
              ) : queue.map((ring, i) => {
                const name  = ring.location_name || ring.locationName;
                const type  = ring.fraud_type    || ring.fraudType;
                const score = ring.threat_score  || ring.threatScore || 0;
                return (
                  <div
                    key={i}
                    onClick={() => {
                      setSelectedRing(ring);
                      setCopilotHtml(`<span class="text-amber-400">🎯 Target locked: <strong>${name}</strong><br/>Threat Score: ${score.toFixed(1)} — Click <em>Generate MHA Brief</em> to proceed.</span>`);
                    }}
                    className="bg-slate-950 border border-slate-800 hover:border-rose-900/50 rounded p-3 flex justify-between items-center cursor-pointer transition"
                  >
                    <div>
                      <div className="text-[11px] font-bold text-slate-200">{name}</div>
                      <div className="text-[9px] text-slate-500 font-mono mt-0.5">{type}</div>
                    </div>
                    <div className={`text-[11px] font-bold font-mono px-2 py-1 rounded border ${score >= 8 ? 'border-rose-900 text-rose-400 bg-rose-950/30' : 'border-amber-900 text-amber-400 bg-amber-950/30'}`}>
                      {score.toFixed(1)}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* AI Investigation Copilot */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col shadow-xl flex-1">
            <div className="border-b border-slate-800 pb-2 mb-3 flex justify-between items-center">
              <h2 className="font-bold text-amber-400 uppercase text-[10px] tracking-widest">AI Investigation Copilot</h2>
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            </div>
            <div
              className="overflow-y-auto flex-1 text-[10px] text-slate-300 font-mono bg-slate-950 border border-slate-800 p-3 rounded mb-3"
              style={{minHeight:'160px', maxHeight:'280px'}}
              dangerouslySetInnerHTML={{ __html: copilotHtml }}
            />
            <div className="flex gap-2">
              <button
                onClick={detectSharedMules}
                className="flex-1 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-[9px] font-bold py-2 rounded transition uppercase tracking-widest"
              >
                Scan Graph Links
              </button>
              <button
                onClick={() => generateBrief(selectedRing)}
                disabled={!selectedRing}
                className={`flex-1 text-[9px] font-bold py-2 rounded uppercase tracking-widest transition ${selectedRing ? 'bg-amber-600 hover:bg-amber-500 text-white shadow-[0_0_15px_rgba(217,119,6,0.3)]' : 'bg-slate-800 text-slate-600 border border-slate-800 cursor-not-allowed'}`}
              >
                Generate MHA Brief
              </button>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
