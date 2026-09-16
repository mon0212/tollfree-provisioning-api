package com.telecom.tollfree.config;

import com.telecom.tollfree.messaging.NumberEvent;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
	@Bean
	NewTopic numberEventsTopic() {
		return new NewTopic("tollfree.number.events", 3, (short) 1);
	}

	@Bean
	NewTopic deadLetterTopic() {
		return new NewTopic("tollfree.number.events.DLT", 3, (short) 1);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, NumberEvent> kafkaListenerContainerFactory(
			ConsumerFactory<String, NumberEvent> cf, KafkaTemplate<Object, Object> template) {
		var f = new ConcurrentKafkaListenerContainerFactory<String, NumberEvent>();
		f.setConsumerFactory(cf);
		f.setCommonErrorHandler(new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template,
				(r, e) -> new TopicPartition(r.topic() + ".DLT", r.partition())), new FixedBackOff(1000, 3)));
		return f;
	}
}
