package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.util.PasswordUtil;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

@Stateless
public class UserService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private SecurityService securityService;

    public List<User> listUsers(UserRole caller) {
        securityService.requireAdmin(caller);
        return userRepository.findAllOrdered();
    }

    public User createUser(UserRole caller, String username, String password, UserRole role, boolean enabled) {
        securityService.requireAdmin(caller);
        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(password, "Password");
        if (role == null) {
            throw new BusinessException("Role is required.");
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new BusinessException("Username already exists.");
        }
        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(PasswordUtil.hash(password));
        u.setRole(role);
        u.setEnabled(enabled);
        return userRepository.save(u);
    }

    public User updateUser(UserRole caller, Long id, UserRole role, boolean enabled) {
        securityService.requireAdmin(caller);
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException("User not found."));
        if (role != null) {
            u.setRole(role);
        }
        u.setEnabled(enabled);
        return userRepository.save(u);
    }

    public void resetPassword(UserRole caller, Long id, String newPassword) {
        securityService.requireAdmin(caller);
        ValidationUtil.requireNonBlank(newPassword, "Password");
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException("User not found."));
        u.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(u);
    }
}
