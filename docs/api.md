# API

## Health

```http
GET /api/health
```

## Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "runtime-password"
}
```

## Current User

```http
GET /api/me
Authorization: Bearer <access-token>
```
