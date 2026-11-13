package com.resolveit.attachment;

import com.resolveit.complaint.ComplaintAccessService;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.config.AppProperties;
import com.resolveit.entity.Attachment;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.User;
import com.resolveit.exception.*;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.EventType;
import com.resolveit.workflow.WorkflowService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class AttachmentService {

    private static final Map<String, Set<String>> ALLOWED_MIME_BY_EXT = Map.of(
            "pdf", Set.of("application/pdf"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "webp", Set.of("image/webp"),
            "txt", Set.of("text/plain"),
            "csv", Set.of("text/csv", "text/plain", "application/vnd.ms-excel"),
            "doc", Set.of("application/msword"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    private final AttachmentRepository attachmentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintAccessService accessService;
    private final StorageService storageService;
    private final WorkflowService workflowService;
    private final AppProperties appProperties;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             ComplaintRepository complaintRepository,
                             UserRepository userRepository,
                             ComplaintAccessService accessService,
                             StorageService storageService,
                             WorkflowService workflowService,
                             AppProperties appProperties) {
        this.attachmentRepository = attachmentRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.storageService = storageService;
        this.workflowService = workflowService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AttachmentDtos.AttachmentResponse upload(Long complaintId, UserPrincipal principal, MultipartFile file) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("EMPTY_FILE", "Uploaded file must not be empty");
        }
        if (file.getSize() > appProperties.fileStorage().maxFileSizeBytes()) {
            throw new PayloadTooLargeException("File exceeds maximum allowed size of "
                    + appProperties.fileStorage().maxFileSizeBytes() + " bytes");
        }

        long existingCount = attachmentRepository.countByComplaintId(complaintId);
        if (existingCount >= appProperties.fileStorage().maxFilesPerComplaint()) {
            throw new ConflictException("MAX_ATTACHMENTS_REACHED",
                    "Maximum number of attachments per complaint reached");
        }

        String rawFilename = StringUtils.hasText(file.getOriginalFilename())
                ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                : "attachment";
        String extension = extractExtension(rawFilename);
        List<String> allowedExts = appProperties.fileStorage().allowedExtensions().stream()
                .map(String::toLowerCase)
                .toList();
        if (!allowedExts.contains(extension)) {
            throw new UnsupportedMediaTypeException("File type ." + extension + " is not allowed");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "application/octet-stream";
        validateContentMatchesExtension(extension, contentType, bytes);

        String storageKey = storageService.store(new ByteArrayInputStream(bytes), extension);
        String checksum = sha256(bytes);

        User uploader = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Attachment attachment = new Attachment();
        attachment.setComplaint(complaint);
        attachment.setUploader(uploader);
        attachment.setOriginalFilename(sanitizeFilename(rawFilename));
        attachment.setStorageKey(storageKey);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.getSize());
        attachment.setChecksum(checksum);
        attachment = attachmentRepository.save(attachment);

        workflowService.recordEvent(
                complaint,
                uploader,
                EventType.ATTACHMENT_UPLOADED,
                complaint.getStatus(),
                complaint.getStatus(),
                "Uploaded attachment: " + attachment.getOriginalFilename(),
                Map.of("attachmentId", attachment.getId(), "sizeBytes", attachment.getSizeBytes())
        );

        return AttachmentDtos.AttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentDtos.AttachmentResponse> list(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);
        return attachmentRepository.findByComplaintIdWithUploader(complaintId).stream()
                .map(AttachmentDtos.AttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadableAttachment download(Long attachmentId, UserPrincipal principal) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        accessService.requireCanView(attachment.getComplaint(), principal);
        Resource resource = storageService.loadAsResource(attachment.getStorageKey());
        return new DownloadableAttachment(attachment, resource);
    }

    public record DownloadableAttachment(Attachment metadata, Resource resource) {}

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new UnsupportedMediaTypeException("File must have an allowed extension");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateContentMatchesExtension(String ext, String contentType, byte[] bytes) {
        Set<String> expectedMimes = ALLOWED_MIME_BY_EXT.getOrDefault(ext, Set.of());
        if (!expectedMimes.contains(contentType)) {
            throw new UnsupportedMediaTypeException("Content-Type " + contentType + " does not match ." + ext);
        }

        // Magic-byte verification for key binary types so spoofed extensions are rejected.
        switch (ext) {
            case "pdf" -> {
                if (bytes.length < 4 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
                    throw new UnsupportedMediaTypeException("File content is not a valid PDF");
                }
            }
            case "png" -> {
                if (bytes.length < 4 || (bytes[0] & 0xFF) != 0x89 || bytes[1] != 'P' || bytes[2] != 'N' || bytes[3] != 'G') {
                    throw new UnsupportedMediaTypeException("File content is not a valid PNG image");
                }
            }
            case "jpg", "jpeg" -> {
                if (bytes.length < 3 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
                    throw new UnsupportedMediaTypeException("File content is not a valid JPEG image");
                }
            }
            default -> {
                // Other allowlisted formats validated via MIME + extension allowlist.
            }
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

