package com.resolveit.report;

import com.resolveit.common.Constants;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.complaint.ComplaintSpecifications;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.entity.Complaint;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.feedback.FeedbackRepository;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.MemberType;
import com.resolveit.workflow.Priority;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    private final ComplaintRepository complaintRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final FeedbackRepository feedbackRepository;

    public ReportService(ComplaintRepository complaintRepository,
                         DepartmentMemberRepository departmentMemberRepository,
                         FeedbackRepository feedbackRepository) {
        this.complaintRepository = complaintRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public ReportDtos.SummaryReport getSummary(
            UserPrincipal principal,
            Instant from,
            Instant to,
            Long departmentId,
            Long categoryId,
            Priority priority,
            ComplaintStatus status
    ) {
        List<Complaint> rows = fetchScopedComplaints(principal, from, to, departmentId, categoryId, priority, status);

        long total = rows.size();
        long open = rows.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.CLOSED && c.getStatus() != ComplaintStatus.REJECTED)
                .count();
        long pendingReview = rows.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.UNDER_REVIEW)
                .count();
        long inProgress = rows.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        long resolvedOrClosed = rows.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.CLOSED)
                .count();
        long escalatedActive = rows.stream()
                .filter(c -> c.isEscalated()
                        && c.getStatus() != ComplaintStatus.CLOSED
                        && c.getStatus() != ComplaintStatus.REJECTED)
                .count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus st : ComplaintStatus.values()) {
            byStatus.put(st.name(), 0L);
        }
        rows.forEach(c -> byStatus.merge(c.getStatus().name(), 1L, Long::sum));

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), 0L);
        }
        rows.forEach(c -> byPriority.merge(c.getPriority().name(), 1L, Long::sum));

        Double avgResHours = computeAverageResolutionHours(rows);
        Double slaCompliance = computeSlaCompliance(rows);

        Double avgRating = feedbackRepository.findAverageRating();
        Map<Integer, Long> ratingDist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, 0L);
        }
        for (Object[] row : feedbackRepository.countGroupedByRating()) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            ratingDist.put(rating, count);
        }

        return new ReportDtos.SummaryReport(
                total,
                open,
                pendingReview,
                inProgress,
                resolvedOrClosed,
                escalatedActive,
                avgResHours,
                slaCompliance,
                avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : null,
                byStatus,
                byPriority,
                ratingDist
        );
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.DepartmentMetrics> getByDepartment(
            UserPrincipal principal,
            Instant from,
            Instant to,
            Long departmentId
    ) {
        List<Complaint> rows = fetchScopedComplaints(principal, from, to, departmentId, null, null, null);
        Map<String, List<Complaint>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getDepartment() != null ? c.getDepartment().getId() + "::" + c.getDepartment().getName() : "0::Unassigned",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ReportDtos.DepartmentMetrics> result = new ArrayList<>();
        for (Map.Entry<String, List<Complaint>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("::", 2);
            Long deptId = Long.parseLong(parts[0]);
            String deptName = parts[1];
            List<Complaint> list = entry.getValue();

            long total = list.size();
            long openCount = list.stream()
                    .filter(c -> c.getStatus() != ComplaintStatus.CLOSED && c.getStatus() != ComplaintStatus.REJECTED)
                    .count();
            long resolvedCount = list.stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.CLOSED)
                    .count();
            long escalatedCount = list.stream()
                    .filter(c -> c.isEscalated() && c.getStatus() != ComplaintStatus.CLOSED && c.getStatus() != ComplaintStatus.REJECTED)
                    .count();

            result.add(new ReportDtos.DepartmentMetrics(
                    deptId == 0L ? null : deptId,
                    deptName,
                    total,
                    openCount,
                    resolvedCount,
                    escalatedCount,
                    computeAverageResolutionHours(list),
                    computeSlaCompliance(list)
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.TrendPoint> getTrends(
            UserPrincipal principal,
            Instant from,
            Instant to,
            String groupBy,
            Long departmentId
    ) {
        List<Complaint> rows = fetchScopedComplaints(principal, from, to, departmentId, null, null, null);
        DateTimeFormatter fmt = "daily".equalsIgnoreCase(groupBy) ? DAY_FMT : MONTH_FMT;

        Map<String, long[]> buckets = new TreeMap<>();
        for (Complaint c : rows) {
            String createdPeriod = fmt.format(c.getCreatedAt());
            buckets.computeIfAbsent(createdPeriod, k -> new long[2])[0]++;
            if (c.getResolvedAt() != null) {
                String resolvedPeriod = fmt.format(c.getResolvedAt());
                buckets.computeIfAbsent(resolvedPeriod, k -> new long[2])[1]++;
            }
        }

        return buckets.entrySet().stream()
                .map(e -> new ReportDtos.TrendPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ResolutionTimeMetric> getResolutionTimes(
            UserPrincipal principal,
            Instant from,
            Instant to,
            String groupBy,
            Long departmentId
    ) {
        List<Complaint> rows = fetchScopedComplaints(principal, from, to, departmentId, null, null, null);
        boolean byCategory = "category".equalsIgnoreCase(groupBy);

        Map<String, List<Double>> durations = new LinkedHashMap<>();
        for (Complaint c : rows) {
            if (c.getResolvedAt() == null) {
                continue;
            }
            String key = byCategory
                    ? (c.getCategory() != null ? c.getCategory().getName() : "Uncategorized")
                    : (c.getDepartment() != null ? c.getDepartment().getName() : "Unassigned");
            double hours = Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes() / 60.0;
            durations.computeIfAbsent(key, k -> new ArrayList<>()).add(hours);
        }

        return durations.entrySet().stream()
                .map(e -> {
                    List<Double> vals = e.getValue().stream().sorted().toList();
                    double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double median = vals.size() % 2 == 0
                            ? (vals.get(vals.size() / 2 - 1) + vals.get(vals.size() / 2)) / 2.0
                            : vals.get(vals.size() / 2);
                    return new ReportDtos.ResolutionTimeMetric(
                            e.getKey(),
                            vals.size(),
                            round2(avg),
                            round2(median)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportCsv(
            UserPrincipal principal,
            Instant from,
            Instant to,
            Long departmentId,
            Long categoryId,
            Priority priority,
            ComplaintStatus status
    ) {
        List<Complaint> rows = fetchScopedComplaints(principal, from, to, departmentId, categoryId, priority, status);
        StringBuilder sb = new StringBuilder();
        sb.append("complaint_number,title,status,priority,category,department,escalated,created_at,due_at,resolved_at,closed_at\n");
        for (Complaint c : rows) {
            sb.append(csvEscape(c.getComplaintNumber())).append(',')
                    .append(csvEscape(c.getTitle())).append(',')
                    .append(c.getStatus().name()).append(',')
                    .append(c.getPriority().name()).append(',')
                    .append(csvEscape(c.getCategory() != null ? c.getCategory().getName() : "")).append(',')
                    .append(csvEscape(c.getDepartment() != null ? c.getDepartment().getName() : "")).append(',')
                    .append(c.isEscalated()).append(',')
                    .append(c.getCreatedAt()).append(',')
                    .append(c.getDueAt() != null ? c.getDueAt() : "").append(',')
                    .append(c.getResolvedAt() != null ? c.getResolvedAt() : "").append(',')
                    .append(c.getClosedAt() != null ? c.getClosedAt() : "").append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public ReportDtos.AdminDashboardResponse getAdminDashboard(UserPrincipal principal) {
        ReportDtos.SummaryReport summary = getSummary(principal, null, null, null, null, null, null);
        List<ReportDtos.DepartmentMetrics> depts = getByDepartment(principal, null, null, null);
        List<ReportDtos.TrendPoint> trends = getTrends(principal, null, null, "monthly", null);
        return new ReportDtos.AdminDashboardResponse(summary, depts, trends);
    }

    private List<Complaint> fetchScopedComplaints(
            UserPrincipal principal,
            Instant from,
            Instant to,
            Long requestedDepartmentId,
            Long categoryId,
            Priority priority,
            ComplaintStatus status
    ) {
        Specification<Complaint> spec = ComplaintSpecifications.withFilters(
                status, priority, categoryId, requestedDepartmentId, null, null, from, to
        );

        if (Constants.ROLE_ADMIN.equals(principal.getRoleName())) {
            return complaintRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        if (Constants.ROLE_MANAGER.equals(principal.getRoleName())) {
            List<Long> managedDeptIds = departmentMemberRepository.findActiveDepartmentIdsByUserIdAndMemberType(
                    principal.getId(), MemberType.MANAGER);
            if (managedDeptIds.isEmpty()) {
                return List.of();
            }
            if (requestedDepartmentId != null && !managedDeptIds.contains(requestedDepartmentId)) {
                throw new ForbiddenException("You can only view reports for your own department");
            }
            spec = spec.and(ComplaintSpecifications.inDepartments(managedDeptIds));
            return complaintRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        throw new ForbiddenException("You are not authorized to access reports");
    }

    private Double computeAverageResolutionHours(List<Complaint> rows) {
        OptionalDouble avg = rows.stream()
                .filter(c -> c.getResolvedAt() != null)
                .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes() / 60.0)
                .average();
        return avg.isPresent() ? round2(avg.getAsDouble()) : null;
    }

    private Double computeSlaCompliance(List<Complaint> rows) {
        List<Complaint> eligible = rows.stream()
                .filter(c -> c.getResolvedAt() != null && c.getDueAt() != null)
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }
        long withinSla = eligible.stream()
                .filter(c -> !c.getResolvedAt().isAfter(c.getDueAt()))
                .count();
        return round2((withinSla * 100.0) / eligible.size());
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

