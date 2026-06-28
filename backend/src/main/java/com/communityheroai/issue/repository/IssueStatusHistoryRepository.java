package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Long> {
    List<IssueStatusHistory> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    void deleteByIssueId(Long issueId);
}
