package com.resolveit.workflow;

import com.resolveit.common.Constants;
import com.resolveit.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TransitionPolicy {

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ComplaintStatus.SUBMITTED, EnumSet.of(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED, ComplaintStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.UNDER_REVIEW, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED, ComplaintStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.ASSIGNED, EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.UNDER_REVIEW));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, EnumSet.of(ComplaintStatus.RESOLVED, ComplaintStatus.ASSIGNED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.RESOLVED, EnumSet.of(ComplaintStatus.CLOSED, ComplaintStatus.REOPENED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.REOPENED, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.CLOSED, EnumSet.noneOf(ComplaintStatus.class));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.REJECTED, EnumSet.noneOf(ComplaintStatus.class));
    }

    public boolean isTransitionAllowed(ComplaintStatus from, ComplaintStatus to, String actorRole) {
        if (from == null || to == null) {
            return false;
        }
        if ((from == ComplaintStatus.CLOSED || from == ComplaintStatus.REJECTED)
                && to == ComplaintStatus.REOPENED
                && Constants.ROLE_ADMIN.equals(actorRole)) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ComplaintStatus.class)).contains(to);
    }

    public void validateTransition(ComplaintStatus from, ComplaintStatus to, String actorRole) {
        if (!isTransitionAllowed(from, to, actorRole)) {
            throw new ConflictException(
                    "INVALID_STATUS_TRANSITION",
                    "Status transition from " + from + " to " + to + " is not permitted"
            );
        }
    }
}

