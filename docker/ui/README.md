# OpenEMS UI (local build)

- Built with Angular config `openems,openems-edge-docker`.
- Runtime env `UI_WEBSOCKET` defines the websocket URL. In compose it's set to `ws://edge:8075`.

Run from the `docker` directory:

```bash
docker compose up -d
```

Open `http://localhost/` in your browser.




