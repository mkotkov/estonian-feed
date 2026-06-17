package com.estonianfeed.repository;

import com.estonianfeed.model.UserSubscription;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.lang.NonNull;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByChatId(Long chatId);

    boolean existsByChatIdAndKeyword(Long chatId, String keyword);

    @Transactional
    @Modifying
    void deleteByChatIdAndKeyword(Long chatId, String keyword);

    @NonNull
    List<UserSubscription> findAll();
}