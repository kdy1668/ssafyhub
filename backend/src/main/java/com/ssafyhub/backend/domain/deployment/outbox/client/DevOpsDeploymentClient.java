package com.ssafyhub.backend.domain.deployment.outbox.client;

import com.ssafyhub.backend.domain.deployment.outbox.dto.DevOpsDeploymentRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DevOpsDeploymentClient {

    private final RestClient restClient;

    public DevOpsDeploymentClient(@Qualifier("devOpsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void requestDeployment(Long deploymentId) {
        restClient.post()
                .uri("/api/deployments")
                .body(new DevOpsDeploymentRequest(deploymentId))
                .retrieve()
                .toBodilessEntity();
    }
}
