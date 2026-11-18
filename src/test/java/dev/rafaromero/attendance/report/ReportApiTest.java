package dev.rafaromero.attendance.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import dev.rafaromero.attendance.ApiTestSupport;
import org.junit.jupiter.api.Test;

class ReportApiTest extends ApiTestSupport {

    @Test
    void summarisesAPeriod() throws Exception {
        long ana = createBeneficiary("12345678Z", "2027-01-01");
        long luis = createBeneficiary("X1234567L", "2026-01-01"); // expired file
        assignCard(ana, "04A1B2C3");
        assignCard(luis, "04D4E5F6");

        checkIn("04A1B2C3");
        checkIn("04A1B2C3"); // same day: not counted twice
        postJson("/api/check-ins", "{\"cardUid\": \"04D4E5F6\", \"confirmExpired\": true}");
        clock.advance(Duration.ofDays(1));
        checkIn("04A1B2C3");

        mvc.perform(get("/api/reports/summary").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendances").value(3))
                .andExpect(jsonPath("$.peopleAttended").value(2))
                .andExpect(jsonPath("$.newBeneficiaries").value(2))
                .andExpect(jsonPath("$.activeWithExpiredFile").value(1));
    }

    @Test
    void rejectsAnInvertedPeriod() throws Exception {
        mvc.perform(get("/api/reports/summary").param("from", "2026-11-30").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }
}
