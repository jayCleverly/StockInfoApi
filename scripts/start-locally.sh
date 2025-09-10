#!/bin/bash
set -e

echo "Validating application tests..."
mvn clean install

echo "Building docker containers..."
docker compose pull
docker compose -p stock-api up --force-recreate --build -d
docker image prune -f

echo "Sleeping to allow time for container startup..."
sleep 3

echo "Adding table to local docker instance..."
AWS_ACCESS_KEY_ID=dummyId AWS_SECRET_ACCESS_KEY=dummySecret \
aws dynamodb create-table \
  --table-name StockMetrics \
  --attribute-definitions \
      AttributeName=symbol,AttributeType=S \
      AttributeName=date,AttributeType=S \
  --key-schema \
      AttributeName=symbol,KeyType=HASH \
      AttributeName=date,KeyType=RANGE \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --endpoint-url http://localhost:8000 \
  --region eu-west-2 \
  --output text \
  --no-cli-pager

echo "Application running locally, ready for use!"
