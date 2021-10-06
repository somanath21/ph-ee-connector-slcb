package org.mifos.connector.slcb.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.mifos.connector.slcb.camel.config.CamelProperties.CORRELATION_ID;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_ACCESS_TOKEN;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_CHANNEL_REQUEST;
import static org.mifos.connector.slcb.camel.config.CamelProperties.TRANSACTION_FAILED;
import static org.mifos.connector.slcb.camel.config.CamelProperties.TRANSACTION_ID;

@Component
public class TransferRoutes extends RouteBuilder {

    @Autowired
    private TransferResponseProcessor transferResponseProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${slcb.api.host}")
    private String BaseURL;

    @Value("${slcb.signature.key}")
    private String signatureKey;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {

        /**
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

        /**
         * Route to handle async API responses
         */
        from("direct:transaction-response-handler")
                .id("transaction-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Transaction request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process(exchange -> {
                    logger.info("Status: " + exchange.getIn().getBody(PaymentRequestDTO.class).getStatus());
                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                })
                .setProperty(TRANSACTION_FAILED, constant(true))
                .process(transferResponseProcessor);

        /**
         * Calls SLCB API to commit transaction
         */
        from("direct:commit-transaction")
                .removeHeader("*")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Date", simple(ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty."+SLCB_ACCESS_TOKEN+"}"))
                .setHeader("Content-Type", constant("application/json"))
                .process(exchange -> {
                    // TODO: Verify the exact content to be signed from SLCB team.
                    String signedBody = SecurityUtils.signContent(exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class), signatureKey);
                    PaymentRequestDTO slcbChannelRequestBody = objectMapper.readValue(exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class), PaymentRequestDTO.class);
                    slcbChannelRequestBody.setAuthorizationCode(signedBody);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, slcbChannelRequestBody);
                })
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(BaseURL + "/PaymentRequest" + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

    }
}
