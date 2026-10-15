# LeaderElectionSystem

A database-backed distributed coordination and leader election service for multi-node Spring Boot applications.

In multi-instance backend deployments, background jobs and singleton tasks often require a single active leader to prevent duplicated work. While solutions like ZooKeeper or etcd are common, they add operational complexity. This project implements reliable coarse-grained coordination directly on MySQL using transactional row-level locks, leased ownership, and monotonic fencing tokens.

---

## Architecture & Mechanics

* **Heartbeat-Based Membership:** Each node registers on boot and periodically pings the `/api/nodes/{id}/heartbeat` endpoint to update its timestamp in the `cluster_node` table.
* **Leased Leader Election:** Nodes contend for a singleton row (`id = 1`) in `leader_lease` using pessimistic locking (`SELECT ... FOR UPDATE`). The winning node claims a 10-second lease. If the leader fails to renew before expiration, competing nodes detect the expired lease and elect a new leader.
* **Distributed Locking:** Provides named lock leases (`distributed_lock`) with owner verification, lease timeouts, and explicit release.
* **Fencing Tokens (Split-Brain Guard):** Time-based leases alone are vulnerable to long Stop-The-World GC pauses or network partitions. If a leader freezes and wakes up after its lease has lapsed, it might attempt an out-of-order write. To prevent data corruption, every lock acquisition increments a monotonic fencing token. Protected resource updates validate that the incoming token matches or exceeds the current token, rejecting stale writes with `HTTP 409 Conflict`.

---

## Data Model

* `cluster_node`: Tracks node identity, status (`ACTIVE`/`INACTIVE`), start time, and last heartbeat.
* `leader_lease`: Singleton lease row holding the active leader node ID, lease expiration, and election epoch.
* `distributed_lock`: Named lock records with owner node ID, expiration timestamp, and fencing token.
* `protected_resource`: Sample resource verifying fencing token ordering on write operations.

---

## Core Endpoints

* **Node Registry:** `POST /api/nodes/{nodeId}/register`, `POST /api/nodes/{nodeId}/heartbeat`, `GET /api/nodes`
* **Leader Election:** `POST /api/leader/claim/{nodeId}`, `POST /api/leader/renew/{nodeId}`, `GET /api/leader`
* **Distributed Locks:** `POST /api/locks/{name}/acquire/{nodeId}`, `POST /api/locks/{name}/renew/{nodeId}`, `POST /api/locks/{name}/release/{nodeId}`
* **Protected Write:** `PUT /api/resources/{key}?lockName={name}&nodeId={nodeId}`

---

## Getting Started

### Prerequisites
* Java 21+
* MySQL 8+ on `localhost:3306` (database: `coordination_hub`, user: `Akhil`, password: `2170`)

### Run Automated Tests
Runs concurrency integration tests verifying lock mutual exclusion and fencing token enforcement:
```bash
./mvnw clean test
```

### Run Locally
```bash
./mvnw spring-boot:run
```

### Multi-Node Cluster (Docker Compose)
Spins up MySQL 8 and three independent application nodes (ports 8081, 8082, 8083):
```bash
docker compose up -d --build
```
