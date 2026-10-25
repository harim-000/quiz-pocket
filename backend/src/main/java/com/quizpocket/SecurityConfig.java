package com.quizpocket;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
class SecurityConfig {
 @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultUser(){return username->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);};}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityContextRepository contextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean SecurityFilterChain security(HttpSecurity http,SecurityContextRepository repository)throws Exception{
  http.securityContext(c->c.securityContextRepository(repository))
   .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/api/auth/csrf","/api/auth/login","/api/auth/register").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"로그인이 필요해요.\"}");})
    .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"요청을 확인할 수 없어요. 새로고침 후 다시 시도해 주세요.\"}");}))
   .logout(l->l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->res.setStatus(204)));
  return http.build();
 }
}
