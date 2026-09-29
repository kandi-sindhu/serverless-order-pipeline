variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Prefix for all resource names"
  type        = string
  default     = "order-pipeline"
}

variable "lambda_jar" {
  description = "Path to the shaded Lambda jar built by Maven"
  type        = string
  default     = "../lambda/target/order-pipeline-lambda.jar"
}
