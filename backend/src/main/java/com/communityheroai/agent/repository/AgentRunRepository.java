package com.communityheroai.agent.repository;

import com.communityheroai.agent.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    List<AgentRun> findByIssueIdOrderByStartedAtDesc(Long issueId);
    Optional<AgentRun> findFirstByIssueIdOrderByStartedAtDesc(Long issueId);
}
