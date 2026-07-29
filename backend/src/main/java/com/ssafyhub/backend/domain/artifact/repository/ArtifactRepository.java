package com.ssafyhub.backend.domain.artifact.repository;

import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
}
