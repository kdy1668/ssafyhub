package com.ssafyhub.backend.domain.member.repository;

import com.ssafyhub.backend.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByStudentNumber(String studentNumber);

    Optional<Member> findByStudentNumber(String studentNumber);
}
