package com.resolveit.report;

import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {}

    public record SummaryReport(
            long totalComplaints,
            long openComplaints,
            long pendingReview,
            long inProgress,
            long resolvedOrClosed,
            long escalatedActive,
            Double averageResolutionHours,
            Double slaCompliancePercent,
            Double averageFeedbackRating,
            Map<String, Long> byStatus,
            Map<String, Long> byPriority,
            Map<Integer, Long> feedbackDistribution
    ) {}

    public record DepartmentMetrics(
            Long departmentId,
            String departmentName,
            long totalComplaints,
            long openCount,
            long resolvedCount,
            long escalatedCount,
            Double averageResolutionHours,
            Double slaCompliancePercent
    ) {}

    public record TrendPoint(
            String period,
            long submittedCount,
            long resolvedCount
    ) {}

    public record ResolutionTimeMetric(
            String groupName,
            long resolvedCount,
            Double averageHours,
            Double medianHours
    ) {}

    public record AdminDashboardResponse(
            SummaryReport summary,
            List<DepartmentMetrics> departmentMetrics,
            List<TrendPoint> monthlyTrend
    ) {}
}

