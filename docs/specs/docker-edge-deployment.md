# Docker Edge Deployment

## Overview

Replace the `openems-build` git-based deployment with Docker images published to GitHub Container Registry (ghcr.io) and a Docker Compose file for self-starting edge deployments on Raspberry Pis. Introduces a composite version scheme (`<openems-version>-<build-number>`) to track both upstream base and fork iterations.

## User Stories

### Story 1 — Automated Image Publishing

As a developer, I want Docker images for Edge and UI automatically built and published to ghcr.io on every push to main, so that production-ready images are always available without manual artifact management.

#### Acceptance Criteria

- Given a push to the `main` branch, when the GitHub Actions workflow runs, then Docker images for `openems-edge` and `openems-ui-edge` are built and pushed to `ghcr.io/<repository-owner>/`.
- Given the workflow runs, when the build completes, then images are tagged with both the composite version (e.g. `2025.10.0-42`) and `latest`.
- Given the workflow runs, when images are built, then they target `linux/amd64` and `linux/arm64` platforms (Pis use arm64).
- Given a push to a non-main branch (e.g. a PR), when the workflow triggers, then no images are published to ghcr.io.
- Given the workflow fails at the Java build step, when the UI build has not started, then no images are pushed for either component.

### Story 2 — Composite Versioning

As an operator, I want image tags to encode both the upstream OpenEMS version and an incrementing fork build number, so that I can identify exactly which base version and which fork iteration is running on any edge.

#### Acceptance Criteria

- Given the OpenEMS version is `2025.10.0` and this is the 42nd workflow run, when the images are tagged, then the version tag is `2025.10.0-42`.
- Given the OpenEMS version in the source code changes (e.g. after an upstream merge), when the next workflow runs, then the new base version is reflected in the tag while the build number continues incrementing.
- Given a published image, when an operator runs `docker inspect` or checks ghcr.io, then the composite version is visible as both a Docker tag and an OCI label.
- Given two images with tags `2025.10.0-41` and `2025.10.0-42`, when comparing them, then the higher build number is the newer build regardless of base version changes.

### Story 3 — Production Docker Compose

As an operator, I want a ready-to-deploy Docker Compose file that runs Edge and UI from ghcr.io images with persistent config, so that I can deploy to a Pi with a single `docker compose up -d`.

#### Acceptance Criteria

- Given a Pi with Docker and Docker Compose installed, when the operator runs `docker compose up -d` with the provided compose file, then the Edge and UI containers start and restart automatically on reboot or crash.
- Given the compose file, when the Edge container starts, then the OSGi config directory is persisted via a host-mounted volume that survives container recreation.
- Given the compose file, when the Edge container starts, then the Edge data directory is persisted via a host-mounted volume.
- Given the compose file, when the UI container starts, then it connects to the Edge container's WebSocket endpoint without manual network configuration.
- Given the compose file references `ghcr.io/<repository-owner>/openems-edge` and `ghcr.io/<repository-owner>/openems-ui-edge`, when the operator runs `docker compose pull`, then the latest images are pulled from the fork's registry.
- Given a running deployment, when the operator wants to update, then `docker compose pull && docker compose up -d` replaces containers with new images while preserving config and data volumes.
- Given the Edge container crashes or the Pi reboots, when Docker starts, then both containers automatically restart without operator intervention.

## Dependencies

- External: GitHub Container Registry (ghcr.io) for image hosting.
- External: GitHub Actions for CI/CD pipeline.
- Behavioral: The existing Dockerfiles in `tools/docker/` (edge, UI) already produce working images — this spec builds on that capability.
- Behavioral: The OpenEMS version is defined in Java source (`OpenemsConstants.java`) and must be extractable during CI.

## Constraints

- Images must build for `linux/arm64` (Raspberry Pi) and `linux/amd64` (dev/CI).
- The workflow must not publish to Docker Hub or the upstream `openems/` namespace — only to the fork's ghcr.io namespace.
- The compose file must use bind mounts (not named volumes) for config and data, so operators can directly access and backup config files on the host filesystem.
- The compose file must be self-contained: no `.env` file required for basic operation; the image registry and tag should have sensible defaults.

## Out of Scope

- Config corruption prevention and backup strategy (separate spec: `edge-config-resilience`).
- Backend deployment (this spec covers Edge + UI only).
- Watchtower or automated pull-based update mechanisms.
- Docker Hub publishing.
- SSL/TLS certificate management for the UI container.
- Multi-edge fleet management or orchestration tooling.
- Removal of the existing `openems-build` repo workflow or legacy deploy tasks.
