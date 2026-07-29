package com.ssafyhub.backend.domain.deployment.enums;

public enum DeploymentStatus {
    PENDING("배포 대기 중"),
    BUILDING("이미지 빌드 중"),
    DEPLOYING("배포 환경에 적용 중"),
    RUNNING("정상 기동"),
    FAILED("배포 실패");

    private final String displayStatus;

    DeploymentStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String displayStatus() {
        return displayStatus;
    }
}
