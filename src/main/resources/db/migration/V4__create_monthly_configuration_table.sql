CREATE TABLE monthly_configuration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  config_month INT NOT NULL,
  config_year INT NOT NULL,
  working_days INT NOT NULL,
  leaves INT NOT NULL DEFAULT 0,
  public_holidays INT NOT NULL DEFAULT 0,
  exception_days INT NOT NULL DEFAULT 0,
  required_office_days INT NOT NULL DEFAULT 0,
  manual_checkins INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE (employee_id, config_month, config_year)
);
