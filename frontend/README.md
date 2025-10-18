# JobScopeAI Frontend

This is a minimal React + Vite + TypeScript frontend scaffold for JobScopeAI.
It includes a simple UI to interact with AI agents (via a /api/agent endpoint).

Quick start:
- cd frontend
- npm install
- npm run dev

The frontend expects an API endpoint at /api/agent/generate which accepts a POST JSON body { prompt: string } and returns { result: string }.
