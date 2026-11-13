package com.resolveit.department;

import com.resolveit.entity.DepartmentMember;
import com.resolveit.workflow.MemberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentMemberRepository extends JpaRepository<DepartmentMember, Long> {

    Optional<DepartmentMember> findByDepartmentIdAndUserId(Long departmentId, Long userId);

    List<DepartmentMember> findByDepartmentIdAndActiveTrue(Long departmentId);

    List<DepartmentMember> findByDepartmentIdAndMemberTypeAndActiveTrue(Long departmentId, MemberType memberType);

    List<DepartmentMember> findByUserIdAndActiveTrue(Long userId);

    boolean existsByDepartmentIdAndUserIdAndActiveTrue(Long departmentId, Long userId);

    boolean existsByDepartmentIdAndUserIdAndMemberTypeAndActiveTrue(Long departmentId, Long userId, MemberType memberType);

    @Query("SELECT dm.department.id FROM DepartmentMember dm WHERE dm.user.id = :userId AND dm.active = true")
    List<Long> findActiveDepartmentIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT dm.department.id FROM DepartmentMember dm WHERE dm.user.id = :userId AND dm.memberType = :memberType AND dm.active = true")
    List<Long> findActiveDepartmentIdsByUserIdAndMemberType(@Param("userId") Long userId, @Param("memberType") MemberType memberType);
}

