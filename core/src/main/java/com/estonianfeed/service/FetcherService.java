package com.estonianfeed.service;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.estonianfeed.model.Article;
import com.estonianfeed.model.Job;
import com.estonianfeed.model.Source;
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;
import com.estonianfeed.repository.SourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import jakarta.annotation.PostConstruct;

@Service
public class FetcherService {

    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);

    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;
    private final SourceRepository sourceRepository;

    // sourceId -> URL. LinkedHashMap зберігає порядок вставки — корисно для /sources меню.
    private static final Map<String, String> NEWS_FEEDS = new LinkedHashMap<>() {{
        put("err", "https://err.ee/rss");
        put("err_news", "https://news.err.ee/rss");
        put("postimees", "https://news.postimees.ee/rss");
        put("estonianworld", "https://estonianworld.com/feed/");
        put("gazeta", "https://gazeta.ee/feed/");
        put("narvaleht", "https://narvaleht.ee/feed/");
        put("sonumitooja", "https://sonumitooja.ee/feed/");
        put("kesknadal", "https://kesknadal.ee/feed/");
        put("online_le", "https://online.le.ee/feed/");
    }};

    private static final Map<String, String> JOB_FEEDS = new LinkedHashMap<>() {{
        put("aripaev", "https://feeds.feedburner.com/aripaev-rss");
    }};

    public FetcherService(
            ArticleRepository articleRepository,
            JobRepository jobRepository,
            SourceRepository sourceRepository) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.sourceRepository = sourceRepository;
    }

    @Scheduled(fixedDelay = 300000)
    public void fetchNews() {
        log.info("Fetching Estonian news...");
        for (var entry : NEWS_FEEDS.entrySet()) {
            String sourceId = entry.getKey();
            String feedUrl = entry.getValue();
            try {
                SyndFeed feed = parseFeed(feedUrl);
                int saved = 0;
                for (SyndEntry e : feed.getEntries()) {
                    String url = e.getLink();
                    String title = e.getTitle();
                    if (url == null || title == null) continue;
                    if (!articleRepository.existsByUrl(url)) {
                        Article article = new Article(
                            title.trim(), url,
                            feed.getTitle(),
                            toLocalDateTime(e)
                        );
                        article.setSourceId(sourceId);
                        if (e.getDescription() != null) {
                            article.setDescription(e.getDescription().getValue());
                        }
                        articleRepository.save(article);
                        saved++;
                    }
                }
                log.info("Source: {} — saved {} new articles", sourceId, saved);
            } catch (Exception ex) {
                log.error("Failed to fetch: {}", feedUrl, ex);
            }
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void fetchJobs() {
        log.info("Fetching Estonian jobs...");
        for (var entry : JOB_FEEDS.entrySet()) {
            String sourceId = entry.getKey();
            String feedUrl = entry.getValue();
            try {
                SyndFeed feed = parseFeed(feedUrl);
                int saved = 0;
                for (SyndEntry e : feed.getEntries()) {
                    String url = e.getLink();
                    String title = e.getTitle();
                    if (url == null || title == null) continue;
                    if (!jobRepository.existsByUrl(url)) {
                        Job job = new Job(
                            title.trim(), feed.getTitle(),
                            url, "Estonia",
                            toLocalDateTime(e)
                        );
                        jobRepository.save(job);
                        saved++;
                    }
                }
                log.info("Jobs: {} — saved {} new jobs", sourceId, saved);
            } catch (Exception ex) {
                log.error("Failed to fetch jobs: {}", feedUrl, ex);
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

    @PostConstruct
    public void initSources() {
        saveSourceIfMissing("err", "ERR News (ET)", NEWS_FEEDS.get("err"), "ET");
        saveSourceIfMissing("err_news", "ERR News (EN)", NEWS_FEEDS.get("err_news"), "EN");
        saveSourceIfMissing("postimees", "Postimees", NEWS_FEEDS.get("postimees"), "ET");
        saveSourceIfMissing("estonianworld", "Estonian World", NEWS_FEEDS.get("estonianworld"), "EN");
        saveSourceIfMissing("gazeta", "Gazeta.ee", NEWS_FEEDS.get("gazeta"), "RU");
        saveSourceIfMissing("narvaleht", "Narva Leht", NEWS_FEEDS.get("narvaleht"), "RU");
        saveSourceIfMissing("sonumitooja", "Sõnumitooja", NEWS_FEEDS.get("sonumitooja"), "ET");
        saveSourceIfMissing("kesknadal", "Kesknädal", NEWS_FEEDS.get("kesknadal"), "ET");
        saveSourceIfMissing("online_le", "Õhtuleht", NEWS_FEEDS.get("online_le"), "ET");
        log.info("Sources initialized: {} total", sourceRepository.count());
    }

    private void saveSourceIfMissing(String id, String name, String url, String language) {
        if (!sourceRepository.existsById(id)) {
            sourceRepository.save(new Source(id, name, url, language));
        }
    }
}
