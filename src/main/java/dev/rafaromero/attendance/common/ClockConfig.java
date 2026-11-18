package dev.rafaromero.attendance.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Today" is a business concept here (one attendance per day, file expiry), so time comes
 * from an injected clock in the food bank's time zone. Tests replace it with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Atlantic/Canary"));
    }
}
