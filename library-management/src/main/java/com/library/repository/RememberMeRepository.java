package com.library.repository;
import com.library.model.RememberMeToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RememberMeRepository extends JpaRepository<RememberMeToken, Long>{
    RememberMeToken findByToken(String token);
    RememberMeToken deleteByToken(String token);
}
