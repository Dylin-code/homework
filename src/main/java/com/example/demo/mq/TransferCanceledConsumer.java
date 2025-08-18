package com.example.demo.mq;

import com.example.demo.mq.event.TransferCanceledEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${app.mq.topic}",
        selectorExpression = "canceled",
        consumerGroup = "balance-transfer-consumer-canceled")
public class TransferCanceledConsumer implements RocketMQListener<TransferCanceledEvent> {
    @Override
    public void onMessage(TransferCanceledEvent msg) {
        log.info("[MQ] transfer canceled: id={}", msg.getTransferId());
    }
}
