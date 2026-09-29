output "api_url" {
  description = "Base URL of the HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "orders_table" {
  value = aws_dynamodb_table.orders.name
}

output "orders_queue_url" {
  value = aws_sqs_queue.orders.url
}

output "orders_dlq_url" {
  value = aws_sqs_queue.orders_dlq.url
}
