package com.revpasswordmanager_p2.app.repository;

import com.revpasswordmanager_p2.app.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {

    List<SecurityQuestion> findByUserId(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
