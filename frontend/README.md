# Deploy History Frontend

React + Vite frontend 애플리케이션입니다.

## Commands

```bash
npm ci
npm test
npm run build
docker build -t deploy-history-frontend:local .
```

개발 서버의 `/api` 요청은 `http://localhost:8080` backend로 프록시됩니다.
