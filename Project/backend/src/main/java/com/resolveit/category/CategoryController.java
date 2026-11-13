package com.resolveit.category;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List active complaint categories")
    public ApiResponse<List<CategoryDtos.CategoryResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(categoryService.listCategories(includeInactive, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a complaint category")
    public ApiResponse<CategoryDtos.CategoryResponse> create(
            @Valid @RequestBody CategoryDtos.CreateCategoryRequest request
    ) {
        return ApiResponse.ok("Category created", categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a complaint category")
    public ApiResponse<CategoryDtos.CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDtos.UpdateCategoryRequest request
    ) {
        return ApiResponse.ok("Category updated", categoryService.updateCategory(id, request));
    }
}

