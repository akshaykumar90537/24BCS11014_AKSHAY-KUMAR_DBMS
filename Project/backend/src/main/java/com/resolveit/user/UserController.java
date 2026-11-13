package com.resolveit.user;

import com.resolveit.common.ApiResponse;
import com.resolveit.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users (Admin)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users with filters and pagination")
    public ApiResponse<PageResponse<UserDto>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return ApiResponse.ok(userService.listUsers(role, active, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserDto> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create staff or user account")
    public ApiResponse<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User created", userService.createUser(request));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ApiResponse<UserDto> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResponse.ok("User role updated", userService.updateRole(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate user account")
    public ApiResponse<UserDto> updateStatus(@PathVariable Long id, @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok("User status updated", userService.updateStatus(id, request));
    }
}

