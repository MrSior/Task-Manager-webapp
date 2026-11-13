# Grafana — дашборды Task Manager

Стек: **Grafana** (Helm) читает метрики из **VictoriaMetrics Single** в ns `metrics`.

Конфиги:

- Helm values: [`grafana-values.yaml`](grafana-values.yaml)
- дашборд приложения: [`dashboards/task-manager.json`](dashboards/task-manager.json) → ConfigMap [`grafana-dashboard-task-manager.yaml`](grafana-dashboard-task-manager.yaml)
- Postgres Exporter: JSON из grafana.com **9628** ([`dashboards/postgres-exporter.json`](dashboards/postgres-exporter.json)) → ConfigMap [`grafana-dashboard-postgres.yaml`](grafana-dashboard-postgres.yaml)

## Установка

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

kubectl apply -f k8s/metrics/grafana-dashboard-task-manager.yaml
kubectl apply -f k8s/metrics/grafana-dashboard-postgres.yaml

helm upgrade --install grafana grafana/grafana \
  -n metrics \
  -f k8s/metrics/grafana-values.yaml
```

Логин по умолчанию: `admin` / `admin` (задано в values).

## Доступ

```bash
kubectl -n metrics port-forward svc/grafana 3000:80
# http://127.0.0.1:3000
```

## Data source

В Grafana provisioning:

| Поле | Значение |
|---|---|
| Name | `VictoriaMetrics` |
| Type | Prometheus |
| URL | `http://vmsingle-victoria-metrics-single-server.metrics.svc:8428` |
| Access | proxy |
| Default | yes |

Проверка: Connections → Data sources → VictoriaMetrics → **Test**.

## Дашборды

Папка **Task Manager**:

### 1. Task Manager — Overview

Подходы **Golden Signals / RED** (верх) и **USE** (runtime), плюс отдельный ряд **SLA**.

| Секция | Панели |
|---|---|
| Golden Signals / RED | Backend status, RPS by status, HTTP 5xx error rate, Total RPS, Latency p95/p50, Auth success, Tasks create/complete |
| Runtime / USE | JVM heap utilization, Process CPU, JVM threads, GC pause rate |
| SLA | Composite SLA score + SLI-1 HTTP / SLI-2 Auth / SLI-3 Postgres (пороги 0.99 / 0.95 / 0.999) |

PromQL для SLA — те же, что в [`sli-queries.md`](sli-queries.md).

### 2. PostgreSQL Database (imported)

Импорт [grafana.com/dashboards/9628](https://grafana.com/grafana/dashboards/9628) (revision 8, JSON в репозитории), datasource → `VictoriaMetrics`.  
Метрики снимает sidecar `postgres-exporter` в StatefulSet `postgres` (ns `application`).

## Демо живых графиков

```bash
ROUNDS=60 ./scripts/sli-demo.sh mixed

# опционально: рост 5xx / падение SLI-1
kubectl -n application set env deployment/task-manager-backend APP_DEMO_FAIL_RATE=0.5
ROUNDS=60 ./scripts/sli-demo.sh tasks
kubectl -n application set env deployment/task-manager-backend APP_DEMO_FAIL_RATE-
```

## Проверка

```bash
kubectl -n metrics get pods,svc
kubectl -n metrics get cm -l grafana_dashboard=1
```
