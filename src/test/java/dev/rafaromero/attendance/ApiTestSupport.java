package dev.rafaromero.attendance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base class for API tests: the real application against PostgreSQL in a container,
 * with a clean database and a fixed "now" (17 Nov 2026, 10:00 in the Canary Islands) for every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiTestSupport.ClockConfig.class})
public abstract class ApiTestSupport {

    protected static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MutableClock clock;

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOW, ZoneId.of("Atlantic/Canary"));
        }
    }

    @BeforeEach
    void resetState() {
        jdbc.execute("truncate attendance, card, beneficiary restart identity cascade");
        clock.set(NOW);
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected long createBeneficiary(String document, String fileExpiresOn) throws Exception {
        String expiry = fileExpiresOn == null ? "null" : "\"" + fileExpiresOn + "\"";
        String body = postJson("/api/beneficiaries", """
                {"document": "%s", "firstName": "Ana", "lastName": "Pérez Díaz",
                 "phone": "612345678", "fileNumber": "EXP-1", "householdAdults": 2,
                 "householdMinors": 1, "fileExpiresOn": %s}
                """.formatted(document, expiry))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected void assignCard(long beneficiaryId, String uid) throws Exception {
        postJson("/api/beneficiaries/" + beneficiaryId + "/cards", "{\"uid\": \"" + uid + "\"}")
                .andExpect(status().isCreated());
    }

    protected ResultActions checkIn(String uid) throws Exception {
        return postJson("/api/check-ins", "{\"cardUid\": \"" + uid + "\"}");
    }
}
