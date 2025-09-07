# StockInfoApi

## CI/CD Scripts
### Start Locally

Note: Must have docker + aws cli available to use 

Use the helper script to start the spring app and dynamo db locally using docker:

```bash
./cicd/start-locally
```

* Spring app runs in Docker on http://localhost:8080
* Dynamo instance runs in Docker on http://localhost:8000
