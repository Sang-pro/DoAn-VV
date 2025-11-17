package com.trsang.doan2.repositories;

import org.springframework.stereotype.Repository;

import com.trsang.doan2.entities.User;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingOrEmailContainingOrFirstNameContainingOrLastNameContaining(String username, String email, String firstName, String lastName, Pageable pageable);
    Page<User> findByIsActive(boolean isActive, Pageable pageable);
}
