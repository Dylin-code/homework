package com.example.demo.mq.event;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferCreatedEvent {
    private String transferId;
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
}
