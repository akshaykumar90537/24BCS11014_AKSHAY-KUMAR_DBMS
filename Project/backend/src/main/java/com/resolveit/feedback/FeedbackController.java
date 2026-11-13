package com.resolveit.feedback;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints/{complaintId}/feedback")
@Tag(name = "Feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit final rating (1-5) and optional comment once per complaint")
    public ApiResponse<FeedbackDtos.FeedbackResponse> submit(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedbackDtos.SubmitFeedbackRequest request
    ) {
        return ApiResponse.ok("Feedback submitted", feedbackService.submitFeedback(complaintId, principal, request));
    }

    @GetMapping
    @Operation(summary = "Get feedback for a complaint if submitted")
    public ApiResponse<FeedbackDtos.FeedbackResponse> get(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(feedbackService.getFeedback(complaintId, principal));
    }
}

