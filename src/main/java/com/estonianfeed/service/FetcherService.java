package com.estonianfeed.service;

import com.estonianfeed.model.Article;
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class FetcherService {

    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);

    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String HN_TOP_STORIES =
        "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String HN_ITEM =
        "https://hacker-news.firebaseio.com/v0/item/{id}.json";

    public FetcherService(ArticleRepository articleRepository, JobRepository jobRepository) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelay = 300000)
    public void fetchNews() {
        log.info("Fetching news from HackerNews...");
        try {
            int[] storyIds = restTemplate.getForObject(HN_TOP_STORIES, int[].class);
            if (storyIds == null) return;

            // Беремо тільки перші 10 новин
            for (int i = 0; i < Math.min(10, storyIds.length); i++) {
                Map item = restTemplate.getForObject(HN_ITEM, Map.class, storyIds[i]);
                if (item == null) continue;

                String url = (String) item.get("url");
                String title = (String) item.get("title");

                // Деякі пости не мають URL (це дискусії) — пропускаємо
                if (url == null || title == null) continue;

                if (!articleRepository.existsByUrl(url)) {
                    Long time = ((Number) item.get("time")).longValue();
                    LocalDateTime publishedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(time),
                        ZoneId.systemDefault()
                    );
                    Article article = new Article(title, url, "HackerNews", publishedAt);
                    articleRepository.save(article);
                    log.info("Saved: {}", title);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch HackerNews", e);
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void fetchJobs() {
        log.info("Fetching jobs from HackerNews...");
        try {
            int[] jobIds = restTemplate.getForObject(
                "https://hacker-news.firebaseio.com/v0/jobstories.json",
                int[].class
            );
            if (jobIds == null) return;

            for (int i = 0; i < Math.min(10, jobIds.length); i++) {
                Map item = restTemplate.getForObject(HN_ITEM, Map.class, jobIds[i]);
                if (item == null) continue;

                String url = (String) item.get("url");
                String title = (String) item.get("title");
                if (title == null) continue;

                // Якщо немає URL — використовуємо посилання на пост HN
                if (url == null) {
                    url = "https://news.ycombinator.com/item?id=" + jobIds[i];
                }

                if (!jobRepository.existsByUrl(url)) {
                    Long time = ((Number) item.get("time")).longValue();
                    LocalDateTime publishedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(time),
                        ZoneId.systemDefault()
                    );
                    com.estonianfeed.model.Job job = new com.estonianfeed.model.Job(
                        title, "HackerNews", url, "Remote", publishedAt
                    );
                    jobRepository.save(job);
                    log.info("Saved job: {}", title);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch jobs", e);
        }
    }
}