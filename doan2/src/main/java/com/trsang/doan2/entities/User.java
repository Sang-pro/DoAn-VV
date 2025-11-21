package com.trsang.doan2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "username")
        })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "phone_number", length = 15, unique = true)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    // updatedAt must be updatable so @PreUpdate can change it
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant updatedAt;

    @Transient
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Mqtt mqtt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SensorData> sensorData = new HashSet<>();

    // Ensure only a single @PrePersist method exists: consolidate initialization logic here.
    @PrePersist
    protected void initBeforePersist() {
        // timestamps
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        // active flag
        if (!isActive) {
            isActive = true;
        }
    }

}
