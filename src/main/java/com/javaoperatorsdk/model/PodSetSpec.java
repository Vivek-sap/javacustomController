package com.javaoperatorsdk.model;

public class PodSetSpec {

	 public int getReplicas() {
	        return replicas;
	    }

	    @Override
	    public String toString() {
	        return "PodSetSpec{replicas=" + replicas + "}";
	    }

	    public void setReplicas(int replicas) {
	        this.replicas = replicas;
	    }

	    private int replicas;
	    
}
