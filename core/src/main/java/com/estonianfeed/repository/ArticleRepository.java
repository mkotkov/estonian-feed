package com.estonianfeed.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.estonianfeed.model.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    boolean existsByUrl(String url);
    List<Article> findBySentFalseOrderByPublishedAtDesc();
    List<Article> findByNotifiedFalseOrderByPublishedAtDesc();
    List<Article> findByNotifiedFalseAndPublishedAtAfterOrderByPublishedAtDesc(LocalDateTime after);
    List<Article> findBySentFalseAndSourceIdInOrderByPublishedAtDesc(List<String> sourceIds);
}