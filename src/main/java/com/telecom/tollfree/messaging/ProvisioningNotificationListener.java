package com.telecom.tollfree.messaging;

import com.telecom.tollfree.domain.Customer;
import com.telecom.tollfree.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningNotificationListener.class);

    private final JavaMailSender mail;
    private final CustomerRepository customers;
    private final String from;

    public ProvisioningNotificationListener(JavaMailSender mail,
                                            CustomerRepository customers,
                                            @Value("${app.mail.from}") String from) {
        this.mail = mail;
        this.customers = customers;
        this.from = from;
    }

    @KafkaListener(topics = "tollfree.number.events", groupId = "notification-service")
    public void onEvent(NumberEvent event) {
        if (!"NumberReserved".equals(event.eventType())) {
            return;
        }
        if (event.customerId() == null) {
            return;
        }
        customers.findById(event.customerId()).ifPresent(customer -> sendMail(customer, event.number()));
    }

    private void sendMail(Customer customer, String number) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(customer.getEmail());
            message.setSubject("Your toll-free number " + number + " has been reserved");
            message.setText(
                    "Hi " + customer.getName() + ",\n\n" +
                    "Your toll-free number " + number + " has been successfully reserved.\n\n" +
                    "It will be active once an operator completes the activation step.\n\n" +
                    "Regards,\nToll-Free Provisioning Team"
            );
            mail.send(message);
            log.info("Provisioning notification sent to {} for number {}", customer.getEmail(), number);
        } catch (Exception e) {
            log.error("Failed to send provisioning notification to {} for number {}", customer.getEmail(), number, e);
        }
    }
}
