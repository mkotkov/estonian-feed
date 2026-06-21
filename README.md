# 🇪🇪 Estonian Feed

Two Telegram bots that aggregate news and jobs from Estonian media sources.
Built with Java 17 + Spring Boot as a Maven multi-module project.

## Bots

| Bot | Description | Link |
|-----|-------------|-------|
| Estonian Feed | News from ERR, Postimees, Gazeta and more | [@estonian_feed_bot](https://t.me/estonian_feed_bot) |
| Estonian Jobs | Job postings from Estonian media | [@estonian_jobs_bot](https://t.me/estonian_jobs_bot) |

## Channels

| Channel | Description | Link |
|---------|-------------|-------|
| Estonian News Feed | Auto-published news | [t.me/eestifeed](https://t.me/eestifeed) |
| Estonian Jobs Feed | Auto-published jobs | [t.me/eesti_job](https://t.me/eesti_job) |

## Features

- 📰 `/news` — latest news from selected sources
- 💼 `/jobs` — latest job postings
- 🌐 `/sources` — choose which sources to follow, filterable by language, or get all by default
- 🗣️ `/language` — bot interface language: Estonian, Russian, English, or Ukrainian
- 🔔 `/subscribe` — get notified when a keyword appears in fresh news (last 24h)
- 🔕 `/unsubscribe` — remove a subscription
- 📋 `/subscriptions` — list your subscriptions
- 🔄 Auto-fetches new content every 5 minutes
- 🛡️ Deduplication — no repeated articles
- 💾 PostgreSQL storage — data survives restarts

## News Sources

| Source | Language | Type |
|--------|----------|------|
| ERR News (ET) | ET | News |
| ERR News (EN) | EN | News |
| Postimees | ET | News |
| Estonian World | EN | News |
| Gazeta.ee | RU | News |
| Narva Leht | RU | Regional |
| Sõnumitooja | ET | Regional |
| Kesknädal | ET | News |
| Õhtuleht | ET | News |
| Äripäev | ET | Business (jobs) |

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Data JPA + PostgreSQL
- Telegram Bot API (`telegrambots` 6.9.7.1)
- Rome (RSS parser)
- Maven multi-module

## Architecture
```
   estonian-feed/
   ├── core/          # Shared: models, repositories, FetcherService, Messages (i18n)
   ├── news-bot/      # News bot + ERR, Postimees, Gazeta sources
   └── jobs-bot/      # Jobs bot + Äripäev source
```

Each module has its own PostgreSQL database and Telegram bot token.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 14+

### Setup

1. Clone the repository:
```bash
git clone https://github.com/<your-username>/estonian-feed.git
cd estonian-feed
```

2. Create PostgreSQL databases:
```bash
sudo -u postgres psql
```
```sql
CREATE USER estonianfeed WITH PASSWORD 'your_password';
CREATE DATABASE newsdb OWNER estonianfeed;
CREATE DATABASE jobsdb OWNER estonianfeed;
```

3. Configure news-bot:
```bash
cp news-bot/src/main/resources/application.properties.example \
   news-bot/src/main/resources/application.properties
```

Edit `news-bot/src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_NEWS_BOT_TOKEN
telegram.bot.username=your_news_bot
telegram.channel.id=YOUR_NEWS_CHANNEL_ID

spring.datasource.url=jdbc:postgresql://localhost:5432/newsdb
spring.datasource.username=estonianfeed
spring.datasource.password=your_password
```

4. Configure jobs-bot:
```bash
cp jobs-bot/src/main/resources/application.properties.example \
   jobs-bot/src/main/resources/application.properties
```

Edit `jobs-bot/src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_JOBS_BOT_TOKEN
telegram.bot.username=your_jobs_bot
telegram.channel.id=YOUR_JOBS_CHANNEL_ID

spring.datasource.url=jdbc:postgresql://localhost:5432/jobsdb
spring.datasource.username=estonianfeed
spring.datasource.password=your_password
```

5. Build:
```bash
./mvnw install -DskipTests
```

6. Run news-bot:
```bash
./mvnw spring-boot:run -pl news-bot
```

7. Run jobs-bot (separate terminal):
```bash
./mvnw spring-boot:run -pl jobs-bot
```

## Roadmap

- [x] Estonian news sources (ERR, Postimees, Narva Leht, Gazeta, and more)
- [x] User subscriptions with keyword filters
- [x] PostgreSQL for persistent storage
- [x] Maven multi-module architecture
- [x] Per-user source selection (`/sources`)
- [x] Multi-language interface (ET / RU / EN / UK)
- [ ] Subscriptions respect source/language preferences
- [ ] Fix jobs-bot data fetching
- [ ] Auto-publish to Telegram channels
- [ ] Docker + VPS deployment