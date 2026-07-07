# Architecture

`deploy-history-app` contains application source code only.

```text
frontend -> backend -> PostgreSQL
```

CI/CD flow:

```text
deploy-history-app push
  -> Jenkins
  -> backend/frontend build and test
  -> Docker image push to Nexus
  -> deploy-history-manifest helm/values.yaml tag update
  -> Argo CD sync
  -> Kubernetes rollout
```

Kubernetes manifests, Helm chart, and Argo CD Application live in the separate `deploy-history-manifest` repository.
