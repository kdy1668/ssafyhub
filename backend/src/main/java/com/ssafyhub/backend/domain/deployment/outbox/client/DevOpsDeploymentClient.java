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

    /**
     * deploy-engine에 k8s 리소스(Deployment/Service/Ingress/Middleware) 정리를 요청한다.
     * deploy-engine은 deploymentId로 namespace(팀)와 appName(slug)을 역추적하므로
     * 이 호출 시점에는 deployments 행이 아직 남아 있어야 한다.
     */
    public void deleteDeployment(Long deploymentId) {
        restClient.delete()
                .uri("/api/deployments/{deploymentId}", deploymentId)
                .retrieve()
                // 404는 deploy-engine이 정리할 대상을 찾지 못한 경우다. 배포된 적이 없거나
                // 이미 삭제된 상태이므로, 행을 지우지 못하게 막을 이유가 없어 성공으로 취급한다.
                .onStatus(status -> status.value() == 404, (request, response) -> {
                })
                .toBodilessEntity();
    }
}
