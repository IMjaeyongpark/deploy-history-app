# Deploy History App

`deploy-history-app`은 Deploy History 서비스의 애플리케이션 소스코드 레포지토리입니다.

이 프로젝트의 목표는 서비스 기능 완성보다 Kubernetes GitOps CI/CD 흐름을 명확히 검증하는 것입니다. Jenkins가 backend/frontend를 빌드하고 Nexus Registry에 이미지를 push한 뒤, 별도 manifest 레포지토리의 Helm `values.yaml` image tag를 갱신하면 Argo CD가 Kubernetes 배포를 수행합니다.

## Repository Split

이 프로젝트는 GitHub 레포지토리를 2개로 나눕니다.

```text
deploy-history-app
  실제 애플리케이션 소스코드
  backend, frontend, Dockerfile, Jenkinsfile

deploy-history-manifest
  Kubernetes 배포 설정
  Helm chart, Argo CD Application, values.yaml
```

`deploy-history-manifest`의 `image.registry` 값은 `PLACEHOLDER`로 유지합니다. 실제 Nexus Registry 주소는 Jenkins 환경변수 또는 Jenkins Credential에서만 사용합니다.

## Structure

```text
deploy-history-app/
├─ README.md
├─ .gitignore
├─ docker-compose.yml
├─ Jenkinsfile
├─ backend/
│  ├─ build.gradle.kts
│  ├─ settings.gradle.kts
│  ├─ Dockerfile
│  ├─ gradle/
│  └─ src/
│     ├─ main/
│     │  ├─ kotlin/
│     │  └─ resources/
│     │     ├─ application.yml
│     │     ├─ application-local.yml
│     │     └─ application-prod.yml
│     └─ test/
├─ frontend/
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ Dockerfile
│  ├─ index.html
│  ├─ vite.config.ts
│  ├─ public/
│  └─ src/
│     ├─ api/
│     ├─ components/
│     ├─ pages/
│     ├─ router/
│     └─ main.tsx
└─ docs/
   ├─ architecture.md
   └─ api.md
```

## Runtime Architecture

```text
Browser
  -> frontend
  -> backend
  -> PostgreSQL
```

Kubernetes 환경에서 PostgreSQL은 이 앱 레포가 직접 배포하지 않습니다. `deploy-history-manifest` Helm chart가 Postgres를 배포하고 backend에는 아래 환경변수를 주입합니다.

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

Postgres Kubernetes Service 이름은 Helm release 기준 `deploy-history-postgres`입니다.

## Docker Images

Jenkins는 backend/frontend 이미지를 같은 `IMAGE_TAG`로 빌드합니다.

```text
${NEXUS_REGISTRY}/deploy-history/backend:${IMAGE_TAG}
${NEXUS_REGISTRY}/deploy-history/frontend:${IMAGE_TAG}
```

`IMAGE_TAG`는 Jenkins Multibranch Pipeline에서 branch, build number, commit hash를 조합해 생성합니다.

## CI/CD Flow

```text
GitHub deploy-history-app push
  -> Jenkins Multibranch Pipeline
  -> checkout scm
  -> backend build/test
  -> frontend build/test
  -> Docker image build
  -> Nexus push
  -> deploy-history-manifest clone
  -> helm/values.yaml backendTag/frontendTag update
  -> manifest repo commit/push
  -> Argo CD sync
  -> Kubernetes rollout
```

`Jenkinsfile`에서 manifest tag 수정은 아래 형태로 수행합니다.

```bash
sed -i "s|backendTag: .*|backendTag: ${IMAGE_TAG}|g" helm/values.yaml
sed -i "s|frontendTag: .*|frontendTag: ${IMAGE_TAG}|g" helm/values.yaml
```

## Jenkins Requirements

Multibranch Pipeline 기준입니다.

필요한 Jenkins 설정:

- GitHub App Credential: app repo 감지, Jenkinsfile checkout, source checkout
- Fine-grained PAT Credential: `deploy-history-manifest` clone, commit, push
- Nexus Credential: docker login, image push
- Jenkins environment variable: `NEXUS_REGISTRY`

현재 `Jenkinsfile`에서 사용하는 기본 Credential ID:

```text
DEPLOY_REPO_CREDENTIALS_ID = deploy-history-manifest-pat
NEXUS_CREDENTIALS_ID = nexus-credentials
```

Jenkins 환경에 맞게 ID가 다르면 `Jenkinsfile`의 environment 값을 수정합니다.

## Secrets Policy

아래 값은 코드, README, manifest에 실제 값으로 저장하지 않습니다.

- DB password
- JWT secret
- Nexus password
- GitHub token
- 실제 Nexus Registry 주소
- Kubernetes Secret value

앱 레포와 manifest 레포에는 Secret 이름, key 이름, 환경변수 이름만 저장합니다.

## Local Development

로컬 개발용 `docker-compose.yml`은 Postgres, backend, frontend를 함께 실행합니다.

```bash
APP_SECURITY_PASSWORD='local-password' \
JWT_SECRET='local-jwt-secret' \
POSTGRES_PASSWORD='local-db-password' \
docker compose up --build
```

접속:

```text
frontend: http://localhost:3000
backend:  http://localhost:8080
postgres: localhost:5432
```

## Backend Commands

```bash
cd backend
./gradlew test
./gradlew bootJar
docker build -t deploy-history-backend:local .
```

## Frontend Commands

```bash
cd frontend
npm ci
npm test
npm run build
docker build -t deploy-history-frontend:local .
```

## Related Repository

Manifest repository:

```text
https://github.com/IMjaeyongpark/deploy-history-manifest.git
```

배포 설정은 manifest 레포에서 관리합니다. 이 앱 레포에는 Kubernetes manifest, Helm chart, Argo CD Application을 중복으로 두지 않습니다.
