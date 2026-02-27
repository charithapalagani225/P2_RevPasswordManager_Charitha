package com.revpasswordmanager_p2.app.repository;

import com.passwordmanager.app.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String purpose);

    @Modifying
    @Query("DELETE FROM VerificationCode vc WHERE vc.expiresAt < :now OR vc.used = true")
    void deleteExpiredAndUsed(@Param("now") LocalDateTime now);
}
