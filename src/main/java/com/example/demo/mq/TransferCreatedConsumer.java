package com.example.demo.mq;

import com.example.demo.mq.event.TransferCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener( topic = "${app.mq.topic}",
        selectorExpression = "created",
        consumerGroup = "balance-transfer-consumer-created"
)
public class TransferCreatedConsumer implements RocketMQListener<TransferCreatedEvent> {
    @Override
    public void onMessage(TransferCreatedEvent msg) {
        log.info("[MQ] transfer created: {} -> {} amount={} id={}", msg.getFromUserId(), msg.getToUserId(), msg.getAmount(), msg.getTransferId());
    }
}

