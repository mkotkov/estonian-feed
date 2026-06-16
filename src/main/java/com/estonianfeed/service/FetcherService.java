package com.estonianfeed.service;

import com.estonianfeed.bot.EstonianFeedBot;
import com.estonianfeed.model.Article;
import com.estonianfeed.model.Job;
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;
import com.estonianfeed.repository.UserSubscriptionRepository;
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
import java.util.Map;

@Service
public class FetcherService {

    private static final Logger log = LoggerFactory.getLogger(FetcherService.class);

    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final EstonianFeedBot bot;

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

    public FetcherService(
            ArticleRepository articleRepository,
            JobRepository jobRepository,
            UserSubscriptionRepository subscriptionRepository,
            EstonianFeedBot bot) {
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.bot = bot;
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

                        if (entry.getDescription() != null) {
                            article.setDescription(entry.getDescription().getValue());
                        }

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

    @Scheduled(fixedDelay = 300000)
    public void notifySubscribers() {
        var newArticles = articleRepository.findByNotifiedFalseOrderByPublishedAtDesc();
        if (newArticles.isEmpty()) return;

        var allSubscriptions = subscriptionRepository.findAll();
        if (allSubscriptions.isEmpty()) return;

        // Групуємо підписки по chatId
        Map<Long, List<String>> subsByChatId = new java.util.HashMap<>();
        for (var sub : allSubscriptions) {
            subsByChatId
                .computeIfAbsent(sub.getChatId(), k -> new java.util.ArrayList<>())
                .add(sub.getKeyword());
        }

        // Для кожного користувача шукаємо відповідні статті
        for (var entry : subsByChatId.entrySet()) {
            long chatId = entry.getKey();
            List<String> keywords = entry.getValue();

            for (var article : newArticles) {
                boolean matches = keywords.stream().anyMatch(keyword -> {
                    String title = article.getTitle().toLowerCase();
                    String desc = article.getDescription() != null
                        ? article.getDescription().toLowerCase()
                        : "";
                    return title.contains(keyword) || desc.contains(keyword);
                });

                if (matches) {
                    String msg = "🔔 Новина за твоєю підпискою:\n\n" +
                        "📰 " + article.getTitle() + "\n" +
                        article.getUrl();
                    bot.sendNotification(chatId, msg);
                }
            }
        }

        // Позначаємо статті як відправлені
        newArticles.forEach(a -> a.setNotified(true));
        articleRepository.saveAll(newArticles);
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