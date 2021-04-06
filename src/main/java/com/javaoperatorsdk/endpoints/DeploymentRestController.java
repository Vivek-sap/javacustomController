package com.javaoperatorsdk.endpoints;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaoperatorsdk.model.DeploymentResult;
import com.javaoperatorsdk.services.DeploymentService;

@RestController
@RequestMapping(path = "/v1/deployment")
@Validated
public class DeploymentRestController {
	public static final Logger logger = Logger.getLogger(DeploymentRestController.class.getName());
	
	@Autowired
	private DeploymentService service;
	
	@GetMapping(path = "namespace/{namespace}/label/labelkey/{labelkey}/labelValue/{labelValue}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DeploymentResult> getDeploymentByNamespaceAndLabel(@Valid @PathVariable String namespace,
			@Valid @PathVariable String labelkey, @Valid @PathVariable String labelValue) {
		logger.log(Level.INFO, "Fetching the pod deployment information by namespace={} and labelkey={} and labelValue={}"  +  namespace);
		return ResponseEntity.ok(service.getPodByNamespaceAndLabel(namespace, labelkey, labelValue));
	}

}
