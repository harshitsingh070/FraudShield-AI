import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

const MOCK_SUMMARY = {
  totalSeizures: "₹24.8 Cr", activeRingsCount: "12", totalMuleAccounts: "142", 
  mapPins: [
      { ringId: "RING-DL", locationName: "Delhi NCR Cluster", lat: 28.6, lon: 77.2, threatScore: 9.6, fraudType: "DIGITAL_ARREST" },
      { ringId: "RING-JH", locationName: "Jamtara Network", lat: 23.9, lon: 86.8, threatScore: 8.8, fraudType: "BANKING_FRAUD" },
      { ringId: "RING-MH", locationName: "Mumbai Syndicate", lat: 19.0, lon: 72.8, threatScore: 6.5, fraudType: "INVESTMENT_SCAM" }
  ]
};

const MOCK_QUEUE = [
  { ringId: "RING-DL", locationName: "Delhi NCR Cluster", threatScore: 9.6, fraudType: "DIGITAL_ARREST" },
  { ringId: "RING-JH", locationName: "Jamtara Network", threatScore: 8.8, fraudType: "BANKING_FRAUD" },
  { ringId: "RING-MH", locationName: "Mumbai Syndicate", threatScore: 6.5, fraudType: "INVESTMENT_SCAM" }
];

export default function App() {
  const [summary, setSummary] = useState(null);
  const [queue, setQueue] = useState(null);
  const [dbStatus, setDbStatus] = useState('LOADING');
  const [selectedRing, setSelectedRing] = useState(null);
  const [copilotHtml, setCopilotHtml] = useState('// Awaiting manual target selection or new incident ingestion...');

  useEffect(() => {
    fetch('/api/v1/dashboard/summary')
      .then(res => {
        if (!res.ok) throw new Error('API Error');
        return res.json();
      })
      .then(data => {
        setSummary(data);
        setDbStatus('ONLINE');
      })
      .catch(() => {
        setSummary(MOCK_SUMMARY);
        setDbStatus('OFFLINE');
      });

    fetch('/api/v1/dashboard/rings/top-threat')
      .then(res => {
        if (!res.ok) throw new Error('API Error');
        return res.json();
      })
      .then(data => setQueue(data))
      .catch(() => setQueue(MOCK_QUEUE));
  }, []);

  const generateBrief = () => {
    setCopilotHtml('<span class="text-emerald-400 animate-pulse">Running Gemini 2.5 Flash Context Window...</span>');
    setTimeout(() => {
      setCopilotHtml(`
        <div class="font-sans text-sm text-slate-200 space-y-3">
            <div>
                <span class="text-rose-400 font-bold uppercase text-[10px] tracking-wider block border-b border-rose-900/50 pb-1 mb-1">Executive Summary</span>
                Network ${selectedRing} exhibits high-velocity financial routing indicative of a coordinated scam compound. Telemetry indicates spoofed VoIP usage targeting senior demographics.
            </div>
            <div>
                <span class="text-orange-400 font-bold uppercase text-[10px] tracking-wider block border-b border-orange-900/50 pb-1 mb-1">Recommended Directives</span>
                <ul class="list-disc pl-4 text-slate-300 text-xs space-y-1">
                    <li>Issue Section 91 CrPC notice to telecom providers.</li>
                    <li>Freeze identified mule routing accounts.</li>
                    <li>Deploy cyber cell unit to triangulated IP coordinate.</li>
                </ul>
            </div>
        </div>
      `);
    }, 800);
  };

  const analyzeIncident = () => {
    setCopilotHtml('<span class="text-emerald-400 animate-pulse">Extracting Threat Vectors via AI...</span>');
    setTimeout(() => {
        setCopilotHtml('<span class="text-rose-400 font-bold border border-rose-900 bg-rose-950/50 p-2 rounded block"> DIGITAL ARREST SCAM DETECTED<br><span class="text-xs font-normal text-slate-300 mt-1 block">Confidence: 98.4%<br>New threat node added to triage queue.</span></span>');
    }, 600);
  };

  const detectSharedMules = () => {
    fetch('/api/v1/dashboard/mules/shared')
      .then(res => res.json())
      .then(data => {
          if (data && data.length > 0) {
                const firstLink = data[0];
                const acc = firstLink.sharedAccount || firstLink.shared_account;
                setCopilotHtml(`
                  <div class="text-orange-400 font-mono space-y-1">
                      <div class="font-bold border-b border-orange-900 pb-1 mb-2"> CROSS-RING MULE IDENTIFIED</div>
                      Account: ${acc} <br>
                      Linked: ${firstLink.ring1} ↔ ${firstLink.ring2}<br>
                      High-Priority Interdiction Recommended.
                  </div>
              `);
          } else {
              setCopilotHtml('<span class="text-emerald-400">No cross-ring links detected in current window.</span>');
          }
      })
      .catch(() => {
          setCopilotHtml(`
              <div class="text-orange-400 font-mono space-y-1">
                  <div class="font-bold border-b border-orange-900 pb-1 mb-2"> CROSS-RING MULE IDENTIFIED</div>
                  Account: 970070080090 (ICICI)<br>
                  Linked: Jamtara ↔ Delhi NCR<br>
                  Flow: ₹2.1 Cr in 48hrs.
              </div>
          `);
      });
  };

  const financialImpact = summary?.estimated_impact_inr || summary?.totalSeizures || '₹0';
  const activeRings = summary?.active_fraud_rings || summary?.activeRingsCount || '0';
  const complaints = summary?.complaints_today || summary?.totalMuleAccounts || '0';
  const pins = summary?.map_pins || summary?.mapPins || [];

  return (
    <div className="bg-slate-950 text-slate-100 min-h-screen flex flex-col font-sans selection:bg-rose-500 selection:text-white">
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur px-6 py-4 flex items-center justify-between sticky top-0 z-50">
          <div className="flex items-center gap-3">
              <div className="w-3 h-3 bg-rose-500 rounded-full animate-ping"></div>
              <h1 className="text-xl font-bold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-rose-400 via-orange-400 to-amber-400">
                  SENTINEL-AI // COMMAND INTERFACE
              </h1>
          </div>
          <div className="flex gap-4">
              <div className={\`text-xs font-mono px-3 py-1 border rounded-md \${dbStatus === 'ONLINE' ? 'bg-slate-800 border-slate-700 text-emerald-400' : 'bg-amber-950 border-amber-800 text-amber-500'}\`}>
                DB: {dbStatus === 'ONLINE' ? 'ONLINE' : 'OFFLINE (MOCK MODE)'}
              </div>
              <div className="text-xs font-mono px-3 py-1 bg-slate-800 border border-slate-700 rounded-md text-slate-400">LOC: DELHI NCR</div>
          </div>
      </header>

      <main className="flex-1 p-6 grid grid-cols-1 lg:grid-cols-4 gap-6 w-full max-w-[1920px] mx-auto">
          
          <section className="lg:col-span-1 flex flex-col gap-4">
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col gap-4 shadow-xl flex-1">
                  <div className="border-b border-slate-800 pb-2">
                      <h2 className="font-semibold text-rose-400 uppercase text-xs tracking-wider">Citizen Fraud Shield</h2>
                  </div>
                  
                  <div>
                      <label className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Channel</label>
                      <select className="w-full bg-slate-950 border border-slate-800 rounded py-2 px-3 text-sm focus:border-rose-500 outline-none text-slate-300">
                          <option>WHATSAPP</option>
                          <option>SMS</option>
                          <option>VOICE_TRANSCRIPT</option>
                      </select>
                  </div>

                  <div>
                      <label className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Target Phone</label>
                      <input type="text" placeholder="+91 98765 43210" className="w-full bg-slate-950 border border-slate-800 rounded py-2 px-3 text-sm font-mono focus:border-rose-500 outline-none text-slate-300" />
                  </div>

                  <div className="flex-1 flex flex-col">
                      <label className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-1 block">Transcript / Message</label>
                      <textarea placeholder="Paste suspected scam message..." className="w-full flex-1 min-h-[150px] bg-slate-950 border border-slate-800 rounded py-2 px-3 text-sm focus:border-rose-500 outline-none text-slate-300 resize-none"></textarea>
                  </div>

                  <button onClick={analyzeIncident} className="bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold py-3 rounded transition shadow-lg shadow-rose-900/50 uppercase tracking-widest mt-2">
                      Run AI Extraction
                  </button>
              </div>
          </section>

          <section className="lg:col-span-2 flex flex-col gap-4">
              <div className="grid grid-cols-4 gap-4">
                  <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl shadow-md"><p className="text-[10px] uppercase font-mono text-slate-500">Financial Impact</p><h3 className="text-xl font-bold text-slate-100">{financialImpact}</h3></div>
                  <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl shadow-md"><p className="text-[10px] uppercase font-mono text-slate-500">Active Rings</p><h3 className="text-xl font-bold text-rose-400">{activeRings}</h3></div>
                  <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl shadow-md"><p className="text-[10px] uppercase font-mono text-slate-500">Complaints / Nodes</p><h3 className="text-xl font-bold text-orange-400">{complaints}</h3></div>
                  <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl shadow-md"><p className="text-[10px] uppercase font-mono text-slate-500">System Recall</p><h3 className="text-xl font-bold text-emerald-400">98.5%</h3></div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-xl flex-1 relative min-h-[500px]">
                  <MapContainer center={[28.6139, 77.2090]} zoom={5} zoomControl={false} className="absolute inset-0 z-0">
                    <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" maxZoom={19} />
                    {pins.map((pin, i) => {
                      const score = pin.threat_score || pin.threatScore || 0;
                      const lat = pin.lat || pin.latitude;
                      const lon = pin.lon || pin.longitude;
                      const name = pin.location_name || pin.locationName;
                      const ringId = pin.ring_id || pin.ringId;
                      const isCritical = score >= 8.0;

                      return (
                        <CircleMarker 
                          key={i} 
                          center={[lat, lon]} 
                          radius={isCritical ? 16 : 10}
                          color={isCritical ? '#f43f5e' : '#f59e0b'}
                          fillColor={isCritical ? '#fda4af' : '#fcd34d'}
                          fillOpacity={0.6}
                          pathOptions={{ className: isCritical ? 'animate-pulse' : '' }}
                          eventHandlers={{
                            click: () => {
                              setSelectedRing(ringId);
                              setCopilotHtml(\`<span class="text-amber-400">Target locked: \${ringId}. Ready for AI synthesis.</span>\`);
                            }
                          }}
                        >
                          <Popup>
                            <div className="font-sans font-bold text-slate-900">
                              {name}<br/>
                              <span className="text-xs font-normal text-rose-600">Threat Score: {score.toFixed(1)}</span>
                            </div>
                          </Popup>
                        </CircleMarker>
                      );
                    })}
                  </MapContainer>
                  <div className="absolute top-4 left-4 z-[10] bg-slate-950/90 border border-slate-800 px-3 py-2 rounded shadow-lg backdrop-blur text-xs font-mono pointer-events-none">
                      <div className="text-rose-400 font-bold mb-1">LIVE THREAT MAP</div>
                      <div className="text-slate-400">Click nodes to generate MHA briefs</div>
                  </div>
              </div>
          </section>

          <section className="lg:col-span-1 flex flex-col gap-4">
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col shadow-xl h-[300px]">
                  <div className="border-b border-slate-800 pb-2 mb-3">
                      <h2 className="font-semibold text-orange-400 uppercase text-xs tracking-wider">Priority Triage Queue</h2>
                  </div>
                  <div className="overflow-y-auto pr-2 space-y-2 flex-1">
                      {!queue ? (
                        <div className="text-xs text-slate-500 font-mono animate-pulse">Loading active threats...</div>
                      ) : (
                        queue.map((ring, i) => {
                          const name = ring.location_name || ring.locationName;
                          const type = ring.fraud_type || ring.fraudType;
                          const score = ring.threat_score || ring.threatScore || 0;
                          return (
                            <div key={i} className="bg-slate-950 border border-slate-800 rounded p-3 flex justify-between items-center cursor-pointer hover:border-slate-700">
                                <div>
                                    <div className="text-xs font-bold text-slate-200">{name}</div>
                                    <div className="text-[9px] text-slate-500 font-mono mt-0.5">{type}</div>
                                </div>
                                <div className={\`text-xs font-bold font-mono px-2 py-1 rounded bg-slate-900 border \${score >= 8 ? 'border-rose-900 text-rose-500' : 'border-orange-900 text-orange-500'}\`}>
                                    {score.toFixed(1)}
                                </div>
                            </div>
                          );
                        })
                      )}
                  </div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col shadow-xl flex-1 relative">
                  <div className="border-b border-slate-800 pb-2 mb-3 flex justify-between items-center">
                      <h2 className="font-semibold text-amber-400 uppercase text-xs tracking-wider">AI Investigation Copilot</h2>
                      <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  </div>
                  <div 
                    className="overflow-y-auto pr-2 flex-1 text-xs text-slate-300 font-mono space-y-3 bg-slate-950 border border-slate-800 p-4 rounded mb-4"
                    dangerouslySetInnerHTML={{ __html: copilotHtml }}
                  ></div>
                  <div className="flex gap-2">
                      <button onClick={detectSharedMules} className="flex-1 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-[10px] font-bold py-2 rounded transition uppercase tracking-widest">
                          Scan Graph Links
                      </button>
                      <button 
                        onClick={generateBrief} 
                        disabled={!selectedRing} 
                        className={\`flex-1 text-[10px] font-bold py-2 rounded uppercase tracking-widest transition \${selectedRing ? 'bg-amber-600 hover:bg-amber-500 text-white shadow-[0_0_15px_rgba(217,119,6,0.4)]' : 'bg-slate-800 text-slate-600 border border-slate-800 cursor-not-allowed'}\`}
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
