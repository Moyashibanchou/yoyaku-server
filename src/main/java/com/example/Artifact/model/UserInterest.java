package com.example.Artifact.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserInterest {
    @Id
    private String userId;
    private String category;

    public UserInterest() {
    }

    public UserInterest(String userId, String category) {
        this.userId = userId;
        this.category = category;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
