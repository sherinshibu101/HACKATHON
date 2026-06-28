package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.IssueMedia;
import com.communityheroai.issue.entity.IssueMediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueMediaRepository extends JpaRepository<IssueMedia, Long> {
    List<IssueMedia> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    long countByIssueIdAndMediaType(Long issueId, IssueMediaType mediaType);
}
