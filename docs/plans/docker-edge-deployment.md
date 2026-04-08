# Implementation Plan: Docker Edge Deployment

Spec: docs/specs/docker-edge-deployment.md

## Tasks

### T1 — Rewrite GitHub Actions Docker workflow for fork-specific publishing

- **Files**: `.github/workflows/docker.yml`
- **Depends on**: none
- **Description**: Replace the existing Docker workflow (which publishes 4 images to Docker Hub + upstream ghcr.io namespace) with a fork-specific workflow that only publishes `openems-edge` and `openems-ui-edge` to `ghcr.io/${{ github.repository_owner }}/`.

  Key changes to the existing workflow:
  - **Trigger**: Keep `push` on `main` and `workflow_dispatch`. Remove `develop` branch and tag triggers.
  - **Matrix**: Reduce from 4 images (`edge`, `backend`, `ui-edge`, `ui-backend`) to 2 (`edge`, `ui-edge`). Keep existing Dockerfile paths (`tools/docker/edge/Dockerfile`, `tools/docker/ui/Dockerfile`).
  - **Version extraction**: Add a step before the matrix that parses `io.openems.common/src/io/openems/common/OpenemsConstants.java` for `VERSION_MAJOR` (currently `2025`), `VERSION_MINOR` (`10`), `VERSION_PATCH` (`0`). Compose `OPENEMS_VERSION` as `$MAJOR.$MINOR.$PATCH`. The composite tag is `$OPENEMS_VERSION-${{ github.run_number }}`.
  - **Registry**: Remove Docker Hub login step (`secrets.DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN`). Keep only ghcr.io login.
  - **Image names**: Change from `openems/${{ matrix.image }}` + `ghcr.io/openems/${{ matrix.image }}` to only `ghcr.io/${{ github.repository_owner }}/openems-${{ matrix.image }}` (producing `openems-edge` and `openems-ui-edge`).
  - **Tags**: Replace current metadata-action tag rules with: `type=raw,value=$OPENEMS_VERSION-${{ github.run_number }}` and `type=raw,value=latest`. Both only on `main`.
  - **OCI labels**: Add `org.opencontainers.image.version=$OPENEMS_VERSION-${{ github.run_number }}` via the metadata-action labels output (already partially handled by metadata-action defaults).
  - **Push guard**: Set `push: ${{ github.ref == 'refs/heads/main' }}` in build-push-action to prevent publishing from non-main contexts (e.g., workflow_dispatch on other branches).
  - **Fail-fast**: Keep `fail-fast: true` on the matrix strategy so a failed edge build prevents ui-edge from publishing.
  - **Multi-platform**: Keep existing `linux/amd64, linux/arm64` platform config.
  - Keep QEMU + Buildx setup steps unchanged.

- **Acceptance criteria**:
  - [ ] Workflow triggers on `push` to `main` and `workflow_dispatch` only — no `develop` or tag triggers
  - [ ] Matrix contains only `edge` and `ui-edge` entries
  - [ ] No Docker Hub login step; only ghcr.io login remains
  - [ ] Image names resolve to `ghcr.io/<owner>/openems-edge` and `ghcr.io/<owner>/openems-ui-edge`
  - [ ] Images tagged with composite version `<major>.<minor>.<patch>-<run_number>` and `latest`
  - [ ] Version extracted from `OpenemsConstants.java` using grep/sed in a shell step
  - [ ] `push` is conditional on `github.ref == 'refs/heads/main'`
  - [ ] `fail-fast: true` prevents partial publishes
  - [ ] Platforms remain `linux/amd64, linux/arm64`

### T2 — Create production Docker Compose for Pi deployment

- **Files**: `docker/docker-compose.yml`
- **Depends on**: none
- **Description**: Rewrite the existing `docker/docker-compose.yml` (currently a local-build compose file with `build:` directives and `openems_edge:local` tags) into a production pull-based deployment file for Raspberry Pis.

  Key changes to the existing file:
  - **Remove** all `build:` blocks — this file is for deploying pre-built images, not local builds.
  - **Image references**: Use `ghcr.io/mumme-it/openems-edge:latest` and `ghcr.io/mumme-it/openems-ui-edge:latest` as defaults. Use YAML `x-` extension or inline comment to document how to override with a specific version tag.
  - **Edge service**:
    - `image: ghcr.io/mumme-it/openems-edge:latest`
    - `container_name: openems-edge`
    - `restart: unless-stopped`
    - Bind mounts (existing pattern is correct): `./edge/config:/var/opt/openems/config:rw` and `./edge/data:/var/opt/openems/data:rw`
    - Ports: `8080:8080` (Felix console), `8075:8075` (WebSocket), `502:502` (Modbus TCP) — matching existing
  - **UI service**:
    - `image: ghcr.io/mumme-it/openems-ui-edge:latest`
    - `container_name: openems-ui`
    - `restart: unless-stopped`
    - `environment: UI_WEBSOCKET=ws://edge:8075` — uses Docker Compose DNS name `edge` (existing pattern)
    - Ports: `80:80`, `443:443`
    - `depends_on: [edge]`
  - **Remove** the `volumes:` top-level section (no named volumes — spec requires bind mounts only).
  - **Self-contained**: No `.env` file required. All defaults are inline.

- **Acceptance criteria**:
  - [ ] No `build:` directives — images are pulled from ghcr.io
  - [ ] Edge config persisted via bind mount at `./edge/config:/var/opt/openems/config`
  - [ ] Edge data persisted via bind mount at `./edge/data:/var/opt/openems/data`
  - [ ] Both services have `restart: unless-stopped`
  - [ ] UI `UI_WEBSOCKET` env var points to `ws://edge:8075` (Docker DNS)
  - [ ] UI `depends_on` edge service
  - [ ] No `.env` file required — defaults are inline
  - [ ] `docker compose pull && docker compose up -d` updates containers while preserving bind-mounted data
  - [ ] No named volumes section at top level

## Execution Order

Parallel: [T1, T2] — no dependencies between tasks
