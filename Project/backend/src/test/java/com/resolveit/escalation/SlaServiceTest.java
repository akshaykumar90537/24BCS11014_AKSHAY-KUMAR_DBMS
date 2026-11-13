package com.resolveit.escalation;

import com.resolveit.entity.SlaPolicy;
import com.resolveit.workflow.Priority;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SlaServiceTest {

    @Test
    void calculatesDueAtFromConfiguredPolicy() {
        SlaPolicyRepository repo = Mockito.mock(SlaPolicyRepository.class);
        SlaPolicy highPolicy = new SlaPolicy();
        highPolicy.setPriority(Priority.HIGH);
        highPolicy.setTargetHours(24);
        highPolicy.setReopenWindowDays(10);
        when(repo.findByPriority(Priority.HIGH)).thenReturn(Optional.of(highPolicy));

        SlaService slaService = new SlaService(repo);
        Instant start = Instant.parse("2026-11-10T10:00:00Z");

        assertEquals(start.plus(24, ChronoUnit.HOURS), slaService.calculateDueAt(Priority.HIGH, start));
        assertEquals(10, slaService.getReopenWindowDays(Priority.HIGH));
    }

    @Test
    void fallsBackToDefaultsWhenPolicyMissing() {
        SlaPolicyRepository repo = Mockito.mock(SlaPolicyRepository.class);
        when(repo.findByPriority(Priority.CRITICAL)).thenReturn(Optional.empty());

        SlaService slaService = new SlaService(repo);
        Instant start = Instant.parse("2026-11-10T10:00:00Z");

        assertEquals(start.plus(4, ChronoUnit.HOURS), slaService.calculateDueAt(Priority.CRITICAL, start));
    }
}

