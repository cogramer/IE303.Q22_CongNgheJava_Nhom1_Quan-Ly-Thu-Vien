package com.library.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.library.model.User;
import com.library.repository.UserRepository;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository; 
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public boolean checkExistedUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean checkExistedEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkPassword(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return true;
        } else {
            return false;
        }
    }

    public void addNewUser(String username, String password, String email, String fullName) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setRole(User.Role.READER); // mặc định role là READER
        newUser.setCreatedAt(java.time.LocalDateTime.now());
        userRepository.save(newUser);
    }
}
