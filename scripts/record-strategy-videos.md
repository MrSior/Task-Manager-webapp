# Как записать видео стратегий

На macOS достаточно QuickTime / Cmd+Shift+5 (запись экрана). В кадре: терминал с демо + (опционально) второй терминал с `kubectl logs -f`.

```bash
# 1) Envoy port-forward
ENVOY_SVC=$(kubectl -n envoy-gateway-system get svc \
  -l gateway.envoyproxy.io/owning-gateway-name=task-manager-gw \
  -o jsonpath='{.items[0].metadata.name}')
kubectl -n envoy-gateway-system port-forward "svc/${ENVOY_SVC}" 8088:80

# 2) В другом терминале — старт записи экрана, затем:
export BASE_URL=http://app.test:8088
./scripts/strategy-demo.sh rolling     # → video-rolling.mp4
./scripts/strategy-demo.sh bluegreen   # → video-bluegreen.mp4
./scripts/strategy-demo.sh canary      # → video-canary.mp4
./scripts/strategy-demo.sh ab          # → video-ab.mp4
./scripts/strategy-demo.sh shadow      # → video-shadow.mp4
```

Положите файлы в корень репозитория (или укажите ссылки в `Отчёт.md`):

- `video-rolling.mp4`
- `video-bluegreen.mp4`
- `video-canary.mp4`
- `video-ab.mp4`
- `video-shadow.mp4`

Готовые логи прогона уже лежат в [`artifacts/strategies/`](../artifacts/strategies/).
