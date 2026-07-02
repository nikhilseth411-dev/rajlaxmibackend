package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.PhoneOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallenge, Long> {
    Optional<PhoneOtpChallenge> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
