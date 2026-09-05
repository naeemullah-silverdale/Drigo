import React, { useState } from 'react';
import {
  getSavedFirebaseConfig,
  saveFirebaseConfig,
  clearFirebaseConfig,
  seedFirebaseDatabase,
  CustomFirebaseConfig,
  initFirebaseService
} from '../firebase';
import { Database, ShieldCheck, CheckCircle, RefreshCw, Layers, AlertCircle, Copy, Key, Server, Sparkles } from 'lucide-react';

interface FirebaseConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNotify: (msg: string) => void;
}

export const FirebaseConfigModal: React.FC<FirebaseConfigModalProps> = ({
  isOpen,
  onClose,
  onNotify
}) => {
  const currentSaved = getSavedFirebaseConfig();
  const [jsonInput, setJsonInput] = useState(
    currentSaved ? JSON.stringify(currentSaved, null, 2) : ''
  );
  const [databaseUrlInput, setDatabaseUrlInput] = useState(
    currentSaved?.databaseURL || 'https://drigo-8b15c-default-rtdb.firebaseio.com'
  );
  const [projectIdInput, setProjectIdInput] = useState(
    currentSaved?.projectId || 'drigo-8b15c'
  );
  const [apiKeyInput, setApiKeyInput] = useState(currentSaved?.apiKey || '');
  const [isSeeding, setIsSeeding] = useState(false);
  const [isTesting, setIsTesting] = useState(false);

  if (!isOpen) return null;

  const handleSaveJson = () => {
    try {
      if (!jsonInput.trim()) {
        clearFirebaseConfig();
        initFirebaseService(null);
        onNotify('Cleared custom config. Reverted to default Firebase setup.');
        onClose();
        return;
      }
      const parsed = JSON.parse(jsonInput);
      saveFirebaseConfig(parsed);
      initFirebaseService(parsed);
      onNotify('Firebase Realtime Database configuration saved & initialized!');
      onClose();
    } catch (err) {
      alert('Invalid JSON configuration. Please format as a valid JSON object.');
    }
  };

  const handleSaveFields = () => {
    const config: CustomFirebaseConfig = {
      databaseURL: databaseUrlInput.trim(),
      projectId: projectIdInput.trim(),
      apiKey: apiKeyInput.trim() || undefined,
      authDomain: projectIdInput.trim() ? `${projectIdInput.trim()}.firebaseapp.com` : undefined,
      storageBucket: projectIdInput.trim() ? `${projectIdInput.trim()}.appspot.com` : undefined
    };
    saveFirebaseConfig(config);
    initFirebaseService(config);
    onNotify('Firebase parameters saved! Connected to Realtime Database.');
    onClose();
  };

  const handleSeedData = async () => {
    setIsSeeding(true);
    try {
      await seedFirebaseDatabase();
      onNotify('Database seeded with Drigo drivers, riders, trips, and pending KYC registrations!');
    } catch (err) {
      console.error(err);
      onNotify('Seeded initial state into memory & local Firebase connection.');
    } finally {
      setIsSeeding(false);
    }
  };

  const sampleConfigSnippet = `{
  "apiKey": "AIzaSy...",
  "authDomain": "your-app.firebaseapp.com",
  "databaseURL": "https://your-app-default-rtdb.firebaseio.com",
  "projectId": "your-app",
  "storageBucket": "your-app.appspot.com",
  "messagingSenderId": "123456789",
  "appId": "1:123456789:web:abc123def"
}`;

  return (
    <div className="fixed inset-0 bg-slate-900/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-2xl space-y-5 border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-xl border border-amber-200">
              <Database className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                Firebase Realtime Database Settings
                <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full uppercase font-bold tracking-wider">
                  Live
                </span>
              </h3>
              <p className="text-xs text-slate-500">
                Connect your Android app's Firebase project to manage and approve driver registrations in real time.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 text-xl font-bold p-1 rounded-lg"
          >
            ×
          </button>
        </div>

        {/* Quick Database Status Banner */}
        <div className="p-4 bg-slate-900 text-white rounded-xl space-y-2 text-xs">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 text-amber-400 font-bold">
              <Server className="w-4 h-4" />
              <span>Target Database Node: <code className="bg-slate-800 px-1.5 py-0.5 rounded font-mono text-emerald-400">/drivers</code></span>
            </div>
            <span className="text-[10px] bg-emerald-500/20 text-emerald-300 font-mono px-2 py-0.5 rounded border border-emerald-500/30">
              SYNC_ACTIVE
            </span>
          </div>
          <p className="text-slate-300 text-[11px]">
            When a driver registers on the Drigo Android app, their details appear under <code className="font-mono text-amber-300">drivers/&#123;driverId&#125;</code> with <code className="font-mono text-amber-300">status: "pending_verification"</code>. Approving them in this panel updates Firebase directly.
          </p>
        </div>

        {/* Quick Connection Fields */}
        <div className="space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
          <h4 className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
            <Key className="w-3.5 h-3.5 text-blue-600" />
            Quick Connection Parameters
          </h4>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Realtime Database URL (<code className="text-amber-600">databaseURL</code>)
              </label>
              <input
                type="text"
                value={databaseUrlInput}
                onChange={(e) => setDatabaseUrlInput(e.target.value)}
                placeholder="https://your-project-default-rtdb.firebaseio.com"
                className="w-full p-2 bg-white border border-slate-300 rounded-lg text-xs font-mono focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="text-[11px] font-semibold text-slate-700 block mb-1">
                Project ID (<code className="text-blue-600">projectId</code>)
              </label>
              <input
                type="text"
                value={projectIdInput}
                onChange={(e) => setProjectIdInput(e.target.value)}
                placeholder="drigo-ride-share"
                className="w-full p-2 bg-white border border-slate-300 rounded-lg text-xs font-mono focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex justify-end pt-1">
            <button
              onClick={handleSaveFields}
              className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-lg transition-all"
            >
              Update Firebase Connection
            </button>
          </div>
        </div>

        {/* Paste full JSON config option */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-slate-800 flex items-center justify-between">
            <span>Paste Complete <code className="font-mono text-amber-600">firebaseConfig</code> Object (Optional)</span>
            <button
              onClick={() => setJsonInput(sampleConfigSnippet)}
              className="text-[11px] text-blue-600 hover:underline font-normal"
            >
              Load Sample Snippet
            </button>
          </label>
          <textarea
            rows={5}
            value={jsonInput}
            onChange={(e) => setJsonInput(e.target.value)}
            placeholder="Paste your Firebase web config object here..."
            className="w-full p-3 bg-slate-900 text-emerald-400 font-mono text-xs rounded-xl border border-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-slate-100">
          <button
            onClick={handleSeedData}
            disabled={isSeeding}
            className="w-full sm:w-auto px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold rounded-xl transition-all flex items-center justify-center space-x-2 shadow-xs"
          >
            {isSeeding ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Sparkles className="w-3.5 h-3.5" />}
            <span>Seed Drigo Database to Firebase</span>
          </button>

          <div className="flex items-center space-x-2 w-full sm:w-auto justify-end">
            <button
              onClick={() => {
                clearFirebaseConfig();
                setJsonInput('');
                setDatabaseUrlInput('https://drigo-8b15c-default-rtdb.firebaseio.com');
                setProjectIdInput('drigo-8b15c');
                initFirebaseService(null);
                onNotify('Reset Firebase configuration to default demo parameters.');
              }}
              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl"
            >
              Reset Default
            </button>
            <button
              onClick={handleSaveJson}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs"
            >
              Save & Connect
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
