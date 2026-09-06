import React, { useState } from 'react';
import { ShieldAlert, Key, Mail, Eye, EyeOff, Loader2, Sparkles } from 'lucide-react';

interface AdminLoginProps {
  onLoginSuccess: (email: string, uid: string) => void;
}

export const AdminLogin: React.FC<AdminLoginProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) {
      setError('Please provide both administrative username and password.');
      return;
    }

    setIsLoading(true);
    setError(null);

    // Simulate network delay for verification experience
    setTimeout(() => {
      const targetUser = email.trim().toLowerCase();
      const targetPass = password;

      if (targetUser === 'admin@drigo' && targetPass === 'Asdf@8111') {
        const session = { email: 'admin@drigo', uid: 'admin-hardcoded' };
        localStorage.setItem('drigo_admin_session', JSON.stringify(session));
        onLoginSuccess('admin@drigo', 'admin-hardcoded');
      } else {
        setError('Invalid administrative credentials. Use admin@drigo with password Asdf@8111.');
      }
      setIsLoading(false);
    }, 800);
  };

  const fillDemoCredentials = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError(null);
  };

  return (
    <div className="min-h-screen w-screen bg-slate-950 flex flex-col items-center justify-center p-4 font-sans antialiased text-slate-100 overflow-y-auto">
      {/* Decorative clean ambient backdrop (No slop gradients, just solid layered rings) */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none opacity-20">
        <div className="absolute -top-[40%] -left-[20%] w-[80%] h-[80%] rounded-full bg-slate-900 border border-slate-800"></div>
        <div className="absolute -bottom-[40%] -right-[20%] w-[80%] h-[80%] rounded-full bg-slate-900 border border-slate-800"></div>
      </div>

      <div className="w-full max-w-md bg-slate-900/90 rounded-2xl border border-slate-800 shadow-2xl p-8 backdrop-blur-md relative z-10">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center shadow-lg shadow-blue-500/20 ring-1 ring-white/20 mb-3">
            <span className="text-white font-black text-2xl tracking-wider">D</span>
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            DRIGO <span className="bg-blue-600/20 text-blue-400 text-xs font-bold px-2 py-0.5 rounded border border-blue-500/30">ADMIN</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1.5 text-center max-w-[280px]">
            Ride-Sharing Fleet Command & Operations Management System
          </p>
        </div>

        {/* Security Alert Banner */}
        <div className="mb-6 p-3 rounded-lg bg-blue-500/10 border border-blue-500/20 text-[11px] text-blue-300 flex items-start gap-2.5">
          <ShieldAlert className="w-4 h-4 text-blue-400 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold block mb-0.5">Secure Gateway Access</span>
            Only registered operators with valid hardcoded credentials can authenticate here.
          </div>
        </div>

        {/* Error Banner */}
        {error && (
          <div className="mb-6 p-3.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 font-semibold animate-fadeIn">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Email/Username field */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">
              Operator Username
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Mail className="w-4 h-4" />
              </span>
              <input
                type="text"
                required
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setError(null);
                }}
                placeholder="admin@drigo"
                className="w-full bg-slate-950 text-white placeholder-slate-500 text-sm rounded-xl pl-10 pr-4 py-3 border border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all outline-none"
                disabled={isLoading}
              />
            </div>
          </div>

          {/* Password field */}
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">
              Authorization Key
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Key className="w-4 h-4" />
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setError(null);
                }}
                placeholder="Asdf@8111"
                className="w-full bg-slate-950 text-white placeholder-slate-500 text-sm rounded-xl pl-10 pr-10 py-3 border border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all outline-none"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-white transition-colors"
                disabled={isLoading}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Submit button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm py-3 px-4 rounded-xl shadow-lg hover:shadow-blue-600/20 active:translate-y-[1px] transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed mt-4"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-white" />
                <span>Verifying Role Credentials...</span>
              </>
            ) : (
              <span>Decrypt & Establish Session</span>
            )}
          </button>
        </form>

        {/* Hardcoded Credentials Sandbox */}
        <div className="mt-8 pt-6 border-t border-slate-800">
          <div className="flex items-center gap-1.5 mb-3 text-slate-300">
            <Sparkles className="w-3.5 h-3.5 text-blue-400" />
            <span className="text-xs font-bold uppercase tracking-wider">Required Credentials</span>
          </div>
          <p className="text-[10px] text-slate-400 mb-4 leading-relaxed">
            Click below to load the secure, requested administrator credentials instantly:
          </p>
          <div className="space-y-2">
            <button
              type="button"
              onClick={() => fillDemoCredentials('admin@drigo', 'Asdf@8111')}
              className="w-full flex items-center justify-between p-2.5 rounded-lg bg-slate-950/60 hover:bg-slate-950 border border-slate-800 hover:border-slate-700 transition-all text-left text-[11px]"
            >
              <div>
                <div className="font-bold text-slate-200">admin@drigo</div>
                <div className="text-[10px] text-slate-400">Pass: Asdf@8111</div>
              </div>
              <span className="bg-emerald-500/10 text-emerald-400 font-bold px-1.5 py-0.5 rounded border border-emerald-500/20 animate-pulse">
                Active Admin
              </span>
            </button>
          </div>
        </div>
      </div>
      <div className="text-[10px] text-slate-500 mt-6 select-none font-medium">
        DRIGO INC. • METROPOLITAN FLEET COMMAND v3.1.0 • SSL PROTECTED
      </div>
    </div>
  );
};
