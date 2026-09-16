package com.telecom.tollfree.messaging; import java.time.Instant; public record NumberEvent(String eventType,String number,String customerId,Instant occurredAt){}
