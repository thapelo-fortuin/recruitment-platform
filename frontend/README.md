# Nextstep frontend

A React + Vite frontend for the graduate recruitment platform. Candidates can browse graduate roles, apply, and track application status. Recruiters can post and close roles, review applicants, and update their progress.

## Run locally

1. Start the backend services from the repository root with `docker compose up --build`.
2. In this directory, run `npm install` once, then `npm run dev`.
3. Open the Vite URL shown in the terminal (usually `http://localhost:5173`).

The Vite dev server proxies `/api/v1` requests to `http://localhost:8080` (the API gateway) and `/soc-api` requests to `http://localhost:8084` (the SOC service). The SOC proxy rewrites `/soc-api` to `/api/v1`, so the in-app Security insights page can call the RAG endpoint without a browser cross-origin request. To use different service addresses, edit `server.proxy` in `vite.config.js`.

The sign-in dialog includes candidate and recruiter previews so the interface can be explored without a running backend. Preview actions are stored locally in the browser and are not sent to the API.
