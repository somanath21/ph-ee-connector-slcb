package org.mifos.connector.slcb.camel.config;

public class CamelProperties {

    private CamelProperties() {}

    public static final String CORRELATION_ID = "correlationId";
    public static final String TRANSACTION_FAILED = "transactionFailed";
    public static final String ERROR_INFORMATION = "errorInformation";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String SLCB_ACCESS_TOKEN = "slcbAccessToken";
    public static final String SLCB_CHANNEL_REQUEST = "slcbChannelRequest";
}
