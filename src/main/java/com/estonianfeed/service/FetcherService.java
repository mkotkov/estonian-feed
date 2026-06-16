package com.estonianfeed.service;

import com.estonianfeed.model.Article;
import com.estonianfeed.model.Job;
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FetcherService {

    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);

    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;

    private static final List<String> NEWS_FEEDS = List.of(
        "https://err.ee/rss",
        "https://news.err.ee/rss",
        "https://news.postimees.ee/rss",
        "https://estonianworld.com/feed/",
        "https://gazeta.ee/feed/",
        "https://narvaleht.ee/feed/",
        "https://sonumitooja.ee/feed/",
        "https://kesknadal.ee/feed/",
        "https://online.le.ee/feed/"
    );

    private static final List<String> JOB_FEEDS = List.of(
        "https://feeds.feedburner.com/aripaev-rss"
    );

    public FetcherService(ArticleRepository articleRepository, JobRepository jobRepository) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelay = 300000)
    public void fetchNews() {
        log.info("Fetching Estonian news...");
        for (String feedUrl : NEWS_FEEDS) {
            try {
                SyndFeed feed = parseFeed(feedUrl);
                int saved = 0;
                for (SyndEntry entry : feed.getEntries()) {
                    String url = entry.getLink();
                    String title = entry.getTitle();
                    if (url == null || title == null) continue;
                    if (!articleRepository.existsByUrl(url)) {
                        Article article = new Article(
                            title.trim(),
                            url,
                            feed.getTitle(),
                            toLocalDateTime(entry)
                        );
                        articleRepository.save(article);
                        saved++;
                    }
                }
                log.info("Source: {} — saved {} new articles", feedUrl, saved);
            } catch (Exception e) {
                log.error("Failed to fetch: {}", feedUrl, e);
            }
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void fetchJobs() {
        log.info("Fetching Estonian jobs...");
        for (String feedUrl : JOB_FEEDS) {
            try {
                SyndFeed feed = parseFeed(feedUrl);
                int saved = 0;
                for (SyndEntry entry : feed.getEntries()) {
                    String url = entry.getLink();
                    String title = entry.getTitle();
                    if (url == null || title == null) continue;
                    if (!jobRepository.existsByUrl(url)) {
                        Job job = new Job(
                            title.trim(),
                            feed.getTitle(),
                            url,
                            "Estonia",
                            toLocalDateTime(entry)
                        );
                        jobRepository.save(job);
                        saved++;
                    }
                }
                log.info("Jobs source: {} — saved {} new jobs", feedUrl, saved);
            } catch (Exception e) {
                log.error("Failed to fetch jobs: {}", feedUrl, e);
            }
        }
    }

    private SyndFeed parseFeed(String feedUrl) throws Exception {
        URL url = URI.create(feedUrl).toURL();
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        return input.build(new XmlReader(url));
    }

    private LocalDateTime toLocalDateTime(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        }
        return LocalDateTime.now();
    }
}