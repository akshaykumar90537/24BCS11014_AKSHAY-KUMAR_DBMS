package com.resolveit.category;

import com.resolveit.common.Constants;
import com.resolveit.department.DepartmentRepository;
import com.resolveit.entity.Category;
import com.resolveit.entity.Department;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;

    public CategoryService(CategoryRepository categoryRepository, DepartmentRepository departmentRepository) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listCategories(boolean includeInactive, UserPrincipal principal) {
        boolean isAdmin = principal != null && Constants.ROLE_ADMIN.equals(principal.getRoleName());
        List<Category> categories = (includeInactive && isAdmin)
                ? categoryRepository.findAllByOrderByNameAsc()
                : categoryRepository.findByActiveTrueOrderByNameAsc();
        return categories.stream().map(CategoryDtos.CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse createCategory(CategoryDtos.CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("CATEGORY_EXISTS", "A category with this name already exists");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(true);
        if (request.defaultDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.defaultDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Default department not found"));
            category.setDefaultDepartment(dept);
        }
        return CategoryDtos.CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDtos.CategoryResponse updateCategory(Long id, CategoryDtos.UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active());
        if (request.defaultDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.defaultDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Default department not found"));
            category.setDefaultDepartment(dept);
        } else {
            category.setDefaultDepartment(null);
        }
        return CategoryDtos.CategoryResponse.from(categoryRepository.save(category));
    }
}

