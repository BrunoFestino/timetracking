package com.example.timetracking.milestone.domain;

public record JiraProject(String key, String name) {
    public String getLabel() {
        return key + " - " + name;
    }

}
