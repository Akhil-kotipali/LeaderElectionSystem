
package com.akhil.coordinationhub.repository;

import com.akhil.coordinationhub.entity.ClusterNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterNodeRepository
        extends JpaRepository<ClusterNode, String> {
}
