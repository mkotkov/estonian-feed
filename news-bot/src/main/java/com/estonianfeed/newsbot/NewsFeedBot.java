package com.estonianfeed.newsbot;

// JAVA IMPORTS
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SPRING FRAMEWORK IMPORTS
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// TELEGRAM IMPORTS
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

// MODELS
import com.estonianfeed.model.Article;
import com.estonianfeed.model.UserSourcePreference;
import com.estonianfeed.model.UserSubscription;
import com.estonianfeed.model.Source;
import com.estonianfeed.model.UserLanguagePreference;

// REPOSITORY
import com.estonianfeed.repository.ArticleRepository;
import com.estonianfeed.repository.SourceRepository;
import com.estonianfeed.repository.UserSourcePreferenceRepository;
import com.estonianfeed.repository.UserSubscriptionRepository;
import com.estonianfeed.repository.UserLanguagePreferenceRepository;

//SERVICE
import com.estonianfeed.service.Messages;

@Component
public class NewsFeedBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;
    private final ArticleRepository articleRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final Map<Long, String> userStates = new java.util.concurrent.ConcurrentHashMap<>();
    private final SourceRepository sourceRepository;
    private final UserSourcePreferenceRepository sourcePreferenceRepository;
    private final UserLanguagePreferenceRepository languagePreferenceRepository;

    public NewsFeedBot(
            @Value("${telegram.bot.token}") String botToken,
            ArticleRepository articleRepository,
            UserSubscriptionRepository subscriptionRepository,
            SourceRepository sourceRepository,
            UserSourcePreferenceRepository sourcePreferenceRepository,
            UserLanguagePreferenceRepository languagePreferenceRepository) {
        super(botToken);
        this.articleRepository = articleRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.sourceRepository = sourceRepository;
        this.sourcePreferenceRepository = sourcePreferenceRepository;
        this.languagePreferenceRepository = languagePreferenceRepository;
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
            case "/start" -> sendReply(chatId, Messages.get("welcome", getUserLanguage(chatId)));
            case "/news" -> sendNews(chatId);
            case "/sources" -> showSourcesMenu(chatId);
            case "/subscribe" -> askForKeyword(chatId);
            case "/unsubscribe" -> showUnsubscribeMenu(chatId);
            case "/subscriptions" -> handleListSubscriptions(chatId);
            case "/language" -> showLanguageMenu(chatId);
            default -> {
                userStates.remove(chatId);
                sendReply(chatId, Messages.get("unknown_command", getUserLanguage(chatId)));
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
        List<String> allowedSources = getAllowedSourceIds(chatId);
        var articles = articleRepository.findBySentFalseAndSourceIdInOrderByPublishedAtDesc(allowedSources);
       if (articles.isEmpty()) {
            sendReply(chatId, Messages.get("no_news", getUserLanguage(chatId)));
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
    
    @Scheduled(fixedDelay = 300000)
    public void notifySubscribers() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Article> freshArticles =
            articleRepository.findByNotifiedFalseAndPublishedAtAfterOrderByPublishedAtDesc(since);

        if (freshArticles.isEmpty()) return;

        var allSubscriptions = subscriptionRepository.findAll();
        if (!allSubscriptions.isEmpty()) {
            Map<Long, List<String>> subsByChatId = new HashMap<>();
            for (var sub : allSubscriptions) {
                subsByChatId
                    .computeIfAbsent(sub.getChatId(), k -> new ArrayList<>())
                    .add(sub.getKeyword());
            }

            for (var entry : subsByChatId.entrySet()) {
                long chatId = entry.getKey();
                List<String> keywords = entry.getValue();

                for (var article : freshArticles) {
                    String titleLower = article.getTitle().toLowerCase();
                    String descLower = article.getDescription() != null
                        ? article.getDescription().toLowerCase() : "";

                    boolean matches = keywords.stream()
                        .anyMatch(kw -> titleLower.contains(kw) || descLower.contains(kw));

                    if (matches) {
                        String msg = "🔔 Новина за твоєю підпискою:\n\n" +
                            "📰 " + article.getTitle() + "\n" +
                            article.getUrl();
                        sendNotification(chatId, msg);
                    }
                }
            }
        }

        // Позначаємо як сповіщені — незалежно чи знайшли підписку
        freshArticles.forEach(a -> a.setNotified(true));
        articleRepository.saveAll(freshArticles);
    }

    private void askForKeyword(long chatId) {
        userStates.put(chatId, "WAITING_SUBSCRIBE");
        sendReply(chatId, "Напиши ключове слово для підписки:\n(наприклад: narva, tallinn, estonia)");
    }

    private void showUnsubscribeMenu(long chatId) {
        List<UserSubscription> subs = subscriptionRepository.findByChatId(chatId);
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
            answerCallback(callback, "Відписався від «" + keyword + "»");
            showUnsubscribeMenu(chatId);

        } else if (data.startsWith("srctoggle:")) {
            String sourceId = data.substring("srctoggle:".length());
            toggleSource(chatId, sourceId);
            answerCallback(callback, "Оновлено");
            deleteOldSourcesMenu(callback);
            showSourcesMenu(chatId);

        } else if (data.equals("srcreset")) {
            sourcePreferenceRepository.deleteByChatId(chatId);
            answerCallback(callback, "Повернуто до всіх джерел");
            deleteOldSourcesMenu(callback);
            showSourcesMenu(chatId);

        } else if (data.equals("srcdone")) {
            answerCallback(callback, "Збережено");
        } else if (data.startsWith("lang:")) {
            String lang = data.substring("lang:".length());
            setUserLanguage(chatId, lang);
            answerCallback(callback, "Language set to " + lang);
            sendReply(chatId, getLanguageConfirmation(lang));
        } else if (data.startsWith("srclang:")) {
            String lang = data.substring("srclang:".length());
            deleteOldSourcesMenu(callback);
            if ("ALL".equals(lang)) {
                showSourcesMenu(chatId, (String) null);
            } else {
                showSourcesMenu(chatId, lang);
            }
        }
    }

    private void answerCallback(CallbackQuery callback, String text) {
        try {
            org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answer =
                new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
            answer.setCallbackQueryId(callback.getId());
            answer.setText(text);
            execute(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSource(long chatId, String sourceId) {
        if (sourcePreferenceRepository.existsByChatIdAndSourceId(chatId, sourceId)) {
            sourcePreferenceRepository.deleteByChatIdAndSourceId(chatId, sourceId);
        } else {
            sourcePreferenceRepository.save(new UserSourcePreference(chatId, sourceId));
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

    private void showSourcesMenu(long chatId) {
        showSourcesMenu(chatId, null);
    }

    private void showSourcesMenu(long chatId, String languageFilter) {
        List<Source> allSources = sourceRepository.findAll();
        if (languageFilter != null) {
            allSources = allSources.stream()
                .filter(s -> s.getLanguage().equals(languageFilter))
                .toList();
        }

        List<UserSourcePreference> userPrefs = sourcePreferenceRepository.findByChatId(chatId);
        java.util.Set<String> selectedIds = new java.util.HashSet<>();
        userPrefs.forEach(p -> selectedIds.add(p.getSourceId()));

        boolean hasCustomSelection = !userPrefs.isEmpty();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки фільтра мови — нагорі меню
        InlineKeyboardButton allBtn = new InlineKeyboardButton();
        allBtn.setText(languageFilter == null ? "🔘 Всі мови" : "⚪ Всі мови");
        allBtn.setCallbackData("srclang:ALL");

        InlineKeyboardButton etBtn = new InlineKeyboardButton();
        etBtn.setText("ET".equals(languageFilter) ? "🔘 ET" : "⚪ ET");
        etBtn.setCallbackData("srclang:ET");

        InlineKeyboardButton ruBtn = new InlineKeyboardButton();
        ruBtn.setText("RU".equals(languageFilter) ? "🔘 RU" : "⚪ RU");
        ruBtn.setCallbackData("srclang:RU");

        InlineKeyboardButton enBtn = new InlineKeyboardButton();
        enBtn.setText("EN".equals(languageFilter) ? "🔘 EN" : "⚪ EN");
        enBtn.setCallbackData("srclang:EN");

        rows.add(List.of(allBtn, etBtn, ruBtn, enBtn));

        for (Source source : allSources) {
            boolean isSelected = hasCustomSelection
                ? selectedIds.contains(source.getId())
                : true;

            String emoji = isSelected ? "✅" : "⬜";
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(emoji + " " + source.getName() + " (" + source.getLanguage() + ")");
            btn.setCallbackData("srctoggle:" + source.getId());
            rows.add(List.of(btn));
        }

        InlineKeyboardButton resetBtn = new InlineKeyboardButton();
        resetBtn.setText("🔄 Скинути (всі джерела)");
        resetBtn.setCallbackData("srcreset");
        rows.add(List.of(resetBtn));

        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Готово");
        doneBtn.setCallbackData("srcdone");
        rows.add(List.of(doneBtn));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        String statusText = hasCustomSelection
            ? "Обрані джерела (натисни щоб змінити):"
            : "Зараз отримуєш новини з усіх джерел.\nНатисни щоб обрати конкретні:";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(statusText);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private List<String> getAllowedSourceIds(long chatId) {
        var userPrefs = sourcePreferenceRepository.findByChatId(chatId);
        if (userPrefs.isEmpty()) {
            // Дефолт — всі джерела
            return sourceRepository.findAll().stream()
                .map(Source::getId)
                .toList();
        }
        return userPrefs.stream()
            .map(UserSourcePreference::getSourceId)
            .toList();
    }

    private void showLanguageMenu(long chatId) {
        InlineKeyboardButton et = new InlineKeyboardButton();
        et.setText("🇪🇪 Eesti");
        et.setCallbackData("lang:ET");

        InlineKeyboardButton ru = new InlineKeyboardButton();
        ru.setText("русский");
        ru.setCallbackData("lang:RU");

        InlineKeyboardButton en = new InlineKeyboardButton();
        en.setText("🇬🇧 English");
        en.setCallbackData("lang:EN");

        InlineKeyboardButton uk = new InlineKeyboardButton();
        uk.setText("🇺🇦 Українська");
        uk.setCallbackData("lang:UK");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(
            List.of(et, ru),
            List.of(en, uk)
        ));

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Choose your language / Виберіть мову / Виберіть мову:");
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteOldSourcesMenu(CallbackQuery callback) {
        try {
            org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage delete =
                new org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage();
            delete.setChatId(callback.getMessage().getChatId());
            delete.setMessageId(callback.getMessage().getMessageId());
            execute(delete);
        } catch (Exception e) {
            // нічого, просто покажемо нове повідомлення
        }
    }

    private String getUserLanguage(long chatId) {
        return languagePreferenceRepository.findByChatId(chatId)
            .map(UserLanguagePreference::getLanguage)
            .orElse("EN");
    }

    private void setUserLanguage(long chatId, String lang) {
        var existing = languagePreferenceRepository.findByChatId(chatId);
        if (existing.isPresent()) {
            existing.get().setLanguage(lang);
            languagePreferenceRepository.save(existing.get());
        } else {
            languagePreferenceRepository.save(new UserLanguagePreference(chatId, lang));
        }
    }

    private String getLanguageConfirmation(String lang) {
        return switch (lang) {
            case "ET" -> "✅ Keel on seatud eesti keelele.";
            case "RU" -> "✅ Язык установлен на русский.";
            case "UK" -> "✅ Мову встановлено на українську.";
            default -> "✅ Language set to English.";
        };
    }
}