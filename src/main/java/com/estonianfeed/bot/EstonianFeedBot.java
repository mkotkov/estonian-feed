package com.estonianfeed.bot;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import com.estonianfeed.model.UserSubscription;
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.JobRepository;
import com.estonianfeed.repository.UserSubscriptionRepository;

@Component
public class EstonianFeedBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;
    private final ArticleRepository articleRepository;
    private final JobRepository jobRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final Map<Long, String> userStates = new java.util.concurrent.ConcurrentHashMap<>();

    public EstonianFeedBot(
            @Value("${telegram.bot.token}") String botToken,
            ArticleRepository articleRepository,
            JobRepository jobRepository,
            UserSubscriptionRepository subscriptionRepository) {
        super(botToken);
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Обробка натискання кнопок
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        String state = userStates.get(chatId);

        // Якщо користувач в стані очікування ключового слова
        if ("WAITING_SUBSCRIBE".equals(state) && !text.startsWith("/")) {
            userStates.remove(chatId);
            handleSubscribe(chatId, text);
            return;
        }

        switch (text) {
            case "/start" -> sendReply(chatId,
                "Привіт! Я Estonian Feed Bot 🇪🇪\n\n" +
                "Команди:\n" +
                "/news — останні новини\n" +
                "/jobs — останні вакансії\n" +
                "/subscribe — підписатись на ключове слово\n" +
                "/unsubscribe — відписатись\n" +
                "/subscriptions — мої підписки");
            case "/news" -> sendNews(chatId);
            case "/jobs" -> sendJobs(chatId);
            case "/subscribe" -> askForKeyword(chatId);
            case "/unsubscribe" -> showUnsubscribeMenu(chatId);
            case "/subscriptions" -> handleListSubscriptions(chatId);
            default -> {
                userStates.remove(chatId);
                sendReply(chatId, "Невідома команда. Спробуй /start");
            }
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
            sendReply(chatId, "Поки немає нових новин. Спробуй пізніше.");
            return;
        }
        articles.stream().limit(5).forEach(article -> {
            String msg = "📰 *" + article.getTitle() + "*\n" +
                "🔗 " + article.getUrl() + "\n" +
                "📡 " + article.getSource();
            sendMarkdown(chatId, msg);
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

    private void handleSubscribe(long chatId, String keyword) {
    if (keyword.isEmpty()) {
        sendReply(chatId, "Вкажи ключове слово. Наприклад: /subscribe narva");
        return;
    }
    keyword = keyword.toLowerCase();
    if (subscriptionRepository.existsByChatIdAndKeyword(chatId, keyword)) {
        sendReply(chatId, "Ти вже підписаний на «" + keyword + "»");
        return;
    }
    subscriptionRepository.save(new UserSubscription(chatId, keyword));
    sendReply(chatId, "✅ Підписався на «" + keyword + "»\nБудеш отримувати новини з цим словом.");
    }

    @org.springframework.transaction.annotation.Transactional
    private void handleUnsubscribe(long chatId, String keyword) {
        keyword = keyword.toLowerCase();
        if (!subscriptionRepository.existsByChatIdAndKeyword(chatId, keyword)) {
            sendReply(chatId, "У тебе немає підписки на «" + keyword + "»");
            return;
        }
        subscriptionRepository.deleteByChatIdAndKeyword(chatId, keyword);
        sendReply(chatId, "❌ Відписався від «" + keyword + "»");
    }

    private void handleListSubscriptions(long chatId) {
        var subs = subscriptionRepository.findByChatId(chatId);
        if (subs.isEmpty()) {
            sendReply(chatId, "У тебе немає підписок.\nДодай через /subscribe <слово>");
            return;
        }
        StringBuilder sb = new StringBuilder("Твої підписки:\n");
        subs.forEach(s -> sb.append("• ").append(s.getKeyword()).append("\n"));
        sendReply(chatId, sb.toString());
    }

    public void sendNotification(long chatId, String text) {
        sendReply(chatId, text);
    }

    private void askForKeyword(long chatId) {
        userStates.put(chatId, "WAITING_SUBSCRIBE");
        sendReply(chatId, "Напиши ключове слово для підписки:\n(наприклад: narva, tallinn, estonia)");
    }

    private void showUnsubscribeMenu(long chatId) {
        var subs = subscriptionRepository.findByChatId(chatId);
        if (subs.isEmpty()) {
            sendReply(chatId, "У тебе немає підписок.\nДодай через /subscribe");
            return;
        }

        // Будуємо кнопки для кожної підписки
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (var sub : subs) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("❌ " + sub.getKeyword());
            btn.setCallbackData("unsub:" + sub.getKeyword());
            rows.add(List.of(btn));
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Твої підписки — натисни щоб відписатись:");
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        String data = callback.getData();

        if (data.startsWith("unsub:")) {
            String keyword = data.substring("unsub:".length());
            subscriptionRepository.deleteByChatIdAndKeyword(chatId, keyword);

            // Відповідаємо на callback щоб кнопка не "зависла"
            try {
                org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answer =
                    new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
                answer.setCallbackQueryId(callback.getId());
                answer.setText("Відписався від «" + keyword + "»");
                execute(answer);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Оновлюємо меню — показуємо актуальний список
            showUnsubscribeMenu(chatId);
        }
    }

    private void sendMarkdown(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (Exception e) {
            // Якщо Markdown не спрацював — надсилаємо без форматування
            sendReply(chatId, text);
        }
    }
}