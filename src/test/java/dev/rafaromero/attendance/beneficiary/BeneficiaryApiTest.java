package dev.rafaromero.attendance.beneficiary;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.rafaromero.attendance.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BeneficiaryApiTest extends ApiTestSupport {

    @Test
    void registersABeneficiaryWithANormalisedDocument() throws Exception {
        postJson("/api/beneficiaries", """
                {"document": " 12.345.678-z ", "firstName": " Ana ", "lastName": "Pérez",
                 "phone": "612345678", "fileExpiresOn": "2027-06-01"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/beneficiaries/1"))
                .andExpect(jsonPath("$.document").value("12345678Z"))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.fileStatus").value("VALID"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsAWrongCheckLetterAndSuggestsTheRightOne() throws Exception {
        postJson("/api/beneficiaries", """
                {"document": "12345678A", "firstName": "Ana", "lastName": "Pérez", "phone": "612345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.document").value(containsString("12345678Z")));
    }

    @Test
    void listsEveryInvalidField() throws Exception {
        postJson("/api/beneficiaries", """
                {"document": "12345678Z", "firstName": "", "lastName": "Pérez", "phone": "abc",
                 "householdMinors": -1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.phone").value("must be a phone number"))
                .andExpect(jsonPath("$.errors.householdMinors").exists());
    }

    @Test
    void rejectsADuplicateDocument() throws Exception {
        createBeneficiary("12345678Z", null);

        postJson("/api/beneficiaries", """
                {"document": "12345678z", "firstName": "Otra", "lastName": "Persona", "phone": "622345678"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already exists")));
    }

    @Test
    void correctingTheDocumentKeepsTheAttendanceHistory() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");
        checkIn("04A1B2C3").andExpect(jsonPath("$.outcome").value("REGISTERED"));

        mvc.perform(put("/api/beneficiaries/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"document": "X1234567L", "firstName": "Ana", "lastName": "Pérez Díaz", "phone": "612345678"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("X1234567L"));

        mvc.perform(get("/api/beneficiaries/" + id + "/attendances"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchesByNameDocumentOrFileNumber() throws Exception {
        createBeneficiary("12345678Z", null);

        for (String query : new String[] {"pérez", "345678", "exp-1"}) {
            mvc.perform(get("/api/beneficiaries").param("query", query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }
        mvc.perform(get("/api/beneficiaries").param("query", "nobody"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void renewalExtendsFromTheCurrentExpiryWhenStillValid() throws Exception {
        long valid = createBeneficiary("12345678Z", "2027-01-31");
        long expired = createBeneficiary("X1234567L", "2026-01-01");

        mvc.perform(post("/api/beneficiaries/" + valid + "/renewal").param("months", "6"))
                .andExpect(jsonPath("$.fileExpiresOn").value("2027-07-31"));
        mvc.perform(post("/api/beneficiaries/" + expired + "/renewal").param("months", "12"))
                .andExpect(jsonPath("$.fileExpiresOn").value("2027-11-17"))
                .andExpect(jsonPath("$.fileStatus").value("VALID"));
        mvc.perform(post("/api/beneficiaries/" + valid + "/renewal").param("months", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsFilesAboutToExpire() throws Exception {
        long id = createBeneficiary("12345678Z", "2026-12-01");

        mvc.perform(get("/api/beneficiaries/" + id))
                .andExpect(jsonPath("$.fileStatus").value("EXPIRING_SOON"));
    }

    @Test
    void deactivationIsASoftDelete() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");
        checkIn("04A1B2C3");

        mvc.perform(delete("/api/beneficiaries/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/beneficiaries")).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/beneficiaries").param("active", "false"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/beneficiaries/" + id + "/attendances"))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/api/beneficiaries/" + id + "/reactivation"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unknownBeneficiaryIsA404ProblemDetail() throws Exception {
        mvc.perform(get("/api/beneficiaries/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Beneficiary 999 not found"));
    }
}
