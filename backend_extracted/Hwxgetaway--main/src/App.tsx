/**
 * HWX SMS Gateway - Backend API & Server Console
 *
 * This frontend entry serves as the operational status monitor and documentation viewer
 * for the backend server running on Express, WebSocket, and Firebase Admin.
 */
import { useEffect, useState } from 'react';
import { Server, ShieldCheck, Smartphone, Send, BookOpen, Activity, Terminal } from 'lucide-react';

interface HealthData {
  status: string;
  service: string;
  version: string;
  uptimeSeconds: number;
  environment: string;
  firestore: string;
  memoryUsageMB?: {
    rss: number;
    heapTotal: number;
    heapUsed: number;
  };
}

export default function App() {
  const [health, setHealth] = useState<HealthData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/health')
      .then((res) => res.json())
      .then((data) => {
        setHealth(data);
        setLoading(false);
      })
      .catch((err) => {
        console.warn('Failed to fetch health status:', err);
        setLoading(false);
      });
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-cyan-500 selection:text-white">
      {/* Top Banner */}
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur px-6 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <Server className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="font-semibold text-lg tracking-tight text-white flex items-center gap-2">
              HWX SMS Gateway <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-400 border border-cyan-800">Backend v1.0.0</span>
            </h1>
            <p className="text-xs text-slate-400">Firebase Admin &bull; Cloud Firestore &bull; WebSocket Gateway</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <a
            href="/docs"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-medium transition-colors shadow-sm"
          >
            <BookOpen className="h-4 w-4" />
            Swagger OpenAPI Docs
          </a>
          <a
            href="/health"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition-colors"
          >
            <Activity className="h-3.5 w-3.5 text-emerald-400" />
            /health
          </a>
        </div>
      </header>

      {/* Main Status Container */}
      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-8">
        {/* Status Pill & Overview */}
        <section className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-cyan-500/5 rounded-full blur-3xl -z-0 pointer-events-none" />

          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-emerald-950 border border-emerald-800 text-emerald-400 text-xs font-medium mb-3">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Backend Engine Active
              </div>
              <h2 className="text-2xl font-bold text-white tracking-tight">Android SMS Gateway Backend Server</h2>
              <p className="text-sm text-slate-400 mt-1 max-w-2xl">
                Ready for production deployment on Render, Docker, or Google Cloud Run. Connect Android devices via WebSocket on{' '}
                <code className="text-cyan-400 font-mono text-xs bg-slate-800 px-1.5 py-0.5 rounded">/ws/device</code>.
              </p>
            </div>

            <div className="flex flex-col gap-2 font-mono text-xs bg-slate-950/80 p-4 rounded-xl border border-slate-800/80 min-w-[240px]">
              <div className="flex justify-between">
                <span className="text-slate-400">Firebase Project:</span>
                <span className="text-cyan-400 font-semibold">hwxgetway</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Package:</span>
                <span className="text-slate-200">com.hwxgetway</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Uptime:</span>
                <span className="text-emerald-400">{health?.uptimeSeconds ?? 0}s</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Memory RSS:</span>
                <span className="text-slate-300">{health?.memoryUsageMB?.rss ?? '...'} MB</span>
              </div>
            </div>
          </div>
        </section>

        {/* Core Architecture Cards */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
            <div className="h-9 w-9 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-400 flex items-center justify-center">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <h3 className="font-semibold text-white text-base">Firebase Auth &amp; API Keys</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Verifies Firebase ID Tokens for Google and Email logins. SHA-256 hashed API keys with recognizable prefix <code className="text-cyan-400 font-mono">hwx_live_</code>.
            </p>
          </div>

          <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
            <div className="h-9 w-9 rounded-lg bg-purple-500/10 border border-purple-500/20 text-purple-400 flex items-center justify-center">
              <Smartphone className="h-5 w-5" />
            </div>
            <h3 className="font-semibold text-white text-base">Dual SIM &amp; Device Manager</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Multi-device Android management with Dual-SIM support. Supports Manual SIM, Auto Switch (with authorized fallbacks), and Bot Fixed SIM modes.
            </p>
          </div>

          <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
            <div className="h-9 w-9 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <Send className="h-5 w-5" />
            </div>
            <h3 className="font-semibold text-white text-base">Persistent SMS &amp; Webhooks</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Durable Firestore SMS jobs, duplicate-safe idempotency keys, real-time WebSocket dispatch, and SSRF-protected HMAC-SHA256 webhooks.
            </p>
          </div>
        </section>

        {/* Quick cURL Endpoints Reference */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
          <div className="flex items-center gap-2 text-white font-semibold text-sm">
            <Terminal className="h-4 w-4 text-cyan-400" />
            <h3>Quick API Verification Reference</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 font-mono text-xs text-slate-300 space-y-1">
              <div className="text-cyan-400 font-semibold text-[11px] uppercase tracking-wider">Health Check</div>
              <div className="select-all overflow-x-auto text-slate-300 py-1">
                curl -i http://localhost:3000/health
              </div>
            </div>

            <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 font-mono text-xs text-slate-300 space-y-1">
              <div className="text-cyan-400 font-semibold text-[11px] uppercase tracking-wider">Send SMS (API Key)</div>
              <div className="select-all overflow-x-auto text-slate-300 py-1">
                curl -X POST http://localhost:3000/v1/sms/send -H "X-API-Key: hwx_live_..." -H "Content-Type: application/json" -d '{`"bot_id":"bot_123","device_id":"dev_123","to":"+1234567890","message":"Hello"`}'
              </div>
            </div>

            <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 font-mono text-xs text-slate-300 space-y-1">
              <div className="text-cyan-400 font-semibold text-[11px] uppercase tracking-wider">WebSocket Gateway URL</div>
              <div className="select-all overflow-x-auto text-slate-300 py-1">
                ws://localhost:3000/ws/device?deviceId=dev_...&amp;deviceToken=hwx_dev_...
              </div>
            </div>

            <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 font-mono text-xs text-slate-300 space-y-1">
              <div className="text-cyan-400 font-semibold text-[11px] uppercase tracking-wider">Interactive Swagger Docs</div>
              <div className="select-all overflow-x-auto text-cyan-300 py-1">
                <a href="/docs" target="_blank" rel="noopener noreferrer" className="underline hover:text-cyan-200">
                  Open http://localhost:3000/docs
                </a>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800 py-4 px-6 text-center text-xs text-slate-500">
        HWX SMS Gateway &bull; Designed for legitimate, consent-based messaging adhering to carrier regulations
      </footer>
    </div>
  );
}
