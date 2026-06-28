package com.communityheroai.issue.repository;

import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    long countByStatus(IssueStatus status);
    List<Issue> findByCategory(IssueCategory category);
    List<Issue> findByCategoryAndStatusNot(IssueCategory category, IssueStatus status);
    List<Issue> findByStatusNotOrderByImpactScoreDesc(IssueStatus status);
    List<Issue> findByWard(String ward);

    @Query("select i.category as category, count(i) as count from Issue i group by i.category")
    List<Object[]> countByCategory();

    @Query("select i.ward as ward, count(i) as count from Issue i group by i.ward")
    List<Object[]> countByWard();
}
