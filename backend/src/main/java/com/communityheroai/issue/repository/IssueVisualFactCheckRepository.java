package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.IssueVisualFactCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueVisualFactCheckRepository extends JpaRepository<IssueVisualFactCheck, Long> {
    Optional<IssueVisualFactCheck> findTopByIssueIdOrderByCreatedAtDesc(Long issueId);
    List<IssueVisualFactCheck> findByIssueIdOrderByCreatedAtDesc(Long issueId);
    void deleteByIssueId(Long issueId);
}
