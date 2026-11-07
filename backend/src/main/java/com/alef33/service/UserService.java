package com.alef33.service;

import com.alef33.dto.UserRequest;
import com.alef33.exception.BusinessException;
import com.alef33.exception.InvalidCredentialsException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.AppUser;
import com.alef33.model.enums.Role;
import com.alef33.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Checks email + password at login. Same message in every case, so attackers learn nothing. */
    @Transactional(readOnly = true)
    public AppUser authenticate(String email, String rawPassword) {
        AppUser user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("This account is disabled");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));
    }

    @Transactional(readOnly = true)
    public List<AppUser> list() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional
    public AppUser create(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Email already used: " + request.email());
        }
        AppUser user = new AppUser();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        return userRepository.save(user);
    }

    /** Disable / re-enable an account (an admin can't disable their own account) */
    @Transactional
    public AppUser setEnabled(Long id, boolean enabled, String currentUserEmail) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!enabled && user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BusinessException("You cannot disable your own account");
        }
        user.setEnabled(enabled);
        return user;
    }

    /** Used at startup: creates the account only if the email doesn't exist yet */
    @Transactional
    public boolean createIfMissing(String email, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return false;
        }
        AppUser user = new AppUser();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        userRepository.save(user);
        return true;
    }
}
