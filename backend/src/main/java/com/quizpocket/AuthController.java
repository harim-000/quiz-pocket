package com.quizpocket;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api/auth")
class AuthController {
 record Credentials(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=10,max=72) String password){}
 record Registration(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=10,max=72) String password,@NotBlank @Size(max=60) String displayName){}
 record Profile(UUID id,String email,String displayName){}
 private final Users users;private final PasswordEncoder passwords;private final SecurityContextRepository contexts;
 private final LinkedHashMap<String,Attempt> attempts=new LinkedHashMap<>();
 private record Attempt(int count,long until){}
 AuthController(Users u,PasswordEncoder p,SecurityContextRepository c){users=u;passwords=p;contexts=c;}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
 private synchronized void throttle(HttpServletRequest req){
  long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->e.getValue().until<now);
  String ip=req.getRemoteAddr();Attempt a=attempts.get(ip);
  if(a!=null&&a.count>=20)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"잠시 후 다시 시도해 주세요.");
  if(attempts.size()>10000)attempts.remove(attempts.keySet().iterator().next());
  attempts.put(ip,new Attempt(a==null?1:a.count+1,a==null?now+600000:a.until));
 }
 private void validPassword(String p){if(p.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"비밀번호는 UTF-8 기준 72바이트 이하여야 해요.");}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
 Profile register(@Valid @RequestBody Registration input,HttpServletRequest req,HttpServletResponse res){
  throttle(req);validPassword(input.password());String email=input.email().trim().toLowerCase(Locale.ROOT);
  if(users.findByEmail(email).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"가입 정보를 확인해 주세요. 이미 가입했다면 로그인해 주세요.");
  AppUser user=users.saveAndFlush(new AppUser(email,input.displayName().trim(),passwords.encode(input.password())));return loginUser(user,req,res);
 }
 @PostMapping("/login") Profile login(@Valid @RequestBody Credentials input,HttpServletRequest req,HttpServletResponse res){
  throttle(req);validPassword(input.password());AppUser user=users.findByEmail(input.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
  if(user==null||!passwords.matches(input.password(),user.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"이메일 또는 비밀번호를 확인해 주세요.");
  return loginUser(user,req,res);
 }
 private Profile loginUser(AppUser u,HttpServletRequest req,HttpServletResponse res){
  if(req.getSession(false)!=null)req.changeSessionId();else req.getSession(true);
  var context=SecurityContextHolder.createEmptyContext();
  context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u.id.toString(),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);return new Profile(u.id,u.email,u.displayName);
 }
 @GetMapping("/me") Profile me(java.security.Principal principal){AppUser u=users.findById(UUID.fromString(principal.getName())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));return new Profile(u.id,u.email,u.displayName);}
}
