package org.mifos.connector.slcb.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.mifos.connector.slcb.dto.Transaction;
import org.mifos.connector.slcb.zeebe.ZeebeProcessStarter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    private final ProducerTemplate producerTemplate;

    private final ObjectMapper objectMapper;

    private final ZeebeProcessStarter zeebeProcessStarter;

    public Consumer(ProducerTemplate producerTemplate,
                    ObjectMapper objectMapper,
                    ZeebeProcessStarter zeebeProcessStarter) {
        this.producerTemplate = producerTemplate;
        this.objectMapper = objectMapper;
        this.zeebeProcessStarter = zeebeProcessStarter;
    }

    @KafkaListener(topics = "${kafka.topic.slcb.name}", groupId = "group_id")
    public void listenTopicSlcb(String message) throws JsonProcessingException {
        System.out.println("Received Message in topic SLCB and group group_id: " + message);
        Transaction transaction = objectMapper.readValue((String) message, Transaction.class);
        // todo implementation

        // step 1: Parse the document

        // step 2: Convert document in SLCB format

        // step 3: Call the SLCB camel route with proper exchange variable set
    }

}
