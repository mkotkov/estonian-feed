# 🇪🇪 Estonian Feed

Two Telegram bots that aggregate news and jobs from Estonian media sources.
Built with Java 17 + Spring Boot as a Maven multi-module project.

## Bots

| Bot | Description | Link |
|-----|-------------|-------|
| Estonian Feed | News from ERR, Postimees, Delfi and more | [@estonian_feed_bot](https://t.me/estonian_feed_bot) |
| Estonian Jobs | Job postings from Estonian media | [@estonian_jobs_bot](https://t.me/estonian_jobs_bot) |

## Channels

| Channel | Description | Link |
|---------|-------------|-------|
| Estonian News Feed | Auto-published news | [t.me/eestifeed](https://t.me/eestifeed) |
| Estonian Jobs Feed | Auto-published jobs | [t.me/eesti_job](https://t.me/eesti_job) |

## Features

- 📰 `/news` — latest news from Estonian sources
- 💼 `/jobs` — latest job postings
- 🔔 `/subscribe <keyword>` — get notified when keyword appears in news
- 🔕 `/unsubscribe <keyword>` — remove subscription
- 📋 `/subscriptions` — list your subscriptions
- 🔄 Auto-fetches new content every 5 minutes
- 🛡️ Deduplication — no repeated articles
- 💾 Persistent storage — data survives restarts

## News Sources

| Source | Language | Type |
|--------|----------|------|
| ERR News | ET / EN | News |
| Postimees | ET | News |
| Estonian World | EN | News |
| Gazeta.ee | RU | News |
| Narva Leht | ET / RU | Regional |
| Õhtuleht | ET | News |
| Äripäev | ET | Business |

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Data JPA + H2 (dev) / PostgreSQL (prod)
- Telegram Bot API (`telegrambots` 6.9.7.1)
- Rome (RSS parser)
- Maven multi-module

## Architecture

```
   estonian-feed/

   ├── core/          # Shared: models, repositories, FetcherService

   ├── news-bot/      # News bot + ERR, Postimees, Delfi sources

   └── jobs-bot/      # Jobs bot + Äripäev source
```

Each module has its own database and Telegram bot token.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Setup

1. Clone the repository:
```bash
git clone https://github.com/<your-username>/estonian-feed.git
cd estonian-feed
```

2. Configure news-bot:
```bash
cp news-bot/src/main/resources/application.properties.example \
   news-bot/src/main/resources/application.properties
```

Edit `news-bot/src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_NEWS_BOT_TOKEN
telegram.bot.username=your_news_bot
telegram.channel.id=YOUR_NEWS_CHANNEL_ID
```

3. Configure jobs-bot:
```bash
cp jobs-bot/src/main/resources/application.properties.example \
   jobs-bot/src/main/resources/application.properties
```

Edit `jobs-bot/src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_JOBS_BOT_TOKEN
telegram.bot.username=your_jobs_bot
telegram.channel.id=YOUR_JOBS_CHANNEL_ID
```

4. Build:
```bash
./mvnw install -DskipTests
```

5. Run news-bot:
```bash
./mvnw spring-boot:run -pl news-bot
```

6. Run jobs-bot (separate terminal):
```bash
./mvnw spring-boot:run -pl jobs-bot
```

## Roadmap

- [x] Estonian news sources (ERR, Postimees, Narva Leht, Gazeta)
- [x] User subscriptions with keyword filters
- [x] Persistent H2 file database
- [x] Maven multi-module architecture
- [ ] Auto-publish to Telegram channels
- [ ] Language selection (ET / EN / RU)
- [ ] PostgreSQL for production
- [ ] Docker + VPS deployment