package com.resolveit.comment;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints/{complaintId}/comments")
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a PUBLIC or INTERNAL comment to a complaint")
    public ApiResponse<CommentDtos.CommentResponse> create(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommentDtos.CreateCommentRequest request
    ) {
        return ApiResponse.ok("Comment added", commentService.addComment(complaintId, principal, request));
    }

    @GetMapping
    @Operation(summary = "List comments visible to the authenticated caller")
    public ApiResponse<List<CommentDtos.CommentResponse>> list(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(commentService.listComments(complaintId, principal));
    }
}

