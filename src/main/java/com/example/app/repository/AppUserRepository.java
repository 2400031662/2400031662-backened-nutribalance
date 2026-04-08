package com.example.app.repository;

import com.example.app.model.AppUser;
import com.example.app.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    Optional<AppUser> findByEmployeeIdIgnoreCase(String employeeId);

    Optional<AppUser> findByEmailIdIgnoreCase(String emailId);

    List<AppUser> findByRole(UserRole role);
}
