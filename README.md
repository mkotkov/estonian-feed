# 🇪🇪 Estonian Feed Bot

Telegram bot that aggregates tech news and jobs from HackerNews.
Designed with the Estonian tech market in mind — easily extendable with local sources (ERR, CV.ee, DOU) once deployed on a server.

## Features

- 📰 `/news` — latest tech news from HackerNews
- 💼 `/jobs` — latest job postings from HackerNews
- 🔄 Auto-fetches new content every 5 minutes
- 🛡️ Deduplication — no repeated articles

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Data JPA + H2 (dev) / PostgreSQL (prod)
- Telegram Bot API
- HackerNews API

## Architecture
src/main/java/com/estonianfeed/

├── bot/                  # Telegram bot handler

├── model/                # JPA entities (Article, Job)

├── repository/           # Spring Data repositories

└── service/              # FetcherService with @Scheduled

## Getting Started

1. Clone the repository
2. Copy the config template:
```bash
   cp src/main/resources/application.properties.example \
      src/main/resources/application.properties
```
3. Fill in your values in `application.properties`:
```properties
   telegram.bot.token=YOUR_TOKEN_HERE
   telegram.bot.username=YOUR_BOT_USERNAME
```
4. Run:
```bash
   ./mvnw spring-boot:run
```

## Roadmap

- [ ] Add Estonian local sources (ERR News, CV.ee RSS)
- [ ] User subscriptions with keyword filters
- [ ] Language selection (ET / EN / RU)
- [ ] Public Telegram channel for broadcast
- [ ] Docker + VPS deployment
- [ ] PostgreSQL for production