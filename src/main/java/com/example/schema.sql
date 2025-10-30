CREATE DATABASE IF NOT EXISTS chat_db;
USE chat_db;

CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) UNIQUE,
  password VARCHAR(200),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
  message_id INT AUTO_INCREMENT PRIMARY KEY,
  sender VARCHAR(100),
  receiver VARCHAR(100),
  content TEXT,
  sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender) REFERENCES users(username) ON DELETE CASCADE,
  FOREIGN KEY (receiver) REFERENCES users(username) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_groups (
  group_id INT AUTO_INCREMENT PRIMARY KEY,
  group_name VARCHAR(100) UNIQUE NOT NULL,
  created_by VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(username) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS group_members (
  group_id INT,
  username VARCHAR(100),
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  role ENUM('member', 'admin') DEFAULT 'member',
  PRIMARY KEY (group_id, username),
  FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE,
  FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_messages (
  message_id INT AUTO_INCREMENT PRIMARY KEY,
  group_id INT,
  sender VARCHAR(100),
  content TEXT,
  sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE,
  FOREIGN KEY (sender) REFERENCES users(username) ON DELETE CASCADE
);