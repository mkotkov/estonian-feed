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
- Spring Data JPA + PostgreSQL 16
- Telegram Bot API (`telegrambots` 6.9.7.1)
- Rome (RSS parser)
- Maven multi-module
- Docker + Docker Compose

## Architecture

```
estonian-feed/
├── core/          # Shared: models, repositories, FetcherService, Messages (i18n)
├── news-bot/      # News bot + ERR, Postimees, Gazeta sources
├── jobs-bot/      # Jobs bot + Äripäev source
├── docker-compose.yml
└── init.sql       # Creates newsdb and jobsdb on first run
```

Each module has its own PostgreSQL database and Telegram bot token.

## Quick Start (Docker)

This is the recommended way to run the project.

1. Clone the repository:
```bash
git clone https://github.com/<your-username>/estonian-feed.git
cd estonian-feed
```

2. Create `.env` from the template:
```bash
cp .env.example .env
```

Edit `.env` with your values:
```bash
DB_PASSWORD=your_db_password
NEWS_BOT_TOKEN=your_news_bot_token
NEWS_BOT_USERNAME=estonian_feed_bot
NEWS_CHANNEL_ID=-1001234567890
JOBS_BOT_TOKEN=your_jobs_bot_token
JOBS_BOT_USERNAME=estonian_jobs_bot
JOBS_CHANNEL_ID=-1001234567891
```

3. Build and run:
```bash
./mvnw clean install -DskipTests
docker compose up --build -d
```

4. Check logs:
```bash
docker compose logs -f news-bot
```

## Manual Setup (without Docker)

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 14+

### Setup

1. Create PostgreSQL databases:
```bash
sudo -u postgres psql
```
```sql
CREATE USER estonianfeed WITH PASSWORD 'your_password';
CREATE DATABASE newsdb OWNER estonianfeed;
CREATE DATABASE jobsdb OWNER estonianfeed;
```

2. Configure each module:
```bash
cp news-bot/src/main/resources/application.properties.example \
   news-bot/src/main/resources/application.properties

cp jobs-bot/src/main/resources/application.properties.example \
   jobs-bot/src/main/resources/application.properties
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

Edit `jobs-bot/src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_JOBS_BOT_TOKEN
telegram.bot.username=your_jobs_bot
telegram.channel.id=YOUR_JOBS_CHANNEL_ID

spring.datasource.url=jdbc:postgresql://localhost:5432/jobsdb
spring.datasource.username=estonianfeed
spring.datasource.password=your_password
```

3. Build and run:
```bash
./mvnw install -DskipTests
./mvnw spring-boot:run -pl news-bot
# In a separate terminal:
./mvnw spring-boot:run -pl jobs-bot
```

## Deployment (VPS)

```bash
git clone https://github.com/<your-username>/estonian-feed.git
cd estonian-feed
cp .env.example .env
nano .env  # fill in your tokens

./mvnw clean install -DskipTests
docker compose up --build -d
```

To update after code changes:
```bash
git pull
./mvnw clean install -DskipTests
docker compose up --build -d
```

## Roadmap

- [x] Estonian news sources (ERR, Postimees, Narva Leht, Gazeta, and more)
- [x] User subscriptions with keyword filters (last 24h)
- [x] PostgreSQL for persistent storage
- [x] Maven multi-module architecture
- [x] Per-user source selection (`/sources`) with language filter
- [x] Multi-language interface (ET / RU / EN / UK)
- [x] Docker + VPS deployment
- [ ] Auto-publish to Telegram channels
- [ ] Fix and deploy jobs-bot