package com.communityheroai.ledger.repository;

import com.communityheroai.ledger.entity.CivicLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CivicLedgerRepository extends JpaRepository<CivicLedgerEntry, Long> {
    Optional<CivicLedgerEntry> findTopByOrderByIdDesc();
    Optional<CivicLedgerEntry> findTopByAggregateTypeAndAggregateIdOrderByIdDesc(String aggregateType, Long aggregateId);
    List<CivicLedgerEntry> findAllByOrderByIdAsc();
}
