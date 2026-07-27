package com.ssafyhub.backend.domain.showcase.repository;

import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import com.ssafyhub.backend.domain.showcase.entity.ShowcaseDeployment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShowcaseDeploymentRepository extends JpaRepository<ShowcaseDeployment, Long> {

    Optional<ShowcaseDeployment> findByIdAndMember_Id(Long id, Long memberId);

    @Query("""
            SELECT new com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse(
                d.id, d.member.teamName, d.projectName, d.description, d.endpointUrl, d.updatedAt
            )
            FROM ShowcaseDeployment d
            WHERE d.visibility = com.ssafyhub.backend.domain.showcase.enums.Visibility.PUBLIC
            ORDER BY d.updatedAt DESC
            """)
    Page<PublicDeploymentResponse> findPublicDeployments(Pageable pageable);
}
