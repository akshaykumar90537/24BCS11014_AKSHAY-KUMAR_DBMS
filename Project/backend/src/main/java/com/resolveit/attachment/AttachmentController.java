package com.resolveit.attachment;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/complaints/{complaintId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a validated evidence attachment to a complaint")
    public ApiResponse<AttachmentDtos.AttachmentResponse> upload(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok("Attachment uploaded", attachmentService.upload(complaintId, principal, file));
    }

    @GetMapping("/complaints/{complaintId}/attachments")
    @Operation(summary = "List attachments for an authorized complaint")
    public ApiResponse<List<AttachmentDtos.AttachmentResponse>> list(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(attachmentService.list(complaintId, principal));
    }

    @GetMapping("/attachments/{id}/download")
    @Operation(summary = "Authorize and stream attachment download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AttachmentService.DownloadableAttachment dl = attachmentService.download(id, principal);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(dl.metadata().getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dl.metadata().getOriginalFilename() + "\"")
                .body(dl.resource());
    }
}

