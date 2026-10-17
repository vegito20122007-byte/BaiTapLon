package com.auction.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base abstract entity class cho mọi đối tượng trong hệ thống.
 * Đảm bảo tính đóng gói (Encapsulation) và trừu tượng (Abstraction).
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Long id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Entity(Long id) {
        this();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Phương thức đa hình in tóm tắt thông tin của đối tượng.
     */
    public abstract String getSummary();
}
