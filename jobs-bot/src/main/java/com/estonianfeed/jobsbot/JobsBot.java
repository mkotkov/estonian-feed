package com.estonianfeed.jobsbot;

import com.estonianfeed.model.Job;
import com.estonianfeed.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class JobsBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(JobsBot.class);

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.channel.id}")
    private Long channelId;

    private final JobRepository jobRepository;

    public JobsBot(
            @Value("${telegram.bot.token}") String botToken,
            JobRepository jobRepository) {
        super(botToken);
        this.jobRepository = jobRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();

        switch (text) {
            case "/start" -> sendReply(chatId,
                "Привіт! Я Estonian Jobs Bot 💼\n\n" +
                "Команди:\n" +
                "/jobs — останні вакансії\n" +
                "/channel — посилання на канал вакансій");
            case "/jobs" -> sendJobs(chatId);
            case "/channel" -> sendReply(chatId,
                "Канал вакансій: t.me/eesti_job");
            default -> sendReply(chatId, "Спробуй /jobs або /start");
        }
    }

    private void sendJobs(long chatId) {
        List<Job> jobs = jobRepository.findBySentFalseOrderByPublishedAtDesc();
        if (jobs.isEmpty()) {
            sendReply(chatId, "Поки немає нових вакансій.");
            return;
        }
        jobs.stream().limit(5).forEach(job -> {
            String msg = "💼 " + job.getTitle() + "\n" +
                "🏢 " + job.getCompany() + "\n" +
                "🔗 " + job.getUrl();
            sendReply(chatId, msg);
            job.setSent(true);
            jobRepository.save(job);
        });
    }

    public void sendToChannel(Job job) {
        String msg = "💼 *" + job.getTitle() + "*\n" +
            "🏢 " + job.getCompany() + "\n" +
            "📍 " + job.getLocation() + "\n" +
            "🔗 " + job.getUrl();
        SendMessage message = new SendMessage();
        message.setChatId(channelId);
        message.setText(msg);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send to channel", e);
        }
    }

    public void sendReply(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }
}