package com.resolveit.workflow;

import com.resolveit.common.Constants;
import com.resolveit.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransitionPolicyTest {

    private final TransitionPolicy policy = new TransitionPolicy();

    @Test
    void allowsValidLifecycleTransitions() {
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW, Constants.ROLE_ADMIN));
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.ASSIGNED, Constants.ROLE_MANAGER));
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, Constants.ROLE_EMPLOYEE));
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, Constants.ROLE_EMPLOYEE));
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED, Constants.ROLE_USER));
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.RESOLVED, ComplaintStatus.REOPENED, Constants.ROLE_USER));
    }

    @Test
    void rejectsInvalidTransitionsWithConflictException() {
        assertThrows(ConflictException.class,
                () -> policy.validateTransition(ComplaintStatus.SUBMITTED, ComplaintStatus.RESOLVED, Constants.ROLE_EMPLOYEE));
        assertThrows(ConflictException.class,
                () -> policy.validateTransition(ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS, Constants.ROLE_USER));
        assertThrows(ConflictException.class,
                () -> policy.validateTransition(ComplaintStatus.CLOSED, ComplaintStatus.REOPENED, Constants.ROLE_USER));
    }

    @Test
    void allowsAdminToReopenClosedComplaintByPolicy() {
        assertDoesNotThrow(() -> policy.validateTransition(ComplaintStatus.CLOSED, ComplaintStatus.REOPENED, Constants.ROLE_ADMIN));
    }
}

