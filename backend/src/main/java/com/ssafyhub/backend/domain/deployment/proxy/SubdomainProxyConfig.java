package com.ssafyhub.backend.domain.deployment.proxy;

import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SubdomainProxyConfig {

    /**
     * 서브도메인 프록시 필터를 모든 요청("/*")에 대해, Spring Security 필터체인(order -100)보다
     * 먼저 실행되도록 최우선 순위로 등록한다. 그래야 배포된 사용자 앱으로 가는 요청이 ssafyhub의
     * 인증/CSRF를 거치지 않고 그대로 프록시된다. (서브도메인이 아니면 필터가 통과시킴)
     */
    @Bean
    public FilterRegistrationBean<SubdomainProxyFilter> subdomainProxyFilter(
            DeploymentRepository deploymentRepository,
            @Value("${deployment.public-domain}") String publicDomain,
            @Value("${deployment.frontend-website-base:http://ssafy-deploy-frontend.s3-website.ap-northeast-2.amazonaws.com}")
            String frontendWebsiteBase) {
        FilterRegistrationBean<SubdomainProxyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SubdomainProxyFilter(deploymentRepository, publicDomain, frontendWebsiteBase));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("subdomainProxyFilter");
        return registration;
    }
}
