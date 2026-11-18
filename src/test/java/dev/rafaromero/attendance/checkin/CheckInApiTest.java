package dev.rafaromero.attendance.checkin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import dev.rafaromero.attendance.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CheckInApiTest extends ApiTestSupport {

    @Test
    void unknownCardAsksForRegistration() throws Exception {
        checkIn("04A1B2C3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("UNKNOWN_CARD"))
                .andExpect(jsonPath("$.beneficiary").doesNotExist());
    }

    @Test
    void countsOneAttendancePerPersonAndDay() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");

        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("REGISTERED"))
                .andExpect(jsonPath("$.attendance.source").value("CARD"));
        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("ALREADY_ATTENDED_TODAY"));

        clock.advance(Duration.ofDays(1));
        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("REGISTERED"));

        mvc.perform(get("/api/beneficiaries/" + id + "/attendances"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void acceptsUidsInTheFormatsReadersProduce() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04 a1:b2-c3");

        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("REGISTERED"));
    }

    @Test
    void expiredFileNeedsStaffConfirmation() throws Exception {
        long id = createBeneficiary("12345678Z", "2026-10-01");
        assignCard(id, "04A1B2C3");

        checkIn("04A1B2C3")
                .andExpect(jsonPath("$.outcome").value("FILE_EXPIRED"))
                .andExpect(jsonPath("$.beneficiary.fileStatus").value("EXPIRED"))
                .andExpect(jsonPath("$.attendance").doesNotExist());

        postJson("/api/check-ins", "{\"cardUid\": \"04A1B2C3\", \"confirmExpired\": true}")
                .andExpect(jsonPath("$.outcome").value("REGISTERED"));
    }

    @Test
    void replacingALostCardKeepsHistoryAndFlagsTheOldCard() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");
        checkIn("04A1B2C3");

        postJson("/api/beneficiaries/" + id + "/cards/replacement", "{\"uid\": \"04D4E5F6\", \"reason\": \"LOST\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid").value("04D4E5F6"));

        clock.advance(Duration.ofDays(7));
        checkIn("04A1B2C3")
                .andExpect(jsonPath("$.outcome").value("DEACTIVATED_CARD"))
                .andExpect(jsonPath("$.beneficiary.id").value(id));
        checkIn("04D4E5F6").andExpect(jsonPath("$.outcome").value("REGISTERED"));

        mvc.perform(get("/api/beneficiaries/" + id + "/cards"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].deactivationReason").value("LOST"));
        mvc.perform(get("/api/beneficiaries/" + id + "/attendances"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void aCardCannotBelongToTwoPeople() throws Exception {
        long first = createBeneficiary("12345678Z", null);
        long second = createBeneficiary("X1234567L", null);
        assignCard(first, "04A1B2C3");

        postJson("/api/beneficiaries/" + second + "/cards", "{\"uid\": \"04A1B2C3\"}")
                .andExpect(status().isConflict());
        postJson("/api/beneficiaries/" + first + "/cards", "{\"uid\": \"04D4E5F6\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void deactivatedBeneficiaryIsNotAttended() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");
        mvc.perform(delete("/api/beneficiaries/" + id));

        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("INACTIVE_BENEFICIARY"));
        postJson("/api/check-ins/manual", "{\"beneficiaryId\": " + id + "}").andExpect(status().isConflict());
    }

    @Test
    void manualCheckInFollowsTheSameOncePerDayRule() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");

        postJson("/api/check-ins/manual", "{\"beneficiaryId\": " + id + "}")
                .andExpect(jsonPath("$.outcome").value("REGISTERED"))
                .andExpect(jsonPath("$.attendance.source").value("MANUAL"));
        postJson("/api/check-ins/manual", "{\"beneficiaryId\": " + id + "}")
                .andExpect(jsonPath("$.outcome").value("ALREADY_ATTENDED_TODAY"));
    }

    @Test
    void theDatabaseItselfRejectsASecondAttendanceOnTheSameDay() throws Exception {
        long id = createBeneficiary("12345678Z", null);
        String insert = "insert into attendance (beneficiary_id, attended_on, attended_at, source) "
                + "values (?, date '2026-11-17', now(), 'CARD')";
        jdbc.update(insert, id);

        assertThatThrownBy(() -> jdbc.update(insert, id)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidUidIsRejected() throws Exception {
        checkIn("not-a-uid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.uid").exists());
    }
}
