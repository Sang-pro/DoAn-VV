package com.trsang.doan2.repositories;

import com.trsang.doan2.entities.Mqtt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IMqttRepository extends JpaRepository<Mqtt, UUID> {
    List<Mqtt> findByIsActiveTrue();
    
    Optional<Mqtt> findByMqttUsername(String mqttUsername);
}
