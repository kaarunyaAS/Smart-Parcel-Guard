package com.smartparcelguard.command;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandHistoryRepository extends JpaRepository<CommandHistory, Long> {
}
