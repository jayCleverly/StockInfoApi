#!/bin/bash
set -e

# Config vars
IMAGE_NAME="stockinfoapp"

# Set flags
SKIP_BACKEND=false
SKIP_FRONTEND=false
for arg in "$@"; do
  [[ "$arg" == "--skip-backend" ]] && SKIP_BACKEND=true
  [[ "$arg" == "--skip-frontend" ]] && SKIP_FRONTEND=true
done

# todo: Deploy cloudformation template

# Build and deploy backend
if [ "$SKIP_BACKEND" = false ]; then
    ./cicd/build-backend.sh $IMAGE_NAME

    echo "Deploying backend application..."
    # todo: Upload to ECR
    # todo: Create new task definition in ECS
    # todo: Update ECS service
else
    echo "Skipping backend deployment"
fi

if [ "$SKIP_FRONTEND" = false ]; then
    ./cicd/build-frontend.sh

    echo "Deploying frontend application..."
    # todo: Sync frontend build to S3
    # todo: Invalidate CloudFront cache
else
    echo "Skipping frontend deployment"
fi

echo "Deployment complete!"
