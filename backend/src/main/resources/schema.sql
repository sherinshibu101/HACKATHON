CREATE TABLE IF NOT EXISTS issues (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  reporter_name VARCHAR(100) NULL,
  reporter_email VARCHAR(254) NULL,
  description TEXT NOT NULL,
  category VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  severity VARCHAR(50) NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  ward VARCHAR(100) NOT NULL,
  locality VARCHAR(100) NOT NULL,
  country VARCHAR(100) NULL,
  state VARCHAR(100) NULL,
  district VARCHAR(100) NULL,
  city VARCHAR(100) NULL,
  postal_code VARCHAR(20) NULL,
  formatted_address VARCHAR(1000) NULL,
  location_accuracy_meters DOUBLE NULL,
  location_source VARCHAR(20) NULL,
  recommended_department VARCHAR(150) NULL,
  impact_score INT NULL,
  risk_explanation TEXT NULL,
  suggested_action TEXT NULL,
  complaint_draft TEXT NULL,
  escalation_message TEXT NULL,
  resolution_urgency VARCHAR(255) NULL,
  ai_generated_at DATETIME NULL,
  authority_email_sent_at DATETIME NULL,
  authority_email_recipient VARCHAR(254) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS issue_verifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  verifier_name VARCHAR(100) NOT NULL,
  verifier_email VARCHAR(254) NULL,
  comment VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_issue_verification_issue (issue_id),
  CONSTRAINT fk_issue_verification_issue FOREIGN KEY (issue_id) REFERENCES issues(id)
);

CREATE TABLE IF NOT EXISTS issue_media (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  media_type VARCHAR(20) NOT NULL,
  media_url VARCHAR(500) NOT NULL,
  thumbnail_url VARCHAR(500) NULL,
  storage_key VARCHAR(100) NOT NULL,
  original_filename VARCHAR(255) NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  processing_status VARCHAR(20) NOT NULL,
  validation_status VARCHAR(30) NULL,
  validation_confidence INT NULL,
  validation_summary VARCHAR(1000) NULL,
  validation_labels TEXT NULL,
  validated_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_issue_media_issue (issue_id),
  CONSTRAINT fk_issue_media_issue FOREIGN KEY (issue_id) REFERENCES issues(id)
);

CREATE TABLE IF NOT EXISTS issue_email_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  recipient VARCHAR(254) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_issue_email_log_issue (issue_id),
  CONSTRAINT fk_issue_email_log_issue FOREIGN KEY (issue_id) REFERENCES issues(id)
);

CREATE TABLE IF NOT EXISTS issue_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  actor_name VARCHAR(100) NOT NULL,
  actor_type VARCHAR(30) NOT NULL,
  note VARCHAR(1000) NOT NULL,
  evidence_url VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_issue_status_history_issue (issue_id),
  CONSTRAINT fk_issue_status_history_issue FOREIGN KEY (issue_id) REFERENCES issues(id)
);

CREATE TABLE IF NOT EXISTS issue_visual_fact_checks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  issue_media_id BIGINT NULL,
  status VARCHAR(30) NOT NULL,
  verification_result VARCHAR(30) NULL,
  confidence_score INT NULL,
  baseline_image_url VARCHAR(1000) NULL,
  user_image_url VARCHAR(500) NULL,
  reasoning_report TEXT NULL,
  risk_flags TEXT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_visual_fact_check_issue (issue_id),
  CONSTRAINT fk_visual_fact_check_issue FOREIGN KEY (issue_id) REFERENCES issues(id),
  CONSTRAINT fk_visual_fact_check_media FOREIGN KEY (issue_media_id) REFERENCES issue_media(id)
);

CREATE TABLE IF NOT EXISTS civic_audit_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type VARCHAR(60) NOT NULL,
  aggregate_type VARCHAR(60) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  actor_name VARCHAR(120) NOT NULL,
  payload TEXT NOT NULL,
  previous_hash VARCHAR(64) NOT NULL,
  entry_hash VARCHAR(64) NOT NULL,
  hash_algorithm VARCHAR(20) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_civic_ledger_created (created_at),
  INDEX idx_civic_ledger_aggregate (aggregate_type, aggregate_id)
);

CREATE TABLE IF NOT EXISTS agent_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  trigger_type VARCHAR(30) NOT NULL,
  model VARCHAR(100) NOT NULL,
  citizen_summary TEXT NULL,
  admin_recommendation TEXT NULL,
  recommended_next_action TEXT NULL,
  proposed_department VARCHAR(180) NULL,
  proposed_priority VARCHAR(30) NULL,
  proposed_status VARCHAR(30) NULL,
  target_resolution_hours INT NULL,
  confidence INT NULL,
  requires_human_approval BOOLEAN NOT NULL,
  failure_message VARCHAR(1000) NULL,
  reviewed_by VARCHAR(100) NULL,
  review_note VARCHAR(1000) NULL,
  started_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  reviewed_at DATETIME NULL,
  INDEX idx_agent_run_issue (issue_id),
  CONSTRAINT fk_agent_run_issue FOREIGN KEY (issue_id) REFERENCES issues(id)
);

CREATE TABLE IF NOT EXISTS agent_run_steps (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_run_id BIGINT NOT NULL,
  step_number INT NOT NULL,
  tool_name VARCHAR(80) NOT NULL,
  action_summary VARCHAR(500) NOT NULL,
  observation_summary VARCHAR(1500) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_agent_step_run (agent_run_id),
  CONSTRAINT fk_agent_step_run FOREIGN KEY (agent_run_id) REFERENCES agent_runs(id)
);
