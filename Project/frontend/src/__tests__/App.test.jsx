import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import App from '../app/App';

describe('ResolveIt Frontend', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState({}, '', '/');
  });

  it('renders the landing page with public tracking input', () => {
    render(<App />);
    expect(screen.getByText(/ResolveIt — Complaint & Grievance Management/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/CMP-2026-000001/i)).toBeInTheDocument();
  });

  it('validates empty login submission', async () => {
    window.history.pushState({}, '', '/login');
    render(<App />);
    const submitBtn = screen.getByRole('button', { name: /Sign In/i });
    fireEvent.click(submitBtn);
    expect(await screen.findByText(/Email and password are required/i)).toBeInTheDocument();
  });

  it('redirects unauthenticated users away from protected dashboard to login', async () => {
    window.history.pushState({}, '', '/dashboard');
    render(<App />);
    expect(await screen.findByText(/Sign in to ResolveIt/i)).toBeInTheDocument();
  });
});

