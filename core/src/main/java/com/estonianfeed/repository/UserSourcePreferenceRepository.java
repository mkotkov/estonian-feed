package com.estonianfeed.repository;

import com.estonianfeed.model.UserSourcePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserSourcePreferenceRepository extends JpaRepository<UserSourcePreference, Long> {

    List<UserSourcePreference> findByChatId(Long chatId);

    boolean existsByChatIdAndSourceId(Long chatId, String sourceId);

    @Transactional
    @Modifying
    void deleteByChatIdAndSourceId(Long chatId, String sourceId);

    @Transactional
    @Modifying
    void deleteByChatId(Long chatId);
}