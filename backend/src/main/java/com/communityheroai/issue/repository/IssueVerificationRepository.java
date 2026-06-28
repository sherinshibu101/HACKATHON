package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.IssueVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueVerificationRepository extends JpaRepository<IssueVerification, Long> {
    long countByIssueId(Long issueId);
    List<IssueVerification> findByIssueIdOrderByCreatedAtDesc(Long issueId);
    void deleteByIssueId(Long issueId);
}
