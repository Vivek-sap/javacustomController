package com.javaoperatorsdk.services;

import org.springframework.stereotype.Service;

import com.javaoperatorsdk.model.DeploymentResult;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class DeploymentService {
	private KubernetesClient client;

	public DeploymentService() {
		client = new DefaultKubernetesClient();
	}

	public DeploymentResult getPodByNamespaceAndLabel(String namespace, String labelkey, String labelValue) {
		DeploymentResult result = new DeploymentResult();
		PodList podList = client.pods().inNamespace(namespace).withLabel(labelkey, labelValue).list();
		if (podList != null && podList.getItems() != null) {
			result.setResult(podList.getItems().size());
		}
		return result;
	}

}
