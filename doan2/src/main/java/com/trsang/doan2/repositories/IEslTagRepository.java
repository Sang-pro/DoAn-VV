package com.trsang.doan2.repositories;

import com.trsang.doan2.entities.EslTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IEslTagRepository extends JpaRepository<EslTag, Long> {
    Optional<EslTag> findByMacAddress(String macAddress);
    Optional<EslTag> findByBookId(Long bookId);
    Optional<EslTag> findByLocation(String location);
}
