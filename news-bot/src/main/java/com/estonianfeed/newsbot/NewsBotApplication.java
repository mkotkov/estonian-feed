package com.estonianfeed.newsbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling
@EntityScan("com.estonianfeed.model")
@EnableJpaRepositories("com.estonianfeed.repository")
public class NewsBotApplication {

    public static void main(String[] args) throws TelegramApiException {
        ApplicationContext context = SpringApplication.run(NewsBotApplication.class, args);
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(context.getBean(NewsFeedBot.class));
    }
}