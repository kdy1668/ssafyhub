package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    boolean existsByMemberId(Long memberId);

    // 서브도메인({slug}.ssafyhub.site) 라우팅용 - slug(=학번)로 배포를 찾아 endpoint_url로 프록시한다.
    Optional<Deployment> findBySlug(String slug);

    Optional<Deployment> findByIdAndMember_Id(Long id, Long memberId);

    Optional<Deployment> findByMember_Id(Long memberId);

    @Query("""
            SELECT new com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse(
                d.id, d.member.teamName, d.projectName, d.description, d.endpointUrl, d.updatedAt
            )
            FROM Deployment d
            WHERE d.visibility = com.ssafyhub.backend.domain.deployment.enums.Visibility.PUBLIC
            ORDER BY d.updatedAt DESC
            """)
    Page<PublicDeploymentResponse> findPublicDeployments(Pageable pageable);

    @Query("""
            SELECT new com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse(
                d.id, d.member.teamName, d.projectName, d.description, d.endpointUrl, d.updatedAt
            )
            FROM Deployment d
            WHERE d.id = :id AND d.visibility = com.ssafyhub.backend.domain.deployment.enums.Visibility.PUBLIC
            """)
    Optional<PublicDeploymentResponse> findPublicDeploymentById(@Param("id") Long id);
}
