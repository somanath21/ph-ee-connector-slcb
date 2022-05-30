package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.springframework.stereotype.Component;
import java.util.UUID;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;

@Component
public class TransferRoutes extends BaseSLCBRouteBuilder {

    private final TransferResponseProcessor transferResponseProcessor;

    public TransferRoutes(TransferResponseProcessor transferResponseProcessor) {
        this.transferResponseProcessor = transferResponseProcessor;
    }

    @Override
    public void configure() {

        from("rest:post:test/transferRequest")
                .process(exchange -> exchange.setProperty(SLCB_CHANNEL_REQUEST, exchange.getIn().getBody(String.class)))
                .to("direct:transfer-route")
                .setBody(exchange -> exchange.getIn().getBody(String.class));

        /*
         * Base route for transactions
         */
        from("direct:transfer-route")
                .id("transfer-route")
                .log(LoggingLevel.INFO, "Transfer route started")
                .to("direct:get-access-token")
                .log(LoggingLevel.INFO, "Got access token, moving on")
                .log(LoggingLevel.INFO, "Moving on to API call")
                .to("direct:commit-transaction")
                .log(LoggingLevel.INFO, "Status: ${header.CamelHttpResponseCode}")
                .log(LoggingLevel.INFO, "Transaction API response: ${body}")
                .to("direct:transaction-response-handler");

        /*
         * Route to handle async API responses
         */
        from("direct:transaction-response-handler")
                .id("transaction-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Transaction request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process(exchange ->
                        logger.info("Status: " + exchange.getIn().getBody(PaymentRequestDTO.class).getStatus()))
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                })
                .setProperty(TRANSACTION_FAILED, constant(true))
                .process(transferResponseProcessor);

        /*
         * Calls SLCB API to commit transaction
         */
        getBaseAuthDefinitionBuilder("direct:commit-transaction", HttpRequestMethod.POST)
                .process(exchange -> {
                    String signedBody = SecurityUtils.signContent(UUID.randomUUID().toString(), slcbConfig.signatureKey);
                    String prop = exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class);
                    logger.info("Properties: " + prop);
                    PaymentRequestDTO slcbChannelRequestBody = objectMapper.readValue(
                            prop, PaymentRequestDTO.class);
                    slcbChannelRequestBody.setAuthorizationCode(signedBody);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, slcbChannelRequestBody);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.transactionRequestUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

    }
}
