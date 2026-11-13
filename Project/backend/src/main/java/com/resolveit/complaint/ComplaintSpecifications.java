package com.resolveit.complaint;

import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

public final class ComplaintSpecifications {

    private ComplaintSpecifications() {}

    public static Specification<Complaint> withFilters(
            ComplaintStatus status,
            Priority priority,
            Long categoryId,
            Long departmentId,
            Boolean escalated,
            String search,
            Instant from,
            Instant to
    ) {
        return Specification.where(hasStatus(status))
                .and(hasPriority(priority))
                .and(hasCategoryId(categoryId))
                .and(hasDepartmentId(departmentId))
                .and(isEscalated(escalated))
                .and(matchesSearch(search))
                .and(createdAfter(from))
                .and(createdBefore(to));
    }

    public static Specification<Complaint> createdBy(Long creatorId) {
        return (root, query, cb) -> cb.equal(root.get("creator").get("id"), creatorId);
    }

    public static Specification<Complaint> inDepartmentsOrCreatedBy(List<Long> departmentIds, Long userId) {
        return (root, query, cb) -> {
            if (departmentIds == null || departmentIds.isEmpty()) {
                return cb.equal(root.get("creator").get("id"), userId);
            }
            return cb.or(
                    root.get("department").get("id").in(departmentIds),
                    cb.equal(root.get("creator").get("id"), userId)
            );
        };
    }

    public static Specification<Complaint> assignedToOrCreatedBy(Long employeeId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ComplaintAssignment> ca = sub.from(ComplaintAssignment.class);
            sub.select(ca.get("complaint").get("id"))
                    .where(
                            cb.equal(ca.get("employee").get("id"), employeeId),
                            cb.isTrue(ca.get("active"))
                    );
            return cb.or(
                    root.get("id").in(sub),
                    cb.equal(root.get("creator").get("id"), employeeId)
            );
        };
    }

    public static Specification<Complaint> inDepartments(List<Long> departmentIds) {
        return (root, query, cb) -> {
            if (departmentIds == null || departmentIds.isEmpty()) {
                return cb.disjunction();
            }
            return root.get("department").get("id").in(departmentIds);
        };
    }

    private static Specification<Complaint> hasStatus(ComplaintStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<Complaint> hasPriority(Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    private static Specification<Complaint> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Complaint> hasDepartmentId(Long departmentId) {
        return (root, query, cb) -> departmentId == null ? null : cb.equal(root.get("department").get("id"), departmentId);
    }

    private static Specification<Complaint> isEscalated(Boolean escalated) {
        return (root, query, cb) -> escalated == null ? null : cb.equal(root.get("escalated"), escalated);
    }

    private static Specification<Complaint> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("complaintNumber")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern)
            );
        };
    }

    private static Specification<Complaint> createdAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<Complaint> createdBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
