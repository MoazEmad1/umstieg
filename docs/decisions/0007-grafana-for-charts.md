# 0007: Grafana for charts

Status: Accepted · 2026-10-30

## Context
Phase 1 needs one simple chart showing delays over time and feed health. Phase 4 will need monitoring dashboards.

## Decision
I add Grafana to docker-compose. The datasource and dashboards are provisioned from files in `infra/grafana/provisioning/`, not built by hand. The dashboard credits "Data: VBB, CC BY 4.0".

## Alternatives
- A Spring endpoint with Chart.js: no extra service, but more code for one chart, and not reusable later.
- A Jupyter notebook: good for analysis, but not a live view.

## Consequences
One more container, and I can reuse it for Phase 4 metrics.
