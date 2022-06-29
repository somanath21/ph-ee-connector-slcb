package org.mifos.connector.slcb.camel.routes.transfer;

import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.connector.slcb.camel.config.CamelProperties.ZEEBE_JOB_KEY;
import static org.mifos.connector.slcb.camel.config.CamelProperties.ZEEBE_VARIABLES;

@Component
public class ZeebeCompleteCommandPublisher implements Processor {

    private final ZeebeClient zeebeClient;

    public ZeebeCompleteCommandPublisher(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> variables = (Map<String, Object>) exchange.getProperty(ZEEBE_VARIABLES);
        Long jobKey = exchange.getProperty(ZEEBE_JOB_KEY, Long.class);

        zeebeClient.newCompleteCommand(jobKey)
                .variables(variables)
                .send()
                .join();
    }
}
