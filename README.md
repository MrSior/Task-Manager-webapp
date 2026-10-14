# Task Manager

Веб-менеджер задач: Spring Boot API + простой HTML/CSS/JS фронт + PostgreSQL.

## Быстрый старт

```bash
cp .env.example .env
docker compose up --build
```

После запуска:

- UI: http://localhost:3000
- API: http://localhost:8080/api

## Переменные окружения

Скопируйте `.env.example` в `.env`. Чувствительные данные (пароль БД и т.п.) задаются только через env.

| Переменная | Описание |
|---|---|
| `POSTGRES_DB` | Имя БД |
| `POSTGRES_USER` | Пользователь БД |
| `POSTGRES_PASSWORD` | Пароль БД |
| `SPRING_DATASOURCE_URL` | JDBC URL (backing service) |
| `SPRING_DATASOURCE_USERNAME` | Логин БД для приложения |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД для приложения |
| `SERVER_PORT` | Порт HTTP API (внутри compose проброшен наружу) |
| `FRONTEND_PORT` | Внешний порт UI (маппится на 80 внутри контейнера) |
| `APP_PROCESS_TYPE` | `web` / `worker` / `cron` / `migrate` |

## Процессы (12-factor)

Один и тот же JAR-артефакт запускается как разные процессы:

- `migrate` - одноразовые Flyway-миграции, затем exit
- `backend` (`web`) - HTTP API
- `worker` / `cron` - каркас фоновых процессов (масштабируются добавлением контейнеров)

Graceful shutdown по SIGTERM включён (`server.shutdown=graceful`).

Данные переживают перезапуск благодаря volume `pgdata`.

Логи пишутся в stdout в JSON (одна строка = один event): `timestamp`, `level`, `message`, `service.name`, а также `logger`, `thread`, `service.version`, `service.instance.id` (hostname/pod), `process.type`, при HTTP — `http.method` / `http.path` / `http.status_code` / `http.duration_ms`.

## API

| Method | Path | Auth | Описание |
|---|---|---|---|
| POST | `/api/auth/register` | нет | Регистрация `{login, password}` |
| POST | `/api/auth/login` | нет | Вход → `{token, login}` |
| POST | `/api/auth/logout` | Bearer | Выход |
| GET | `/api/tasks` | Bearer | Список задач |
| POST | `/api/tasks` | Bearer | Создать `{title, description}` |
| PATCH | `/api/tasks/{id}` | Bearer | Обновить title/description/status |
| DELETE | `/api/tasks/{id}` | Bearer | Удалить |

Статусы задач: `NEW`, `IN_PROGRESS`, `DONE`.

Пароли сейчас хранятся как plain text (по условию ДЗ). Точка расширения - `PasswordEncoder`.

## Структура

```
backend/     Spring Boot + Maven
frontend/    HTML + CSS + JS (nginx)
docker-compose.yml
```
# Task-Manager-webapp
