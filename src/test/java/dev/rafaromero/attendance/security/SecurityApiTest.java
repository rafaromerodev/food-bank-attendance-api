package dev.rafaromero.attendance.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import dev.rafaromero.attendance.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

class SecurityApiTest extends ApiTestSupport {

    @Autowired
    AppUserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @WithAnonymousUser
    void anonymousRequestsGetA401WithoutABrowserLoginPrompt() throws Exception {
        mvc.perform(get("/api/beneficiaries"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @WithAnonymousUser
    void healthAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void unknownPathsAreDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "FRONT_DESK")
    void frontDeskCanRegisterAndCheckIn() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");
        checkIn("04A1B2C3").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("REGISTERED"));
    }

    @Test
    @WithMockUser(roles = "FRONT_DESK")
    void frontDeskCannotPerformCoordinatorActions() throws Exception {
        long id = createBeneficiary("12345678Z", "2027-01-01");
        assignCard(id, "04A1B2C3");

        mvc.perform(delete("/api/beneficiaries/" + id)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
        mvc.perform(post("/api/beneficiaries/" + id + "/renewal").param("months", "6"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/beneficiaries/" + id + "/reactivation")).andExpect(status().isForbidden());
        postJson("/api/beneficiaries/" + id + "/cards/replacement", "{\"uid\": \"04D4E5F6\", \"reason\": \"LOST\"}")
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/summary").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void realCredentialsAreCheckedAgainstTheBcryptHash() throws Exception {
        users.save(new AppUser("desk", passwordEncoder.encode("correct-horse-battery"), AppUser.Role.FRONT_DESK,
                Instant.now()));
        assertThat(users.findByUsername("desk").orElseThrow().getPasswordHash()).startsWith("$2");

        mvc.perform(get("/api/beneficiaries").with(httpBasic("desk", "correct-horse-battery")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/beneficiaries").with(httpBasic("desk", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/reports/summary").param("from", "2026-11-01").param("to", "2026-11-30")
                        .with(httpBasic("desk", "correct-horse-battery")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void disabledAccountsCannotSignIn() throws Exception {
        users.save(new AppUser("former", passwordEncoder.encode("correct-horse-battery"), AppUser.Role.COORDINATOR,
                Instant.now()));
        jdbc.update("update app_user set enabled = false where username = 'former'");

        mvc.perform(get("/api/beneficiaries").with(httpBasic("former", "correct-horse-battery")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void bootstrapCreatesTheFirstCoordinatorOnlyOnAnEmptyDatabase() throws Exception {
        new BootstrapCoordinator(users, passwordEncoder, clock, "admin", "a-long-bootstrap-secret").run(null);
        new BootstrapCoordinator(users, passwordEncoder, clock, "second", "another-long-secret").run(null);

        assertThat(users.count()).isEqualTo(1);
        mvc.perform(get("/api/reports/summary").param("from", "2026-11-01").param("to", "2026-11-30")
                        .with(httpBasic("admin", "a-long-bootstrap-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void bootstrapRejectsAShortPassword() {
        assertThatThrownBy(() -> new BootstrapCoordinator(users, passwordEncoder, clock, "admin", "short").run(null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    @WithAnonymousUser
    void crossSiteFormPostsCannotCheckIn() throws Exception {
        // A form on another site can only send form-encoded bodies and carries no credentials here.
        mvc.perform(post("/api/check-ins").contentType(MediaType.APPLICATION_FORM_URLENCODED).content("cardUid=04A1B2C3"))
                .andExpect(status().isUnauthorized());
    }
}
