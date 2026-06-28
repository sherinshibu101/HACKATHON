package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.IssueEmailLog;
import com.communityheroai.issue.entity.IssueEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueEmailLogRepository extends JpaRepository<IssueEmailLog, Long> {
    Optional<IssueEmailLog> findTopByIssueIdAndStatusOrderByCreatedAtDesc(Long issueId, IssueEmailStatus status);
    void deleteByIssueId(Long issueId);
}
