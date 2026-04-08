package com.example.app.service.impl;

import com.example.app.dto.AuthRequest;
import com.example.app.dto.AuthResponse;
import com.example.app.exception.AuthenticationFailedException;
import com.example.app.exception.RegistrationValidationException;
import com.example.app.model.AppUser;
import com.example.app.model.UserRole;
import com.example.app.repository.AppUserRepository;
import com.example.app.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;

    public AuthServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AuthResponse register(AuthRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername(), request.getRole());
        String fullName = trimToNull(request.getFullName());
        String employeeId = trimToNull(request.getEmployeeId());
        String mobileNumber = trimToNull(request.getMobileNumber());
        String emailId = trimToNull(request.getEmailId());
        String referredBy = trimToNull(request.getReferredBy());
        String hospitalName = trimToNull(request.getHospitalName());

        if (referredBy != null) {
            referredBy = referredBy.toUpperCase();
        }
        if ("SELF".equals(referredBy)) {
            hospitalName = null;
        }

        validateRegistration(request.getRole(), fullName, employeeId, mobileNumber, emailId, referredBy, hospitalName);

        appUserRepository.findByUsernameIgnoreCase(normalizedUsername).ifPresent(existing -> {
            throw new DataIntegrityViolationException("Username already exists: " + normalizedUsername);
        });

        if (emailId != null) {
            appUserRepository.findByEmailIdIgnoreCase(emailId).ifPresent(existing -> {
                throw new DataIntegrityViolationException("Email already exists: " + emailId);
            });
        }

        if (employeeId != null) {
            appUserRepository.findByEmployeeIdIgnoreCase(employeeId).ifPresent(existing -> {
                throw new DataIntegrityViolationException("Employee ID already exists: " + employeeId);
            });
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setFullName(fullName);
        user.setEmployeeId(employeeId);
        user.setMobileNumber(mobileNumber);
        user.setEmailId(emailId);
        user.setReferredBy(referredBy);
        user.setHospitalName(hospitalName);

        AppUser savedUser = appUserRepository.save(user);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                resolveDisplayName(savedUser),
                "Account created successfully"
        );
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        String loginValue = trimToNull(request.getUsername());
        if (loginValue == null) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        AppUser user = findMatchingUser(loginValue, request.getRole())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        if (user.getRole() != request.getRole()) {
            throw new AuthenticationFailedException("Selected role does not match this username");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                resolveDisplayName(user),
                "Login successful"
        );
    }

    private java.util.Optional<AppUser> findMatchingUser(String loginValue, UserRole role) {
        List<AppUser> candidates = appUserRepository.findByRole(role);
        String trimmedLogin = loginValue.trim();
        String lowerLogin = trimmedLogin.toLowerCase(Locale.ROOT);
        String emailPrefix = extractEmailPrefix(trimmedLogin).toLowerCase(Locale.ROOT);

        return candidates.stream()
                .filter(candidate -> matchesLogin(candidate, trimmedLogin, lowerLogin, emailPrefix, role))
                .findFirst();
    }

    private boolean matchesLogin(
            AppUser candidate,
            String loginValue,
            String lowerLogin,
            String lowerEmailPrefix,
            UserRole role
    ) {
        return matchesIgnoreCase(candidate.getUsername(), loginValue, lowerLogin)
                || matchesIgnoreCase(candidate.getEmailId(), loginValue, lowerLogin)
                || matchesIgnoreCase(candidate.getEmployeeId(), loginValue, lowerLogin)
                || matchesIgnoreCase(candidate.getFullName(), loginValue, lowerLogin)
                || (role == UserRole.USER && matchesIgnoreCase(candidate.getUsername(), lowerEmailPrefix, lowerEmailPrefix));
    }

    private boolean matchesIgnoreCase(String candidateValue, String loginValue, String lowerLogin) {
        if (candidateValue == null || candidateValue.isBlank()) {
            return false;
        }

        return candidateValue.trim().toLowerCase(Locale.ROOT).equals(lowerLogin)
                || candidateValue.trim().equalsIgnoreCase(loginValue);
    }

    private String extractEmailPrefix(String value) {
        int separatorIndex = value.indexOf('@');
        return separatorIndex >= 0 ? value.substring(0, separatorIndex) : value;
    }

    private String normalizeUsername(String username, UserRole role) {
        if (username == null) {
            return "";
        }

        String trimmedValue = username.trim();
        if (trimmedValue.isEmpty()) {
            return "";
        }

        if (role != UserRole.USER) {
            return trimmedValue;
        }

        int emailSeparatorIndex = trimmedValue.indexOf('@');
        return emailSeparatorIndex >= 0 ? trimmedValue.substring(0, emailSeparatorIndex) : trimmedValue;
    }

    private void validateRegistration(
            UserRole role,
            String fullName,
            String employeeId,
            String mobileNumber,
            String emailId,
            String referredBy,
            String hospitalName
    ) {
        if (role == UserRole.USER) {
            if (isBlank(referredBy)) {
                throw new RegistrationValidationException("Please select how the user was referred");
            }

            if (!"SELF".equalsIgnoreCase(referredBy) && !"HOSPITAL".equalsIgnoreCase(referredBy)) {
                throw new RegistrationValidationException("Referred by must be Self or Hospital");
            }

            if ("HOSPITAL".equalsIgnoreCase(referredBy) && isBlank(hospitalName)) {
                throw new RegistrationValidationException("Hospital name is required when referred by hospital");
            }

            return;
        }

        if (isBlank(fullName) || isBlank(employeeId) || isBlank(mobileNumber) || isBlank(emailId)) {
            throw new RegistrationValidationException(
                    "Name, employee ID, mobile number, and email ID are required for staff registration"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String resolveDisplayName(AppUser user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }

        return user.getUsername();
    }
}
