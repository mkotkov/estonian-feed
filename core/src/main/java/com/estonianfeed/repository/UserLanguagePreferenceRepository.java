package com.estonianfeed.repository;

import com.estonianfeed.model.UserLanguagePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLanguagePreferenceRepository extends JpaRepository<UserLanguagePreference, Long> {

    Optional<UserLanguagePreference> findByChatId(Long chatId);
}