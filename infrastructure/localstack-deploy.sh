#!/bin/bash
set -e

export AWS_ACCESS_KEY_ID=personal
export AWS_SECRET_ACCESS_KEY=personal
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

STACK_NAME="mercora"
TEMPLATE="./cdk.out/localstack.template.json"

echo "Deleting previous stack (if exists)..."

aws cloudformation delete-stack \
  --stack-name $STACK_NAME \
  --endpoint-url $AWS_ENDPOINT_URL || true

echo "Waiting stack deletion..."

aws cloudformation wait stack-delete-complete \
  --stack-name $STACK_NAME \
  --endpoint-url $AWS_ENDPOINT_URL || true

echo "Deploying stack..."

aws cloudformation deploy \
  --stack-name $STACK_NAME \
  --template-file $TEMPLATE \
  --endpoint-url $AWS_ENDPOINT_URL

echo "Fetching ALB DNS..."

aws elbv2 describe-load-balancers \
  --endpoint-url $AWS_ENDPOINT_URL \
  --query "LoadBalancers[0].DNSName" \
  --output text