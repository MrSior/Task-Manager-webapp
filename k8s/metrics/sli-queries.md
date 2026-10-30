# PromQL для SLI / SLO / SLA

VictoriaMetrics: `kubectl -n metrics port-forward svc/vmsingle-victoria-metrics-single-server 8428:8428`  
UI: http://127.0.0.1:8428/vmui

## Сырые метрики

```promql
taskmanager_auth_logins_total
```

```promql
taskmanager_auth_failed_logins_total
```

```promql
sum by (status) (rate(http_server_requests_seconds_count{namespace="application"}[5m]))
```

```promql
pg_up{namespace="application"}
```

## SLI-1 HTTP availability (RBSLO)

```promql
sum(rate(http_server_requests_seconds_count{namespace="application",status!~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{namespace="application"}[5m]))
```

SLO-1 breach (1 = нарушен):

```promql
(
  sum(rate(http_server_requests_seconds_count{namespace="application",status!~"5.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{namespace="application"}[5m]))
) < bool 0.99
```

## SLI-2 Auth success (RBSLO)

```promql
sum(rate(taskmanager_auth_logins_total[5m]))
/
(
  sum(rate(taskmanager_auth_logins_total[5m]))
  +
  sum(rate(taskmanager_auth_failed_logins_total[5m]))
)
```

SLO-2 breach:

```promql
(
  sum(rate(taskmanager_auth_logins_total[5m]))
  /
  (
    sum(rate(taskmanager_auth_logins_total[5m]))
    +
    sum(rate(taskmanager_auth_failed_logins_total[5m]))
  )
) < bool 0.95
```

## SLI-3 Postgres availability (TBSLO)

Демо-окно 15m:

```promql
avg_over_time(pg_up{namespace="application"}[15m])
```

SLO-3 breach (демо-порог 0.999 на 15m):

```promql
avg_over_time(pg_up{namespace="application"}[15m]) < bool 0.999
```

## Composite SLA score (доля выполненных SLO, 0..1)

```promql
(
  ((
    sum(rate(http_server_requests_seconds_count{namespace="application",status!~"5.."}[5m]))
    /
    sum(rate(http_server_requests_seconds_count{namespace="application"}[5m]))
  ) >= bool 0.99)
  +
  ((
    sum(rate(taskmanager_auth_logins_total[5m]))
    /
    (
      sum(rate(taskmanager_auth_logins_total[5m]))
      +
      sum(rate(taskmanager_auth_failed_logins_total[5m]))
    )
  ) >= bool 0.95)
  +
  (
    max(avg_over_time(pg_up{namespace="application"}[15m])) >= bool 0.999
  )
) / 3
```
