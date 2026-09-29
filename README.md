# serverless-order-pipeline

An event-driven order pipeline on **AWS**, written in **Java 21**. The whole stack is deployed with **Terraform**.

```
client ─POST /orders─▶ API Gateway ─▶ λ OrderIngestHandler ─▶ SQS orders ─▶ λ OrderProcessorHandler ─▶ DynamoDB
          (HTTP API)                  validate + enqueue        │            batch, idempotent write
                                      returns 202               └─ after 3 failures ─▶ SQS orders-dlq
```

## What it shows

- **Asynchronous, decoupled design**: the API only validates and enqueues, so it stays fast when the database is slow
- **Partial batch failure reporting** (`ReportBatchItemFailures`): only the failed messages in an SQS batch are retried
- **Idempotent consumer**: a DynamoDB conditional write (`attribute_not_exists`) makes SQS's at-least-once delivery safe
- A **dead-letter queue** for messages that keep failing
- **Least-privilege IAM**: each function gets its own role with only the permissions it needs
- Lambda cold-start care: SDK clients are created once per container, and the lightweight `url-connection-client` replaces the Apache HTTP client
- **Infrastructure as code** for everything: API, functions, queues, table and IAM
- Unit tests with JUnit 5

## Tech stack

Java 21 · AWS Lambda · API Gateway (HTTP API) · SQS · DynamoDB · IAM · AWS SDK for Java v2 · Terraform · Maven · JUnit 5

## Deploy it

Prerequisites: an AWS account, configured AWS credentials, Terraform 1.5 or later, and Maven.

```bash
# 1. Build and test the Lambda jar
cd lambda && mvn clean package && cd ..

# 2. Deploy
cd terraform
terraform init
terraform apply

# 3. Place an order
curl -X POST "$(terraform output -raw api_url)/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C-100","product":"Brake Pads","quantity":2,"unitPrice":49.99}'
# → {"orderId":"…","status":"QUEUED"}
```

The order shows up in the `order-pipeline-orders` DynamoDB table a moment later.

**Clean up** so you are not billed: `terraform destroy`

## Project layout

```
lambda/      Java handlers, model and tests (Maven; builds a shaded jar)
terraform/   API Gateway, Lambda, SQS, DynamoDB, IAM
```

## Next steps

- Turn on Lambda SnapStart to cut Java cold starts
- Add a CloudWatch alarm on DLQ depth
- Add a GitHub Actions workflow that runs `mvn verify` and `terraform plan` on every pull request
