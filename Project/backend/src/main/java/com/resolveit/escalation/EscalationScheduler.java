package com.resolveit.escalation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalationService escalationService;

    public EscalationScheduler(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(fixedDelayString = "${app.sla.scan-delay-ms:60000}")
    public void scanOverdueComplaints() {
        int count = escalationService.escalateOverdueComplaints();
        if (count > 0) {
            log.info("SLA escalation scan marked {} complaint(s) as escalated", count);
        }
    }
}

