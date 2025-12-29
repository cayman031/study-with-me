CREATE TABLE member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_member PRIMARY KEY (id),
  CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study (
  id BIGINT NOT NULL AUTO_INCREMENT,
  leader_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  description TEXT NOT NULL,
  category VARCHAR(50) NULL,
  level VARCHAR(20) NULL,
  is_online TINYINT(1) NOT NULL,
  region VARCHAR(50) NULL,
  recruit_end_at DATETIME(6) NOT NULL,
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  admin_reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_study PRIMARY KEY (id),
  CONSTRAINT fk_study_leader FOREIGN KEY (leader_id) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_study_leader_id ON study (leader_id);
CREATE INDEX idx_study_status ON study (status);
CREATE INDEX idx_study_recruit_end_at ON study (recruit_end_at);

CREATE TABLE capacity (
  study_id BIGINT NOT NULL,
  total INT NOT NULL,
  remain INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_capacity PRIMARY KEY (study_id),
  CONSTRAINT fk_capacity_study FOREIGN KEY (study_id) REFERENCES study(id),
  CONSTRAINT chk_capacity_remain CHECK (remain >= 0),
  CONSTRAINT chk_capacity_total CHECK (remain <= total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE application (
  id BIGINT NOT NULL AUTO_INCREMENT,
  study_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_application PRIMARY KEY (id),
  CONSTRAINT uk_application_study_member UNIQUE (study_id, member_id),
  CONSTRAINT fk_application_study FOREIGN KEY (study_id) REFERENCES study(id),
  CONSTRAINT fk_application_member FOREIGN KEY (member_id) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_application_study_status ON application (study_id, status);
CREATE INDEX idx_application_member_status ON application (member_id, status);

CREATE TABLE review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  study_id BIGINT NOT NULL,
  application_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_review PRIMARY KEY (id),
  CONSTRAINT uk_review_application UNIQUE (application_id),
  CONSTRAINT fk_review_study FOREIGN KEY (study_id) REFERENCES study(id),
  CONSTRAINT fk_review_application FOREIGN KEY (application_id) REFERENCES application(id),
  CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES member(id),
  CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_study_id ON review (study_id);

CREATE TABLE idempotency_key (
  id BIGINT NOT NULL AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  endpoint VARCHAR(100) NOT NULL,
  idempotency_key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  response_body TEXT NOT NULL,
  http_status INT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_idempotency_key PRIMARY KEY (id),
  CONSTRAINT uk_idempotency_key UNIQUE (member_id, endpoint, idempotency_key),
  CONSTRAINT fk_idempotency_member FOREIGN KEY (member_id) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_idempotency_expires_at ON idempotency_key (expires_at);

CREATE TABLE audit_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_type VARCHAR(20) NOT NULL,
  actor_id BIGINT NULL,
  action VARCHAR(50) NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_id BIGINT NOT NULL,
  before_state VARCHAR(20) NULL,
  after_state VARCHAR(20) NULL,
  payload_json TEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_event PRIMARY KEY (id),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_target ON audit_event (target_type, target_id);
CREATE INDEX idx_audit_actor_id ON audit_event (actor_id);

CREATE TABLE report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  processed_by BIGINT NULL,
  processed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_report PRIMARY KEY (id),
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member(id),
  CONSTRAINT fk_report_processed_by FOREIGN KEY (processed_by) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_report_status ON report (status);
CREATE INDEX idx_report_target ON report (target_type, target_id);

CREATE TABLE sanction (
  id BIGINT NOT NULL AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6) NULL,
  status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_sanction PRIMARY KEY (id),
  CONSTRAINT fk_sanction_member FOREIGN KEY (member_id) REFERENCES member(id),
  CONSTRAINT fk_sanction_created_by FOREIGN KEY (created_by) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sanction_member_id ON sanction (member_id);
CREATE INDEX idx_sanction_status ON sanction (status);
