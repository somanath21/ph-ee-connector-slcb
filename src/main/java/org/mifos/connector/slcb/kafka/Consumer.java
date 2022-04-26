package org.mifos.connector.slcb.kafka;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    private final ProducerTemplate producerTemplate;

    public Consumer(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @KafkaListener(topics = "${kafka.topic.slcb.name}", groupId = "group_id")
    public void listenTopicSlcb(String message) {
        // todo implementation

        // step 1: Parse the document

        // step 2: Convert document in SLCB format

        // step 3: Call the SLCB camel route with proper exchange variable set
    }

}
