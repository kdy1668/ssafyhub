package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    boolean existsByMemberId(Long memberId);

    // 서브도메인({slug}.ssafyhub.site) 라우팅용 - slug(=학번)로 배포를 찾아 endpoint_url로 프록시한다.
    Optional<Deployment> findBySlug(String slug);

    Optional<Deployment> findByIdAndMember_Id(Long id, Long memberId);

    Optional<Deployment> findByMember_Id(Long memberId);

    // 쇼케이스 목록/상세 조회용. endpoint_url(내부 프록시 타깃, 예: http://13.124.149.32/team-xxx/slug)은
    // 방문자가 직접 들어가면 안 되는 내부 주소라 여기서 노출하지 않는다 - 공개 접속 주소는
    // ShowcaseService에서 slug로 서브도메인 URL을 만들어 채운다.
    Page<Deployment> findByVisibilityOrderByUpdatedAtDesc(Visibility visibility, Pageable pageable);

    Optional<Deployment> findByIdAndVisibility(Long id, Visibility visibility);
}
