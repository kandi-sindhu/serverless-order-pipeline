package com.sindhu.pipeline;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;
import java.util.Map;

/**
 * POST /orders (API Gateway HTTP API) → validate → enqueue on SQS → 202 Accepted.
 * The API stays fast because the slower work happens asynchronously in {@link OrderProcessorHandler}.
 */
public class OrderIngestHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final SqsClient sqs;
    private final String queueUrl;

    /** Used by the Lambda runtime. The client is created once and reused across invocations. */
    public OrderIngestHandler() {
        this(SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).build(), System.getenv("QUEUE_URL"));
    }

    OrderIngestHandler(SqsClient sqs, String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        OrderRequest request;
        try {
            request = Json.MAPPER.readValue(event.getBody(), OrderRequest.class);
        } catch (Exception e) {
            return response(400, Map.of("error", "Request body must be valid JSON"));
        }

        List<String> errors = request == null ? List.of("Request body is required") : request.validate();
        if (!errors.isEmpty()) {
            return response(400, Map.of("errors", errors));
        }

        Order order = Order.from(request);
        sqs.sendMessage(message -> message.queueUrl(queueUrl).messageBody(Json.write(order)));
        context.getLogger().log("Queued order " + order.orderId());

        return response(202, Map.of("orderId", order.orderId(), "status", "QUEUED"));
    }

    private static APIGatewayV2HTTPResponse response(int status, Object body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(Json.write(body))
                .build();
    }
}
