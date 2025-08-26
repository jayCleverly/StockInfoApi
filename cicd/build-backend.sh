#!/bin/bash
set -e

# Arguments
IMAGE=${1:-stockinfoapp}

echo "Building backend application..."

echo "Packaging spring application..."
mvn clean package

echo "Building docker image: $IMAGE..."
docker build -t $IMAGE .
