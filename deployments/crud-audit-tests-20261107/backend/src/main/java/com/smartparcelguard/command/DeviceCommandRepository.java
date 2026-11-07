package com.smartparcelguard.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    List<DeviceCommand> findByDeviceIdAndStatus(Long deviceId, CommandStatus status);
}
