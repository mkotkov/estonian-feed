package com.estonianfeed.repository;

import com.estonianfeed.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    boolean existsByUrl(String url);

    List<Job> findBySentFalseOrderByPublishedAtDesc();
}