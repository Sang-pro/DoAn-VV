package com.trsang.doan2.repositories;

import com.trsang.doan2.entities.RfidTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRfidTagRepository extends JpaRepository<RfidTag, Long> {
    Optional<RfidTag> findByEpc(String epc);
}
