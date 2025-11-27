package com.trsang.doan2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "sensor_data")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    @Column(name = "is_tipping", nullable = false)
    private boolean isTipping = false;
    
    @Builder.Default
    @Column(name = "is_detected_human", nullable = false)
    private boolean isDetectedHuman = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double distance;

    @Column(name = "gas", nullable = false)
    private Integer gas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "satellites", nullable = false)
    private Integer satellites;

    @Column(name = "latitude", nullable = false)
    private Double latitude;
    
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
    
}
