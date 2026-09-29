package com.sindhu.pipeline;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQS → DynamoDB.
 * <ul>
 *   <li><b>Partial batch failures:</b> only the messages that failed are retried, not the whole batch.</li>
 *   <li><b>Idempotent writes:</b> SQS delivers at least once, so a conditional put ignores duplicates.</li>
 *   <li>Messages that fail 3 times move to a dead-letter queue (configured in Terraform).</li>
 * </ul>
 */
public class OrderProcessorHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public OrderProcessorHandler() {
        this(DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).build(), System.getenv("TABLE_NAME"));
    }

    OrderProcessorHandler(DynamoDbClient dynamo, String tableName) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                Order order = Json.MAPPER.readValue(message.getBody(), Order.class);
                save(order);
                context.getLogger().log("Stored order " + order.orderId());
            } catch (ConditionalCheckFailedException duplicate) {
                context.getLogger().log("Skipping duplicate message " + message.getMessageId());
            } catch (Exception e) {
                context.getLogger().log("Failed message " + message.getMessageId() + ": " + e.getMessage());
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }

        return new SQSBatchResponse(failures);
    }

    private void save(Order order) {
        dynamo.putItem(put -> put
                .tableName(tableName)
                .item(Map.of(
                        "orderId", AttributeValue.fromS(order.orderId()),
                        "customerId", AttributeValue.fromS(order.customerId()),
                        "product", AttributeValue.fromS(order.product()),
                        "quantity", AttributeValue.fromN(Integer.toString(order.quantity())),
                        "unitPrice", AttributeValue.fromN(order.unitPrice().toPlainString()),
                        "total", AttributeValue.fromN(order.total().toPlainString()),
                        "createdAt", AttributeValue.fromS(order.createdAt()),
                        "status", AttributeValue.fromS("RECEIVED")))
                .conditionExpression("attribute_not_exists(orderId)"));
    }
}
