package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.User;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.util.PasswordUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.Optional;

@Stateless
public class AuthService {

    @Inject
    private UserRepository userRepository;

    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException("Username and password are required.");
        }
        Optional<User> opt = userRepository.findByUsername(username.trim());
        if (opt.isEmpty()) {
            throw new BusinessException("Invalid username or password.");
        }
        User u = opt.get();
        if (!u.isEnabled()) {
            throw new BusinessException("This account is disabled.");
        }
        if (!PasswordUtil.matches(password, u.getPassword())) {
            throw new BusinessException("Invalid username or password.");
        }
        return u;
    }
}
