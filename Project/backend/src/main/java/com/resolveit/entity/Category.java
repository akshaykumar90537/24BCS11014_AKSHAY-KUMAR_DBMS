package com.resolveit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_department_id")
    private Department defaultDepartment;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Department getDefaultDepartment() { return defaultDepartment; }
    public void setDefaultDepartment(Department defaultDepartment) { this.defaultDepartment = defaultDepartment; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

