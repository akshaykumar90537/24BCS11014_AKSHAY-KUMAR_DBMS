package com.resolveit.entity;

import com.resolveit.workflow.Priority;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Priority priority;

    @Column(name = "target_hours", nullable = false)
    private int targetHours;

    @Column(name = "reopen_window_days", nullable = false)
    private int reopenWindowDays = 7;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public int getTargetHours() { return targetHours; }
    public void setTargetHours(int targetHours) { this.targetHours = targetHours; }
    public int getReopenWindowDays() { return reopenWindowDays; }
    public void setReopenWindowDays(int reopenWindowDays) { this.reopenWindowDays = reopenWindowDays; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

