package com.trsang.doan2.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "esl_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EslTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mac_address", unique = true, nullable = false)
    private String macAddress;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "is_online")
    private Boolean isOnline;

    private String location;

    private String status;

    @OneToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("eslTag")
    private Product product;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "last_seen")
    private Instant lastSeen;
}
