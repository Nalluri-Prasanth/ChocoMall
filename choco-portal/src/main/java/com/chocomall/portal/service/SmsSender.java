package com.chocomall.portal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Port to an SMS provider. The default implementation only logs, so no message ever leaves the machine. */
public interface SmsSender {

    void send(String phone, String message);

    @Slf4j
    @Component
    class LoggingSmsSender implements SmsSender {

        @Override
        public void send(String phone, String message) {
            log.info("SMS to {}: [message withheld from logs]", mask(phone));
            log.debug("SMS to {}: {}", mask(phone), message);
        }

        private static String mask(String phone) {
            return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
        }
    }
}
