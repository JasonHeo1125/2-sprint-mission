package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

  // ⭐️ AuthService 대신 UserRepository와 PasswordEncoder를 직접 사용
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${discodeit.admin.username}")
  private String username;
  @Value("${discodeit.admin.email}")
  private String email;
  @Value("${discodeit.admin.password}")
  private String password;


  @Override
  public void run(ApplicationArguments args) {
    // "admin" 사용자가 DB에 없는 경우에만 생성
    userRepository.findByUsername(username).orElseGet(() -> {
      User admin = new User(
          username,
          email,
          passwordEncoder.encode(password),
          null // 프로필은 없음
      );
      admin.updateRole(Role.ROLE_ADMIN); // 권한을 ADMIN으로 설정
      return userRepository.save(admin);
    });
  }
}