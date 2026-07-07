# Deploy History Backend

Kotlin + Spring Boot 단일 backend 애플리케이션입니다.

## Runtime Env

Kubernetes manifest에서 아래 값을 주입합니다.

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

Postgres Service 이름은 Helm release 기준 `deploy-history-postgres`입니다.

## Commands

```bash
./gradlew test
./gradlew bootJar
docker build -t deploy-history-backend:local .
```
