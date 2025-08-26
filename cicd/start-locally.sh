#!/bin/bash
set -e

# Config vars
IMAGE_NAME="stockinfoapplocal"

# Build and run backend
./cicd/build-backend.sh $IMAGE_NAME

echo "Starting backend service in docker: $IMAGE_NAME..."
docker rm -f $IMAGE_NAME
docker run -d --name $IMAGE_NAME -p 8080:8080 $IMAGE_NAME

# Build and run frontend
./cicd/build-frontend.sh

echo "Starting frontend service with hot reload"
# todo: start frontend development server
