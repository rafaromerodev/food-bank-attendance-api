package dev.rafaromero.attendance.security;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A staff account. Only the BCrypt hash of the password is stored. */
@Entity
@Table(name = "app_user")
public class AppUser {

    /** Front desk staff register people and check them in; coordinators also manage files, cards and reports. */
    public enum Role { FRONT_DESK, COORDINATOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String username, String passwordHash, Role role, Instant now) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = now;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
