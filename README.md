# LeaderElectionSystem

A simple distributed leader election and lock coordination system for multi-node setups using Spring Boot and MySQL.

In a cluster with multiple nodes running at the same time, this service lets them elect a leader to handle background tasks, while other nodes act as workers. Nodes continuously send heartbeats to show they are active. MySQL handles coordination using row-level locking, lease timers, and fencing tokens to prevent split-brain issues.

## How it works

- **Heartbeats:** Nodes register on startup and send periodic heartbeats to maintain active status.
- **Leader Lease:** Nodes compete to claim a singleton lease row in MySQL. The winner becomes leader for 10 seconds. The leader continuously renews its lease; if it crashes, the lease expires and another node claims it.
- **Distributed Locks:** Supports named locks with owner validation and auto-expiring leases.
- **Fencing Tokens:** Every lock acquisition increments a monotonic token. Writes to protected resources validate the token to reject stale writes from paused nodes (e.g. long GC pauses).

## Getting Started

### Prerequisites
- Java 21+
- MySQL running on `localhost:3306` with database `coordination_hub` (user: `Akhil`, password: `2170`)

### Run Tests
```bash
./mvnw clean test
```

### Run Locally
```bash
./mvnw spring-boot:run
```

### Docker Compose
To spin up a 3-node cluster with MySQL:
```bash
docker compose up -d --build
```
