package com.auction.common.model;

import com.auction.common.enums.UserRole;

/**
 * Lớp đại diện cho Quản trị viên (Admin) quản lý hệ thống.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String department;

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    public Admin(Long id, String username, String password, String fullName, String email, String department) {
        super(id, username, password, fullName, email, UserRole.ADMIN);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String getSummary() {
        return String.format("[Admin] %s | Bộ phận: %s", username, department);
    }
}
