# Warden
A self healing and Data aware pipeline engine

## Why I'm building this

I kept running into the same annoying problem with pipeline tools like Airflow. They only care whether a task *ran*, not whether it did the right thing. If the task exits without an error, it goes green, and that's it.

But a task can "succeed" and still produce garbage. An empty table. A column that's suddenly all nulls. Duplicate rows. Yesterday's data sitting in today's partition. Nothing fails, nobody notices, and the bad data quietly ends up in dashboards, reports, maybe even an ML model. Usually someone finds out a few days later (and they're not happy about it).

Then comes the fun part: figuring out what's affected. Most of the time people just rerun the whole pipeline to be safe, which is slow and wasteful.

## What Warden does differently

In Warden, data is the main thing, not just the tasks. Each task says which datasets it reads and writes, and attaches a **data contract** to its output: the schema, how many rows are expected, how many nulls are okay, what should be unique, and how fresh the data needs to be.

Warden sits between every step and checks that contract. If the output doesn't look right, it doesn't move forward.

## Features

- **Lineage tracking** – Warden keeps a live graph of which datasets feed into which, down to individual partitions (like a single day of data). So when something breaks, it already knows what's impacted.
- **Validation + quarantine** – after every task, the output gets checked against its contract. If it fails, the data goes into quarantine and downstream tasks are paused. Bad data stays contained.
- **Recompute only what's needed** – when data gets fixed or late data shows up, Warden reruns just the tasks and partitions that depend on it. Not the entire DAG.
- **AI failure diagnosis** – this is the part I'm most excited about. When something fails, a local LLM (running through Ollama, completely offline) looks at the error, logs, data stats and lineage, and explains in plain English what probably went wrong and how to fix it.
- **Pipelines in Java or YAML** – you can write pipelines with a fluent Java DSL, which means a lot of mistakes get caught at compile time instead of at 2 AM.
- **Distributed execution** – scheduler with failover, workers you can scale out, retries with backoff, and concurrency limits per task.
- **Dashboard** – DAG and lineage graphs, health of each dataset, a quarantine screen where you can approve or reject bad batches, run history and live logs.
- **Monitoring** – Prometheus metrics, Grafana dashboards, and alerts when a contract is violated or data misses its freshness deadline.

## Tech stack

Everything runs locally. No cloud account needed, no surprise bills.

- Java 21 (using virtual threads) + Spring Boot
- PostgreSQL for metadata and lineage
- RabbitMQ or Kafka for distributing tasks
- MinIO (S3-compatible) for logs and quarantined data
- Ollama for the local LLM
- React for the dashboard
- Prometheus + Grafana
- Docker Compose, plus k3d if you want to try Kubernetes execution
