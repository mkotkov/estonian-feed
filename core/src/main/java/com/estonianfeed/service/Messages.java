package com.estonianfeed.service;

import java.util.Map;

public class Messages {

    private static final Map<String, Map<String, String>> TEXTS = Map.of(
        "welcome", Map.of(
            "ET", "Tere! Olen Estonian Feed Bot 🇪🇪\n\nKäsud:\n/news — viimased uudised\n/sources — valige allikad\n/language — valige keel\n/subscribe — telli märksõna\n/unsubscribe — loobu tellimusest\n/subscriptions — minu tellimused",
            "RU", "Привет! Я Estonian Feed Bot 🇪🇪\n\nКоманды:\n/news — последние новости\n/sources — выбрать источники\n/language — выбрать язык\n/subscribe — подписаться на слово\n/unsubscribe — отписаться\n/subscriptions — мои подписки",
            "EN", "Hello! I'm Estonian Feed Bot 🇪🇪\n\nCommands:\n/news — latest news\n/sources — choose sources\n/language — choose language\n/subscribe — subscribe to a keyword\n/unsubscribe — remove subscription\n/subscriptions — my subscriptions",
            "UK", "Привіт! Я Estonian Feed Bot 🇪🇪\n\nКоманди:\n/news — останні новини\n/sources — обрати джерела\n/language — обрати мову\n/subscribe — підписатись на слово\n/unsubscribe — відписатись\n/subscriptions — мої підписки"
        ),
        "no_news", Map.of(
            "ET", "Värskeid uudiseid ei ole. Proovige hiljem.",
            "RU", "Пока нет новых новостей. Попробуй позже.",
            "EN", "No new news yet. Try again later.",
            "UK", "Поки немає нових новин. Спробуй пізніше."
        ),
        "no_jobs", Map.of(
            "ET", "Uusi töökuulutusi ei ole.",
            "RU", "Пока нет новых вакансий.",
            "EN", "No new job postings yet.",
            "UK", "Поки немає нових вакансій."
        ),
        "unknown_command", Map.of(
            "ET", "Tundmatu käsk. Proovi /start",
            "RU", "Неизвестная команда. Попробуй /start",
            "EN", "Unknown command. Try /start",
            "UK", "Невідома команда. Спробуй /start"
        ),
        "choose_language", Map.of(
            "ET", "Valige keel:",
            "RU", "Выберите язык:",
            "EN", "Choose your language:",
            "UK", "Обери мову:"
        )
    );

    public static String get(String key, String lang) {
        Map<String, String> entry = TEXTS.get(key);
        if (entry == null) return key;
        return entry.getOrDefault(lang, entry.get("EN"));
    }
}