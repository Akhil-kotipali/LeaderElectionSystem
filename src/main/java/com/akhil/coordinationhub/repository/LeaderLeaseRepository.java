
package com.akhil.coordinationhub.repository;

import com.akhil.coordinationhub.entity.LeaderLease;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeaderLeaseRepository
        extends JpaRepository<LeaderLease, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LeaderLease l WHERE l.id = :id")
    Optional<LeaderLease> findByIdForUpdate(@Param("id") Integer id);
}
