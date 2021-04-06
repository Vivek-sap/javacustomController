package com.javaoperatorsdk;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.javaoperatorsdk.controller.PodSetController;
import com.javaoperatorsdk.model.PodSet;
import com.javaoperatorsdk.model.PodSetList;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

@SpringBootApplication
public class JavakubecontrollerApplication {
	public static final Logger logger = Logger.getLogger(JavakubecontrollerApplication.class.getName());

	public static void main(String[] args) {

		try (KubernetesClient client = new DefaultKubernetesClient()) {
			String namespace = client.getNamespace();
			if (namespace == null) {
				logger.log(Level.INFO, "No namespace found via config, assuming default.");
				namespace = "default";
			}

			logger.log(Level.INFO, "Using namespace : " + namespace);

			SharedInformerFactory informerFactory = client.informers();

			MixedOperation<PodSet, PodSetList, Resource<PodSet>> podSetClient = client.customResources(PodSet.class,
					PodSetList.class);
			SharedIndexInformer<Pod> podSharedIndexInformer = informerFactory.sharedIndexInformerFor(Pod.class,
					10 * 60 * 1000);
			SharedIndexInformer<PodSet> podSetSharedIndexInformer = informerFactory
					.sharedIndexInformerForCustomResource(PodSet.class, 10 * 60 * 1000);
			PodSetController podSetController = new PodSetController(client, podSetClient, podSharedIndexInformer,
					podSetSharedIndexInformer, namespace);

			podSetController.create();
			informerFactory.startAllRegisteredInformers();
			informerFactory.addSharedInformerEventListener(
					exception -> logger.log(Level.SEVERE, "Exception occurred, but caught", exception));

			podSetController.run();
		} catch (KubernetesClientException exception) {
			logger.log(Level.SEVERE, "Kubernetes Client Exception : " + exception.getMessage());
		}
		SpringApplication.run(JavakubecontrollerApplication.class, args);
	}

}
