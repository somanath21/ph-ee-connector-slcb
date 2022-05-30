package org.mifos.connector.slcb.camel.routes.transfer;

import org.apache.camel.builder.RouteBuilder;
import org.mifos.connector.slcb.zeebe.ZeebeProcessStarter;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TestRoutes extends RouteBuilder {

    private final ZeebeProcessStarter zeebeProcessStarter;

    public TestRoutes(ZeebeProcessStarter zeebeProcessStarter) {
        this.zeebeProcessStarter = zeebeProcessStarter;
    }

    @Override
    public void configure() throws Exception {
        from("rest:get:/start")
                .id("slcb-flow-start")
                .process(exchange -> {
                    try {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("transactionId", UUID.randomUUID());
                        zeebeProcessStarter.startZeebeWorkflow("SLCB", variables);
                    } catch (Exception e) {

                    }

                })
                .setBody(constant("Started"));
    }
}
