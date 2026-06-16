package com.estonianfeed.repository;

import com.estonianfeed.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    List<Article> findBySentFalseOrderByPublishedAtDesc();
}