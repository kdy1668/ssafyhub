package com.ssafyhub.backend.domain.auth.security;

import com.ssafyhub.backend.domain.member.entity.Member;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String studentNumber;
    private final String name;
    private final String teamName;
    private final String role;

    private CustomUserDetails(Long memberId, String studentNumber, String name, String teamName, String role) {
        this.memberId = memberId;
        this.studentNumber = studentNumber;
        this.name = name;
        this.teamName = teamName;
        this.role = role;
    }

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(
                member.getId(),
                member.getStudentNumber(),
                member.getName(),
                member.getTeamName(),
                member.getRole().name()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return studentNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
