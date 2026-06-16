package com.estonianfeed.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;

@Component
public class EstonianFeedBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;
    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;

    public EstonianFeedBot(
            @Value("${telegram.bot.token}") String botToken,
            ArticleRepository articleRepository,
            JobRepository jobRepository) {
        super(botToken);
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        switch (text) {
            case "/start" -> sendReply(chatId,
                "Привіт! Я Estonian Feed Bot 🇪🇪\n" +
                "Команди:\n" +
                "/news — останні новини\n" +
                "/jobs — останні вакансії");
            case "/news" -> sendNews(chatId);
            case "/jobs" -> sendJobs(chatId);
            default -> sendReply(chatId, "Невідома команда. Спробуй /news або /jobs");
        }
    }

    private void sendReply(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendNews(long chatId) {
        var articles = articleRepository.findBySentFalseOrderByPublishedAtDesc();
        if (articles.isEmpty()) {
            sendReply(chatId, "Поки немає нових новин.");
            return;
        }
        articles.stream().limit(5).forEach(article -> {
            String msg = "📰 " + article.getTitle() + "\n" + article.getUrl();
            sendReply(chatId, msg);
            article.setSent(true);
            articleRepository.save(article);
        });
    }

    private void sendJobs(long chatId) {
        var jobs = jobRepository.findBySentFalseOrderByPublishedAtDesc();
        if (jobs.isEmpty()) {
            sendReply(chatId, "Поки немає нових вакансій.");
            return;
        }
        jobs.stream().limit(5).forEach(job -> {
            String msg = "💼 " + job.getTitle() + "\n" + job.getUrl();
            sendReply(chatId, msg);
            job.setSent(true);
            jobRepository.save(job);
        });
    }
}