import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, Clock, FileCheck2, Search, Shield } from 'lucide-react';
import Navbar from '../../components/layout/Navbar';
import Button from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import { getDashboardPathForRole } from '../../utils/statusLabels';

export default function Landing() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [trackNum, setTrackNum] = useState('');

  const handleTrack = (e) => {
    e.preventDefault();
    if (!trackNum.trim()) return;
    navigate(`/track?number=${encodeURIComponent(trackNum.trim())}`);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1">
        <section className="bg-gradient-to-br from-slate-900 via-brand-900 to-slate-900 text-white py-20 px-4">
          <div className="max-w-5xl mx-auto text-center space-y-6">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1 text-xs font-medium text-brand-100 border border-white/15">
              Transparent • Traceable • SLA-Tracked
            </span>
            <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight">
              ResolveIt — Complaint &amp; Grievance Management
            </h1>
            <p className="max-w-2xl mx-auto text-base sm:text-lg text-slate-300">
              Register complaints with evidence, route them to the right department, track real-time
              status transitions, and measure resolution against service-level agreements.
            </p>

            <div className="flex flex-wrap justify-center gap-3 pt-2">
              {user ? (
                <Link to={getDashboardPathForRole(user.role)}>
                  <Button size="lg">
                    Go to Dashboard <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
              ) : (
                <>
                  <Link to="/register">
                    <Button size="lg">
                      Submit a Complaint <ArrowRight className="h-4 w-4" />
                    </Button>
                  </Link>
                  <Link to="/login">
                    <Button variant="secondary" size="lg">
                      Staff / User Sign In
                    </Button>
                  </Link>
                </>
              )}
            </div>

            <form
              onSubmit={handleTrack}
              className="mt-8 max-w-xl mx-auto flex flex-col sm:flex-row gap-2 bg-white/10 p-2 rounded-2xl border border-white/15 backdrop-blur"
            >
              <div className="relative flex-1">
                <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-300" />
                <input
                  type="text"
                  value={trackNum}
                  onChange={(e) => setTrackNum(e.target.value)}
                  placeholder="Track by complaint number (e.g. CMP-2026-000001)"
                  className="w-full rounded-xl bg-white/95 pl-10 pr-4 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none"
                />
              </div>
              <Button type="submit" size="md">
                Track Status
              </Button>
            </form>
          </div>
        </section>

        <section className="max-w-6xl mx-auto py-16 px-4 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {[
            {
              icon: FileCheck2,
              title: 'Structured Intake',
              desc: 'Categorized submissions with priority, location, duplicate hints, and validated file attachments.',
            },
            {
              icon: Shield,
              title: 'Role-Based Security',
              desc: 'Strict record-level authorization for Complainants, Employees, Department Managers, and Admins.',
            },
            {
              icon: Clock,
              title: 'SLA & Escalation',
              desc: 'Automated target calculation and idempotent overdue escalation alerts without losing workflow state.',
            },
            {
              icon: CheckCircle2,
              title: 'Full Audit Trail',
              desc: 'Immutable timeline of status transitions, assignments, reassignments, resolutions, and feedback.',
            },
          ].map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.title} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                  <Icon className="h-5 w-5" />
                </div>
                <h3 className="font-semibold text-slate-900">{item.title}</h3>
                <p className="mt-1.5 text-sm text-slate-600">{item.desc}</p>
              </div>
            );
          })}
        </section>
      </main>
    </div>
  );
}

