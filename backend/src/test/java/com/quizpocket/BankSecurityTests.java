package com.quizpocket;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class BankSecurityTests {
 @Autowired jakarta.persistence.EntityManager entityManager;
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired Users users;
 private String body(Object value)throws Exception{return json.writeValueAsString(value);}
 private MockHttpSession register(String email)throws Exception{
  var result=mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("email",email,"password","ExamplePassword123!","displayName","테스트 사용자")))).andExpect(status().isCreated()).andReturn();
  return (MockHttpSession)result.getRequest().getSession();
 }
 private String subject(MockHttpSession session)throws Exception{return json.readTree(mvc.perform(post("/api/subjects").session(session).with(csrf()).contentType("application/json").content("{\"name\":\"운영체제\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();}
 private Map<String,Object> input(String subject,String type){return new HashMap<>(Map.of("subjectId",subject,"type",type,"title","테스트 질문","answer",type.equals("MC")?"a":type.equals("OX")?"O":"정답","choices",type.equals("MC")?List.of(Map.of("id","a","text","정답 선지"),Map.of("id","b","text","오답 선지")):List.of(),"keywords",type.equals("ESSAY")?List.of("필수키워드"):List.of(),"aliases",type.equals("SHORT")?List.of("다른정답"):List.of(),"explanation","해설"));}
 @Test void anonymousAndCsrfAreRejected()throws Exception{
  mvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
 }
 @Test void usersCannotReadChangeOrAttachOthersData()throws Exception{
  var alice=register("alice@example.test");String sid=subject(alice);
  String id=json.readTree(mvc.perform(post("/api/questions").session(alice).with(csrf()).contentType("application/json").content(body(input(sid,"MC")))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
  var bob=register("bob@example.test");
  mvc.perform(get("/api/questions").session(bob)).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(get("/api/subjects/"+sid).session(bob)).andExpect(status().isNotFound());
  mvc.perform(get("/api/questions/"+id).session(bob)).andExpect(status().isNotFound());
  mvc.perform(put("/api/questions/"+id).session(bob).with(csrf()).contentType("application/json").content(body(input(sid,"MC")))).andExpect(status().isNotFound());
  mvc.perform(delete("/api/questions/"+id).session(bob).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(delete("/api/subjects/"+sid).session(bob).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(post("/api/questions").session(bob).with(csrf()).contentType("application/json").content(body(input(sid,"OX")))).andExpect(status().isNotFound());
  assertThat(users.findByEmail("alice@example.test").orElseThrow().passwordHash).startsWith("$2").doesNotContain("ExamplePassword");
 }
 @Test void allTypesUpdateCascadeAndValidation()throws Exception{
  var session=register("types@example.test");String sid=subject(session);
  for(String type:List.of("MC","SHORT","ESSAY","OX")){
   var data=input(sid,type);String id=json.readTree(mvc.perform(post("/api/questions").session(session).with(csrf()).contentType("application/json").content(body(data))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
   data.put("title","수정한 질문");mvc.perform(put("/api/questions/"+id).session(session).with(csrf()).contentType("application/json").content(body(data))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("수정한 질문"));
  }
  var invalid=input(sid,"MC");invalid.put("answer","missing");mvc.perform(post("/api/questions").session(session).with(csrf()).contentType("application/json").content(body(invalid))).andExpect(status().isBadRequest());
  mvc.perform(put("/api/subjects/"+sid).session(session).with(csrf()).contentType("application/json").content("{\"name\":\"자료구조\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("자료구조"));
  mvc.perform(delete("/api/subjects/"+sid).session(session).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/questions").session(session)).andExpect(content().json("[]"));
 }
 @Test void loginAndLogout()throws Exception{
  register("login@example.test");
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email","login@example.test","password","WrongPassword123")))).andExpect(status().isUnauthorized());
  var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email","login@example.test","password","ExamplePassword123!")))).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession();mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("login@example.test"));
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());assertThat(session.isInvalid()).isTrue();
 }
 @Test void batchImportIsAtomicIdempotentAndOwnerScoped()throws Exception{
  var alice=register("pdf-alice@example.test");
  var good=new HashMap<String,Object>(input(java.util.UUID.randomUUID().toString(),"MC"));good.remove("subjectId");
  var bad=new HashMap<>(good);bad.put("title","invalid");bad.put("answer","missing");
  mvc.perform(post("/api/questions/import").session(alice).with(csrf()).contentType("application/json").content(body(Map.of("subject","PDF 과목","questions",List.of(good,bad))))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/questions").session(alice)).andExpect(content().json("[]"));
  var batch=body(Map.of("subject","PDF 과목","questions",List.of(good,good)));
  mvc.perform(post("/api/questions/import").session(alice).with(csrf()).contentType("application/json").content(batch)).andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1)).andExpect(jsonPath("$.skipped").value(1));
  mvc.perform(post("/api/questions/import").session(alice).with(csrf()).contentType("application/json").content(batch)).andExpect(jsonPath("$.created").value(0)).andExpect(jsonPath("$.skipped").value(2));
  var bob=register("pdf-bob@example.test");
  mvc.perform(post("/api/questions/import").session(bob).with(csrf()).contentType("application/json").content(batch)).andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
  mvc.perform(get("/api/questions").session(alice)).andExpect(jsonPath("$.length()").value(1));
 }
 @Test void reloadedQuestionsIncludeTheirSubjectAndRenamedSubject()throws Exception{
  var session=register("subject-reload@example.test");String sid=subject(session);
  mvc.perform(post("/api/questions").session(session).with(csrf()).contentType("application/json").content(body(input(sid,"SHORT")))).andExpect(status().isCreated());
  entityManager.flush();entityManager.clear();
  mvc.perform(get("/api/questions").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].subjectId").value(sid)).andExpect(jsonPath("$[0].subject").value("운영체제"));
  mvc.perform(put("/api/subjects/"+sid).session(session).with(csrf()).contentType("application/json").content("{\"name\":\"자료구조\"}")).andExpect(status().isOk());
  entityManager.flush();entityManager.clear();
  mvc.perform(get("/api/questions").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("자료구조"));
 }
}
