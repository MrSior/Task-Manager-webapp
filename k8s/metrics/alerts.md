# Алерты Task Manager

Стек: **vmalert** читает метрики из VictoriaMetrics Single и шлёт уведомления в **Alertmanager** → **Telegram**.

Конфиги:

- правила: [`alert-rules.yaml`](alert-rules.yaml) (копия в [`vmalert-values.yaml`](vmalert-values.yaml) → `server.config.alerts`)
- Alertmanager: секция `alertmanager` в [`vmalert-values.yaml`](vmalert-values.yaml) или Secret из [`alertmanager-telegram-secret.yaml.example`](alertmanager-telegram-secret.yaml.example)
- демо без бота: webhook sink [`webhook-logger.yaml`](webhook-logger.yaml) (логирует JSON уведомлений)
## Какие алерты и почему

| Алерт | Тип | Условие | Почему |
|---|---|---|---|
| `HttpAvailabilityLow` | сервис | доля не-5xx HTTP `< 0.99` за 5m, `for: 1m` | нарушение SLO-1; деградация API / 5xx |
| `AuthFailureRateHigh` | сервис | успешность логинов `< 0.95` за 5m, `for: 1m` | нарушение SLO-2; brute-force / баг auth |
| `JvmHeapUsageHigh` | runtime (Actuator) | heap used/max `> 0.85`, `for: 2m` | риск OOM / GC thrashing |
| `ProcessCpuHigh` | runtime (Actuator) | `process_cpu_usage > 0.8`, `for: 2m` | перегрузка инстанса до каскада ошибок |

Сервисные выражения — инверсия SLI из [`sli-queries.md`](sli-queries.md).

**Демо-пороги runtime:** если на Minikube алерты JVM/CPU не срабатывают, временно снизьте пороги (например heap `> 0.4`, CPU `> 0.1`), сделайте `helm upgrade`, снимите скрин FIRING, затем верните боевые значения `0.85` / `0.8`.

## Установка

```bash
# локальный sink уведомлений (если Telegram-бота ещё нет)
kubectl -n metrics apply -f k8s/metrics/webhook-logger.yaml

# Telegram credentials (из example) — для реального сообщения в чат
cp k8s/metrics/alertmanager-telegram-secret.yaml.example /tmp/am-telegram-secret.yaml
# отредактируйте REPLACE_BOT_TOKEN и REPLACE_CHAT_ID (chat_id ≠ 0)
kubectl -n metrics apply -f /tmp/am-telegram-secret.yaml
# затем в vmalert-values.yaml раскомментируйте alertmanager.configSecret

helm pull oci://ghcr.io/victoriametrics/helm-charts/victoria-metrics-alert --untar
helm upgrade --install vmalert ./victoria-metrics-alert \
  -n metrics \
  -f k8s/metrics/vmalert-values.yaml
```

Проверка:

```bash
kubectl -n metrics get pods
kubectl -n metrics port-forward svc/vmalert-victoria-metrics-alert-server 8880:8880
# http://127.0.0.1:8880/api/v1/rules

kubectl -n metrics port-forward svc/vmalert-victoria-metrics-alert-alertmanager 9093:9093
# http://127.0.0.1:9093/#/alerts
```

Имена Service могут отличаться — смотрите `kubectl -n metrics get svc`.

## Демо срабатывания

### HttpAvailabilityLow + AuthFailureRateHigh

```bash
# HTTP 5xx → HttpAvailabilityLow
kubectl -n application set env deployment/task-manager-backend APP_DEMO_FAIL_RATE=0.5
ROUNDS=60 ./scripts/sli-demo.sh tasks

# Failed logins → AuthFailureRateHigh
ROUNDS=60 ./scripts/sli-demo.sh fail

# сброс
kubectl -n application set env deployment/task-manager-backend APP_DEMO_FAIL_RATE-
```

### Runtime (JVM / CPU)

```bash
helm upgrade vmalert ./victoria-metrics-alert -n metrics \
  -f k8s/metrics/vmalert-values.yaml \
  -f k8s/metrics/vmalert-values-demo-runtime.yaml
```

### После демо

```bash
kubectl -n application set env deployment/task-manager-backend APP_DEMO_FAIL_RATE-
helm upgrade vmalert ./victoria-metrics-alert -n metrics \
  -f k8s/metrics/vmalert-values.yaml
```
