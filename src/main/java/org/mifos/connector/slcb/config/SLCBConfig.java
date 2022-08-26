package org.mifos.connector.slcb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SLCBConfig {

    @Value("${slcb.auth.host}")
    public String authHost;

    @Value("${slcb.auth.username}")
    public String username;

    @Value("${slcb.auth.password}")
    public String password;

    @Value("${slcb.auth.auth-endpoint}")
    public String authEndpoint;

    @Value("${slcb.api.host}")
    public String apiHost;

    @Value("${slcb.api.transaction-request-endpoint}")
    public String transferRequestEndpoint;

    @Value("${slcb.api.reconciliation-endpoint}")
    public String reconciliationEndpoint;

    @Value("${slcb.api.account-balance-endpoint}")
    public String accountBalanceEndpoint;

    @Value("${slcb.signature.key}")
    public String signatureKey;

    @Value("${slcb.account.number}")
    public String sourceAccount;

    @Value("${slcb.account.type}")
    public int accountType;

    @Value("${slcb.institutionCode}")
    public String institutionCode;

    @Value("${config.reconciliation.enable}")
    public boolean isReconciliationEnabled;

    @Value("${config.date-format}")
    public String dateFormat;

    public String authUrl;
    public String transactionRequestUrl;
    public String reconciliationUrl;
    public String accountBalanceUrl;

    @PostConstruct
    private void setup() {
        authUrl = authHost + authEndpoint;
        transactionRequestUrl = apiHost + transferRequestEndpoint;
        reconciliationUrl = apiHost + reconciliationEndpoint;
        accountBalanceUrl = apiHost + accountBalanceEndpoint;
    }


}
