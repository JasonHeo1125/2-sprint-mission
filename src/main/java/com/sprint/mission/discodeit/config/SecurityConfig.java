package com.sprint.mission.discodeit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.config.filter.JsonUsernamePasswordAuthenticationFilter;
import com.sprint.mission.discodeit.security.jwt.JwtAuthenticationFilter;
import com.sprint.mission.discodeit.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  // --- 의존성 주입 ---
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final AuthenticationConfiguration authenticationConfiguration;
  private final JwtService jwtService; // JWT 서비스
  private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 인증 필터

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. CSRF 설정 (SPA 표준 방식)
        .csrf(csrf -> {
          CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
          tokenRepository.setCookieName("XSRF-TOKEN");
          tokenRepository.setHeaderName("X-XSRF-TOKEN");
          csrf.csrfTokenRepository(tokenRepository)
              .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
              .ignoringRequestMatchers(
                  "/api/auth/login", "/api/users",
                  "/api/auth/logout", "/api/auth/refresh"
              );
        })

        // 2. 세션을 사용하지 않도록 STATELESS로 설정 (JWT 방식의 핵심!)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // 3. API 경로별 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                new AntPathRequestMatcher("/api/auth/login"),
                new AntPathRequestMatcher("/api/auth/csrf-token"),
                new AntPathRequestMatcher("/api/auth/refresh"),
                new AntPathRequestMatcher("/api/users", HttpMethod.POST.name())
            ).permitAll()
            .requestMatchers(new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**"))).permitAll()
            .anyRequest().authenticated()
        )

        // 4. 커스텀 필터 등록
        // 로그인 요청 처리 필터 앞에, 우리가 만든 JWT 인증 필터를 먼저 실행하도록 설정
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAt(jsonUsernamePasswordAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

        // 5. 로그아웃 설정 (세션 방식이 아닌 stateless 방식)
        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .logoutSuccessHandler((request, response, authentication) -> {
              // 리프레시 토큰 쿠키를 삭제
              ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                  .path("/")
                  .maxAge(0)
                  .build();
              response.addHeader("Set-Cookie", cookie.toString());
              response.setStatus(HttpServletResponse.SC_OK);
            })
        );

    return http.build();
  }

  // --- 인증 관련 Bean 설정 ---

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, RoleHierarchy roleHierarchy) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager() throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy(
        "ROLE_ADMIN > ROLE_CHANNEL_MANAGER\n" +
            "ROLE_CHANNEL_MANAGER > ROLE_USER"
    );
    return hierarchy;
  }

  // --- 커스텀 필터 및 핸들러 Bean 설정 ---

  @Bean
  public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter() throws Exception {
    JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    filter.setAuthenticationManager(authenticationManager());
    filter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
    filter.setAuthenticationFailureHandler(authenticationFailureHandler());
    return filter;
  }

  @Bean
  public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (request, response, authentication) -> {
      String username = authentication.getName();
      User user = userRepository.findByUsernameWithProfile(username)
          .orElseThrow(() -> new RuntimeException("인증 후 사용자를 찾을 수 없습니다."));

      UserDto userDto = userMapper.toDto(user);

      // JWT 토큰 쌍(액세스, 리프레시) 생성
      JwtService.TokenPair tokenPair = jwtService.generateTokens(userDto);

      // 리프레시 토큰은 HttpOnly 쿠키에 담아서 응답
      ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokenPair.refreshToken())
          .path("/")
          .httpOnly(true)
          .secure(true) // 실제 서비스에서는 true로 설정
          .maxAge(60 * 60 * 24 * 7) // 7일
          .build();
      response.addHeader("Set-Cookie", refreshTokenCookie.toString());

      // 액세스 토큰은 응답 Body에 담아서 전송
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(Map.of("accessToken", tokenPair.accessToken())));
    };
  }

  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return (request, response, exception) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", "로그인 실패", "message", exception.getMessage())));
    };
  }
}