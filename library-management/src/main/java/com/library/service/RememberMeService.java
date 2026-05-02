package com.library.service;
import com.library.model.RememberMeToken;
import com.library.model.User;
import com.library.repository.RememberMeRepository;
import com.library.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RememberMeService {

	@Autowired
	private RememberMeRepository rememberMeRepository;

    @Autowired
    private UserRepository userRepository;

	public RememberMeToken findByToken(String token) {
		return rememberMeRepository.findByToken(token);
	}

	@Transactional
	public void save(RememberMeToken rmt, Long userId) {
        User userReference = userRepository.getReferenceById(userId);
        
        rmt.setUser(userReference);
        
        rememberMeRepository.save(rmt);
	}

	@Transactional
	public void removeToken(String token) {
		rememberMeRepository.deleteByToken(token);
	}
}