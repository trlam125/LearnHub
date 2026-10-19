package vn.learnhub.auth;

import vn.learnhub.user.UserRepository;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwords() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService details(UserRepository repo) {
        return email -> {
            var u = repo.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new UsernameNotFoundException("Tài khoản không hợp lệ."));
            return new SessionUser(u);
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/finance/**", "/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/my-learning/**", "/tai-khoan/**", "/tu-da-luu/**", "/flashcards/**", "/attempts/**", "/orders/**", "/quizzes/**", "/gop-y/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/payments/payos/webhook"))
                .formLogin(login -> login.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/my-learning", false).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(policy -> policy.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' https: data:; media-src 'self' https:; frame-src 'self' https://drive.google.com https://onedrive.live.com https://*.sharepoint.com; object-src 'none'; base-uri 'self'; frame-ancestors 'self'")))
                .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
