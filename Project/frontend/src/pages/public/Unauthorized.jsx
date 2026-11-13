import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import Navbar from '../../components/layout/Navbar';
import Button from '../../components/common/Button';

export default function Unauthorized() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <div className="flex flex-1 flex-col items-center justify-center px-4 text-center space-y-4">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h1 className="text-2xl font-bold text-slate-900">Access Denied</h1>
        <p className="max-w-md text-sm text-slate-600">
          Your account role does not have permission to view this page.
        </p>
        <Link to="/">
          <Button>Return Home</Button>
        </Link>
      </div>
    </div>
  );
}

