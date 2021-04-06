package com.javaoperatorsdk.model;

public class PodSetStatus {

	public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    @Override
    public String toString() {
        return "PodSetStatus{ availableReplicas=" + availableReplicas + "}";
    }

    private int availableReplicas;
    
}
