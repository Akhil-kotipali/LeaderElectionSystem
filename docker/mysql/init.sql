CREATE TABLE cluster_node (
    node_id VARCHAR(100) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    last_heartbeat TIMESTAMP(6) NULL,
    started_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT chk_cluster_node_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB;

CREATE TABLE leader_lease (
    id INT PRIMARY KEY,
    leader_node_id VARCHAR(100) NULL,
    lease_until TIMESTAMP(6) NULL,
    epoch BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_leader_singleton CHECK (id = 1)
) ENGINE = InnoDB;

INSERT INTO leader_lease (id, leader_node_id, lease_until, epoch)
VALUES (1, NULL, NULL, 0);

CREATE TABLE distributed_lock (
    lock_name VARCHAR(120) PRIMARY KEY,
    owner_node_id VARCHAR(100) NULL,
    lease_until TIMESTAMP(6) NULL,
    fencing_token BIGINT NOT NULL DEFAULT 0
) ENGINE = InnoDB;

CREATE TABLE protected_resource (
    resource_key VARCHAR(120) PRIMARY KEY,
    value_text VARCHAR(500) NOT NULL,
    last_fencing_token BIGINT NOT NULL DEFAULT 0
) ENGINE = InnoDB;
