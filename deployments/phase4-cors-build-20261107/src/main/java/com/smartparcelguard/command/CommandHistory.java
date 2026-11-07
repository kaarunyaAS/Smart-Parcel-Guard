package com.smartparcelguard.command;

import com.smartparcelguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "command_history")
public class CommandHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "command_id", nullable = false)
    private DeviceCommand command;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private CommandStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private CommandStatus toStatus;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public DeviceCommand getCommand() {
        return command;
    }

    public void setCommand(DeviceCommand command) {
        this.command = command;
    }

    public CommandStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(CommandStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public CommandStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(CommandStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
