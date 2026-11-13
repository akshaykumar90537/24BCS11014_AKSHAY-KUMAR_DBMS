package com.resolveit.user;

import com.resolveit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleNameAndActiveTrue(String roleName);

    @Query("""
            SELECT u FROM User u
            WHERE (:roleName IS NULL OR u.role.name = :roleName)
              AND (:active IS NULL OR u.active = :active)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                   OR LOWER(u.email) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
            """)
    Page<User> searchUsers(
            @Param("roleName") String roleName,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}

