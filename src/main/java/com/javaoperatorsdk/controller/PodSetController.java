package com.javaoperatorsdk.controller;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.client.informers.cache.Cache;
import com.javaoperatorsdk.model.PodSet;
import com.javaoperatorsdk.model.PodSetList;
import com.javaoperatorsdk.model.PodSetStatus;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.kubernetes.client.informers.cache.Lister;

public class PodSetController implements Runnable {

	private final BlockingQueue<String> workqueue;
	private  SharedIndexInformer<PodSet> podSetInformer;
	private  SharedIndexInformer<Pod> podInformer;
	private  Lister<PodSet> podSetLister;
	private  Lister<Pod> podLister;
	private final KubernetesClient client = new DefaultKubernetesClient();
	private MixedOperation<PodSet, PodSetList, Resource<PodSet>> podSetClient;
	public static final Logger logger = Logger.getLogger(PodSetController.class.getName());
	public static final String APP_LABEL = "app";
	public static final String DEFAULT_NAME_SAPCE = "default";

	public PodSetController() {		
		this.workqueue = new ArrayBlockingQueue<>(1024);
	}
	
	@Override
	public void run() {
		String namespace = client.getNamespace();
		if (namespace == null) {
			logger.log(Level.INFO, "No namespace found via config, assuming default.");
			namespace = "default";
		}

		logger.log(Level.INFO, "Using namespace : " + namespace);
		SharedInformerFactory informerFactory = client.informers();
		
		podSetClient = client.customResources(PodSet.class,
				PodSetList.class);
		podInformer = informerFactory.sharedIndexInformerFor(Pod.class,
				10 * 60 * 1000);
		podSetInformer = informerFactory
				.sharedIndexInformerForCustomResource(PodSet.class, 10 * 60 * 1000);
		
		podLister = new Lister<>(this.podInformer.getIndexer(), namespace);
		podSetLister = new Lister<>(podSetInformer.getIndexer(), namespace);
		
		
		create();
		client.informers().startAllRegisteredInformers();
		client.informers().addSharedInformerEventListener(
				exception -> logger.log(Level.SEVERE, "Exception occurred, but caught", exception));

		run();
	}

	public void create() {
		podSetInformer.addEventHandler(new ResourceEventHandler<PodSet>() {
			@Override
			public void onAdd(PodSet podSet) {
				enqueuePodSet(podSet);
			}

			@Override
			public void onUpdate(PodSet podSet, PodSet newPodSet) {
				enqueuePodSet(newPodSet);
			}

			@Override
			public void onDelete(PodSet podSet, boolean b) {
				// Do nothing
			}
		});

		podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
			@Override
			public void onAdd(Pod pod) {
				handlePodObject(pod);
			}

			@Override
			public void onUpdate(Pod oldPod, Pod newPod) {
				if (oldPod.getMetadata().getResourceVersion().equals(newPod.getMetadata().getResourceVersion())) {
					return;
				}
				handlePodObject(newPod);
			}

			@Override
			public void onDelete(Pod pod, boolean b) {
				// Do nothing
			}
		});
	}

	public void runService() {
		logger.log(Level.INFO, "Starting PodSet controller");
		while (!podInformer.hasSynced() || !podSetInformer.hasSynced()) {
			// Wait till Informer syncs
		}

		while (true) {
			try {
				logger.log(Level.INFO, "trying to fetch item from workqueue...");
				if (workqueue.isEmpty()) {
					logger.log(Level.INFO, "Work Queue is empty");
				}
				String key = workqueue.take();
				Objects.requireNonNull(key, "key can't be null");
				logger.log(Level.INFO, String.format("Got %s", key));
				if (key.isEmpty() || (!key.contains("/"))) {
					logger.log(Level.WARNING, String.format("invalid resource key: %s", key));
				}

				// Get the PodSet resource's name from key which is in format namespace/name
				String name = key.split("/")[1];
				PodSet podSet = podSetLister.get(key.split("/")[1]);
				if (podSet == null) {
					logger.log(Level.SEVERE, String.format("PodSet %s in workqueue no longer exists", name));
					return;
				}
				reconcile(podSet);

			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				logger.log(Level.SEVERE, "controller interrupted..");
			}
		}
	}

	/**
	 * Tries to achieve the desired state for podset.
	 *
	 * @param podSet specified podset
	 */
	protected void reconcile(PodSet podSet) {
		List<String> pods = podCountByLabel(APP_LABEL, podSet.getMetadata().getName());
		if (pods.isEmpty()) {
			createPods(podSet.getSpec().getReplicas(), podSet);
			return;
		}
		int existingPods = pods.size();

		// Compare it with desired state i.e spec.replicas
		// if less then spin up pods
		if (existingPods < podSet.getSpec().getReplicas()) {
			createPods(podSet.getSpec().getReplicas() - existingPods, podSet);
		}

		// If more pods then delete the pods
		int diff = existingPods - podSet.getSpec().getReplicas();
		for (; diff > 0; diff--) {
			String podName = pods.remove(0);
			client.pods().inNamespace(podSet.getMetadata().getNamespace()).withName(podName).delete();
		}

		// Update PodSet status
		updateAvailableReplicasInPodSetStatus(podSet, podSet.getSpec().getReplicas());
	}

	private void createPods(int numberOfPods, PodSet podSet) {
		for (int index = 0; index < numberOfPods; index++) {
			Pod pod = createNewPod(podSet);
			client.pods().inNamespace(podSet.getMetadata().getNamespace()).create(pod);
		}
	}

	private List<String> podCountByLabel(String label, String podSetName) {
		List<String> podNames = new ArrayList<>();
		List<Pod> pods = podLister.list();

		for (Pod pod : pods) {
			if (pod.getMetadata().getLabels().entrySet().contains(new AbstractMap.SimpleEntry<>(label, podSetName))) {
				if (pod.getStatus().getPhase().equals("Running") || pod.getStatus().getPhase().equals("Pending")) {
					podNames.add(pod.getMetadata().getName());
				}
			}
		}

		logger.log(Level.INFO, String.format("count: %d", podNames.size()));
		return podNames;
	}

	private void enqueuePodSet(PodSet podSet) {
		logger.log(Level.INFO, "enqueuePodSet(" + podSet.getMetadata().getName() + ")");
		String key = Cache.metaNamespaceKeyFunc(podSet);
		logger.log(Level.INFO, String.format("Going to enqueue key %s", key));
		if (key != null && !key.isEmpty()) {
			logger.log(Level.INFO, "Adding item to workqueue");
			workqueue.add(key);
		}
	}

	private void handlePodObject(Pod pod) {
		logger.log(Level.INFO, "handlePodObject(" + pod.getMetadata().getName() + ")");
		OwnerReference ownerReference = getControllerOf(pod);
		Objects.requireNonNull(ownerReference);
		if (!ownerReference.getKind().equalsIgnoreCase("PodSet")) {
			return;
		}
		PodSet podSet = podSetLister.get(ownerReference.getName());
		if (podSet != null) {
			enqueuePodSet(podSet);
		}
	}

	private void updateAvailableReplicasInPodSetStatus(PodSet podSet, int replicas) {
		PodSetStatus podSetStatus = new PodSetStatus();
		podSetStatus.setAvailableReplicas(replicas);
		podSet.setStatus(podSetStatus);
		podSetClient.inNamespace(podSet.getMetadata().getNamespace()).withName(podSet.getMetadata().getName())
				.updateStatus(podSet);
	}

	private Pod createNewPod(PodSet podSet) {
		return new PodBuilder().withNewMetadata().withGenerateName(podSet.getMetadata().getName() + "-pod")
				.withNamespace(podSet.getMetadata().getNamespace())
				.withLabels(Collections.singletonMap(APP_LABEL, podSet.getMetadata().getName())).addNewOwnerReference()
				.withController(true).withKind("PodSet").withApiVersion("demo.k8s.io/v1alpha1")
				.withName(podSet.getMetadata().getName()).withNewUid(podSet.getMetadata().getUid()).endOwnerReference()
				.endMetadata().withNewSpec().addNewContainer().withName("busybox").withImage("busybox")
				.withCommand("sleep", "3600").endContainer().endSpec().build();
	}

	private OwnerReference getControllerOf(Pod pod) {
		List<OwnerReference> ownerReferences = pod.getMetadata().getOwnerReferences();
		for (OwnerReference ownerReference : ownerReferences) {
			if (ownerReference.getController().equals(Boolean.TRUE)) {
				return ownerReference;
			}
		}
		return null;
	}

	
}
