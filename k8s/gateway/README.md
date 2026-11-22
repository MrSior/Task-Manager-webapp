# Envoy Gateway — стратегии развёртывания

## Установка Envoy Gateway

```bash
helm upgrade --install eg oci://docker.io/envoyproxy/gateway-helm \
  --version v1.2.6 \
  -n envoy-gateway-system \
  --create-namespace

kubectl -n envoy-gateway-system wait --for=condition=Available deployment/envoy-gateway --timeout=180s
```

## Базовые ресурсы

```bash
# У меня в проекте есть nginx Ingress для app.test. Его надо отключить, чтобы не конкурировал
kubectl -n application delete ingress app --ignore-not-found

kubectl apply -f k8s/gateway/gateway.yaml
kubectl apply -f k8s/gateway/httproute.yaml
kubectl apply -f k8s/gateway/backend-v1.yaml
kubectl apply -f k8s/gateway/backend-v2.yaml
```

Дождаться Ready:

```bash
kubectl -n application get gateway,httproute,pods,svc
```

## Доступ

Envoy Proxy Service создаётся в `envoy-gateway-system` (тип LoadBalancer; в minikube EXTERNAL-IP часто `<pending>` — это нормально).

```bash
kubectl -n envoy-gateway-system get svc -l gateway.envoyproxy.io/owning-gateway-name=task-manager-gw

ENVOY_SVC=$(kubectl -n envoy-gateway-system get svc \
  -l gateway.envoyproxy.io/owning-gateway-name=task-manager-gw \
  -o jsonpath='{.items[0].metadata.name}')
kubectl -n envoy-gateway-system port-forward "svc/${ENVOY_SVC}" 8088:80
```

Альтернатива через NodePort:

```bash
minikube service -n envoy-gateway-system "$ENVOY_SVC" --url
```

Открыть: `http://app.test:8088/` (в `/etc/hosts` — `127.0.0.1 app.test`).

```bash
export BASE_URL=http://app.test:8088
```

## Стратегии

Скрипт: [`scripts/strategy-demo.sh`](../../scripts/strategy-demo.sh)

| Команда | Что делает |
|---|---|
| `./scripts/strategy-demo.sh rolling` | RollingUpdate v1→v2 на Deployment `task-manager-backend` |
| `./scripts/strategy-demo.sh bluegreen` | Мгновенный switch blue→green + rollback |
| `./scripts/strategy-demo.sh canary` | Ramp 90/10 → 50/50 → 0/100 |
| `./scripts/strategy-demo.sh ab` | Header `X-Audience: beta` → v2 |
| `./scripts/strategy-demo.sh shadow` | RequestMirror v1→v2 |

Логи демо пишутся в `artifacts/strategies/`.
