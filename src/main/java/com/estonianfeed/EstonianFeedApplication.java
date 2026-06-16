package com.estonianfeed;

import com.estonianfeed.bot.EstonianFeedBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling 
public class EstonianFeedApplication {

    public static void main(String[] args) throws TelegramApiException {
        ApplicationContext context = SpringApplication.run(EstonianFeedApplication.class, args);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(context.getBean(EstonianFeedBot.class));
    }
}