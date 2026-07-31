package com.ssafyhub.backend.domain.deployment.proxy;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {slug}.ssafyhub.site 로 들어온 요청을 그 배포의 endpoint_url(E206 Traefik)로 리버스 프록시한다.
 * (slug = 학번. 예: 1559989.ssafyhub.site → http://13.124.149.32:443/team-{해시}/1559989/...)
 *
 * - Host가 학번 서브도메인이 아니면(apex/www/api/미지 서브도메인) 그대로 통과시켜 메인 API가 처리한다.
 * - Spring Security보다 먼저 실행되도록 등록한다(SubdomainProxyConfig). 배포된 사용자 앱에 ssafyhub의
 *   인증/CSRF를 강제하지 않기 위함.
 *
 * 한계(단순 API 프록시 전제): WebSocket 미지원, 요청 바디는 메모리에 버퍼링, 요청당 스레드 1개 점유(블로킹).
 * 스트리밍/WS/부하가 필요해지면 Spring Cloud Gateway로 대체할 것.
 */
public class SubdomainProxyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SubdomainProxyFilter.class);

    // 업스트림으로 넘기면 안 되는 요청 헤더(hop-by-hop) + 압축 유발 방지(accept-encoding).
    private static final Set<String> REQUEST_SKIP = Set.of(
            "host", "connection", "content-length", "transfer-encoding", "keep-alive",
            "upgrade", "te", "trailer", "proxy-authenticate", "proxy-authorization",
            "expect", "accept-encoding");
    // 클라이언트로 되돌리면 안 되는 응답 헤더(컨테이너가 관리).
    private static final Set<String> RESPONSE_SKIP = Set.of(
            "connection", "content-length", "transfer-encoding", "keep-alive",
            "upgrade", "trailer");
    // www 등 예약 서브도메인은 프록시 대상에서 제외(메인이 처리).
    private static final Set<String> RESERVED_LABELS = Set.of("www", "api", "admin");

    private final DeploymentRepository deploymentRepository;
    private final String publicDomain;
    private final String frontendWebsiteBase; // S3 정적 웹 호스팅 엔드포인트(끝에 슬래시 없음)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public SubdomainProxyFilter(DeploymentRepository deploymentRepository, String publicDomain,
                                String frontendWebsiteBase) {
        this.deploymentRepository = deploymentRepository;
        this.publicDomain = publicDomain;
        this.frontendWebsiteBase = frontendWebsiteBase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String slug = extractSubdomainSlug(request.getServerName());
        if (slug == null) {
            chain.doFilter(request, response); // 서브도메인 아님 → 메인 API 정상 처리
            return;
        }

        Optional<Deployment> deployment = deploymentRepository.findBySlug(slug);
        if (deployment.isEmpty()) {
            chain.doFilter(request, response); // 알 수 없는 서브도메인 → 메인으로 넘김
            return;
        }

        String uri = request.getRequestURI();
        String suffix = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        if (deployment.get().getFrontendArtifact() == null) {
            // 별도 프론트 배포가 없는 모놀리식 구조(백엔드가 정적 리소스까지 직접 서빙) -
            // /api 분리 없이 요청 경로를 그대로 백엔드로 전달한다.
            String backendBase = deployment.get().getEndpointUrl();
            if (backendBase == null || backendBase.isBlank()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "백엔드가 아직 준비되지 않았습니다.");
                return;
            }
            proxy(request, response, backendBase + uri + suffix, null);
        } else if (uri.equals("/api") || uri.startsWith("/api/")) {
            // /api/* → 백엔드(E206). "/api"는 프론트가 "이건 백엔드로 갈 요청"이라고 표시하는
            // 용도일 뿐이라, 원칙적으로는 벗겨내고 전달한다(k8s Ingress의 stripPrefix -
            // KubernetesDeployService.applyIngress- 와 동일한 방식). 그래서 대부분의 백엔드 앱은
            // 이 플랫폼의 /api 컨벤션을 몰라도 되고, 로컬에서 그냥 돌릴 때와 똑같은 자기 경로
            // (예: /messages)로 요청을 받으면 된다.
            //
            // 다만 사용자가 처음부터 /api 접두사를 자기 라우트에 직접 구현해서 올렸을 수도 있다
            // (구버전 안내를 따랐거나, 원래 그렇게 짠 프로젝트). 그런 경우까지 지원하기 위해,
            // 벗긴 경로가 404면 원래 경로(/api 포함) 그대로 한 번 더 시도한다 - 사용자가 올린
            // 코드를 그대로 존중하고, 어느 쪽이든 실제로 응답하는 쪽을 쓴다.
            String backendBase = deployment.get().getEndpointUrl();
            if (backendBase == null || backendBase.isBlank()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "백엔드가 아직 준비되지 않았습니다.");
                return;
            }
            String backendUri = uri.substring("/api".length());
            if (backendUri.isEmpty()) {
                backendUri = "/";
            }
            proxy(request, response, backendBase + backendUri + suffix, backendBase + uri + suffix);
        } else {
            // 그 외 전부 → 프론트 정적(S3 정적 웹 호스팅)의 {slug}/... 로. 루트/없는 경로는 S3가 index.html로 폴백.
            proxy(request, response, frontendWebsiteBase + "/" + slug + uri + suffix, null);
        }
    }

    /** host가 "{label}.{publicDomain}" 이고 label이 예약어가 아니면 label(slug)을 돌려준다. 아니면 null. */
    private String extractSubdomainSlug(String host) {
        if (host == null) {
            return null;
        }
        String suffix = "." + publicDomain; // .ssafyhub.site
        if (!host.endsWith(suffix) || host.length() == suffix.length()) {
            return null; // apex(ssafyhub.site) 또는 다른 도메인
        }
        String label = host.substring(0, host.length() - suffix.length());
        if (label.isEmpty() || label.contains(".") || RESERVED_LABELS.contains(label)) {
            return null; // 다중 라벨(a.b.ssafyhub.site)/예약 서브도메인 제외
        }
        return label;
    }

    /**
     * target으로 먼저 시도한다. fallbackTarget이 주어졌는데 target이 404를 반환하면(경로 자체가
     * 없다는 뜻), 사용자 백엔드가 다른 경로 컨벤션으로 만들어졌을 수 있으니 fallbackTarget으로
     * 한 번 더 시도해서 그 결과를 그대로 돌려준다.
     */
    private void proxy(HttpServletRequest request, HttpServletResponse response, String target, String fallbackTarget)
            throws IOException {
        try {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            HttpResponse<InputStream> upstream = send(request, target, body);
            if (upstream.statusCode() == 404 && fallbackTarget != null) {
                try (InputStream discard = upstream.body()) {
                    // 첫 시도(404) 응답 바디는 버리고 커넥션만 반환한다.
                }
                upstream = send(request, fallbackTarget, body);
            }
            writeResponse(response, upstream);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "업스트림 연결 중단");
        } catch (IOException e) {
            log.warn("서브도메인 프록시 실패 target={} : {}", target, e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "업스트림 연결 실패");
        }
    }

    private HttpResponse<InputStream> send(HttpServletRequest request, String target, byte[] body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target))
                .timeout(Duration.ofSeconds(30))
                .method(request.getMethod(), body.length == 0
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));

        Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!REQUEST_SKIP.contains(name.toLowerCase())) {
                // JDK HttpClient가 일부 헤더 설정을 막을 수 있어(제한 헤더) 방어적으로 무시.
                try {
                    builder.header(name, request.getHeader(name));
                } catch (IllegalArgumentException ignored) {
                    // 설정 불가 헤더는 건너뜀
                }
            }
        });

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    }

    private void writeResponse(HttpServletResponse response, HttpResponse<InputStream> upstream) throws IOException {
        response.setStatus(upstream.statusCode());
        upstream.headers().map().forEach((key, values) -> {
            if (!RESPONSE_SKIP.contains(key.toLowerCase())) {
                values.forEach(value -> response.addHeader(key, value));
            }
        });
        try (InputStream in = upstream.body()) {
            StreamUtils.copy(in, response.getOutputStream());
        }
    }
}
