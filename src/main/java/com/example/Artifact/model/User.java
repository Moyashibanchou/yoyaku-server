package com.example.Artifact.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bot_users") // 'USER' is sometimes reserved in databases
public class User {
    @Id
    private String id;
    private boolean isWaitingForHuman;
    private boolean isManualMode = false;

    public User() {
    }

    public User(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isWaitingForHuman() {
        return isWaitingForHuman;
    }

    public void setWaitingForHuman(boolean waitingForHuman) {
        isWaitingForHuman = waitingForHuman;
    }

    public boolean isManualMode() {
        return isManualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.isManualMode = manualMode;
    }
}
