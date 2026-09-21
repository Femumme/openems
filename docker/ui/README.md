# OpenEMS Edge UI

- Pulled from `ghcr.io/mummeenergie/openems-ui-edge:${OPENEMS_VERSION:-latest}`.
- UI nginx proxies `/openems-edge` to Compose service `edge:8075`.
- Browser uses UI host for WebSocket access; no Docker-internal hostname is needed.

Run from the repository root:

```bash
cp docker/.env.example docker/.env
# Optionally set OPENEMS_VERSION to a published tag.
docker compose --env-file docker/.env -f docker/docker-compose.yml pull
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d
```

Open `http://<Docker-host-IP>/` in your browser.
