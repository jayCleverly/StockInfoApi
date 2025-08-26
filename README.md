# StockInfoSite

## CI/CD Scripts
### Run Locally

Use the helper script to start both backend (Docker) and frontend (React dev server with hot reload):

```bash
./cicd/start-locally
```

* Backend runs in Docker on http://localhost:8080 (logs: docker logs -f stockinfoapptest)
* Frontend runs on http://localhost:3000

Stop with `docker stop stockinfoapptest` and `CTRL+C` in the frontend terminal.

### Deploy to AWS

Use the helper script to deploy backend (ECS) and frontend (S3 + CloudFront). You can optionally skip either service:

```bash
./cicd/deploy.sh [--skip-backend] [--skip-frontend]
```

* Cloudformation: Deploys infrastructure defined in cloudformation template
* Backend: builds spring application and updates ECS service to use the new docker image
* Frontend: builds React app and overwrites S3 files / Cloudfront cache

