package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.Status;
import org.mifos.connector.slcb.dto.Transaction;
import org.mifos.connector.slcb.file.FileTransferService;
import org.mifos.connector.slcb.utils.CsvUtils;
import org.mifos.connector.slcb.utils.SLCBUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.connector.slcb.camel.config.CamelProperties.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.TRANSFER_FAILED;

@Component
public class TransferRoutes extends BaseSLCBRouteBuilder {

    private final TransferResponseProcessor transferResponseProcessor;

    private final FileTransferService fileTransferService;


    public TransferRoutes(TransferResponseProcessor transferResponseProcessor, @Qualifier("awsStorage") FileTransferService fileTransferService) {
        this.transferResponseProcessor = transferResponseProcessor;
        this.fileTransferService = fileTransferService;
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
                .to("direct:transaction-response-handler")
                .marshal().json(JsonLibrary.Jackson)
                .setBody(exchange -> exchange.getIn().getBody());

        /*
         * Route to handle async API responses
         */
        from("direct:transaction-response-handler")
                .id("transaction-response-handler")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Transaction request successful")
                .unmarshal().json(JsonLibrary.Jackson, PaymentRequestDTO.class)
                .process((exchange) -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getIn().getBody(PaymentRequestDTO.class);
                    exchange.setProperty(SLCB_TRANSACTION_RESPONSE, paymentRequestDTO);
                    logger.info("Status: " + paymentRequestDTO.getStatus());
                }
                )
                .to("direct:update-status")
                .to("direct:update-file")
                .to("direct:upload-file")
                .setProperty(TRANSFER_FAILED, constant(false))
                .otherwise()
                .log(LoggingLevel.ERROR, "Transaction request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, exchange.getProperty(CORRELATION_ID)); // TODO: Improve this
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                })
                .setProperty(TRANSFER_FAILED, constant(true));

        /*
         * Calls SLCB API to commit transaction
         */
        getBaseAuthDefinitionBuilder("direct:commit-transaction", HttpRequestMethod.POST)
                /*.process(exchange -> {
                    String prop = exchange.getProperty(SLCB_CHANNEL_REQUEST, String.class);
                    logger.info("Properties: " + prop);
                    PaymentRequestDTO slcbChannelRequestBody = objectMapper.readValue(
                            prop, PaymentRequestDTO.class);
                    exchange.setProperty(SLCB_CHANNEL_REQUEST, slcbChannelRequestBody);
                })*/
                .setBody(exchange -> exchange.getProperty(SLCB_CHANNEL_REQUEST))
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, "Transaction Request Body: ${body}")
                .toD(slcbConfig.transactionRequestUrl + "?bridgeEndpoint=true&throwExceptionOnFailure=false");

        from("direct:upload-to-s3")
                .id("upload-to-s3")
                .log(LoggingLevel.INFO, "Uploading to S3 route started.")
                .process(exchange -> {
                    PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                            PaymentRequestDTO.class);
                    List<GSMATransaction> transactionList = SLCBUtils.convertPaymentRequestDto(paymentRequestDTO);
                    File csvFile = CsvUtils.createCSVFile(transactionList, GSMATransaction.class);
                    //String fileName = fileTransferService.uploadFile(csvFile);
                    //logger.info("Uploaded CSV in S3 with name: " + fileName);
                });

        from("direct:update-status")
                .id("direct:update-status")
                .log("Starting route direct:update-status")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    PaymentRequestDTO paymentRequestDTO = exchange.getProperty(SLCB_TRANSACTION_RESPONSE,
                            PaymentRequestDTO.class);
                    Status status = paymentRequestDTO.getStatus();
                    exchange.setProperty(STATUS_CODE, status.code);
                    exchange.setProperty(STATUS_DESCRIPTION, status.getDescription());

                    Map<Integer, Integer> idIndexMap = new HashMap<>();
                    for (int i = 0; i < transactionList.size(); i++) {
                        idIndexMap.put(transactionList.get(i).getId(), i);
                    }

                    paymentRequestDTO.getPayees().forEach(payee -> {
                        int index = idIndexMap.get(payee.getExternalTransactionId());
                        transactionList.get(index).setStatus(payee.getStatusMessage());
                    });

                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                    exchange.setProperty(OVERRIDE_HEADER, true);
                });

    }
}
