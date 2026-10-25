package com.quizpocket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;

@RestController @RequestMapping("/api") @Transactional
class BankController {
 record SubjectInput(@NotBlank @Size(max=60) String name){}
 record SubjectView(UUID id,String name){}
 record ChoiceInput(@NotBlank @Size(max=60) String id,@NotBlank @Size(max=5000) String text){}
 record QuestionInput(@NotNull UUID subjectId,@NotBlank @Pattern(regexp="MC|SHORT|ESSAY|OX") String type,@NotBlank @Size(max=10000) String title,@NotBlank @Size(max=10000) String answer,@Size(max=10000) String explanation,@Size(max=10) List<@NotNull @Valid ChoiceInput> choices,@Size(max=30) List<@NotBlank @Size(max=200) String> keywords,@Size(max=30) List<@NotBlank @Size(max=500) String> aliases){}
 record QuestionView(UUID id,UUID subjectId,String subject,String type,String title,String answer,String explanation,List<ChoiceInput> choices,List<String> keywords,List<String> aliases){}
 record ImportDraft(@NotBlank @Pattern(regexp="MC|SHORT|ESSAY|OX") String type,@NotBlank @Size(max=10000) String title,@NotBlank @Size(max=10000) String answer,@Size(max=10000) String explanation,@Size(max=10) List<@NotNull @Valid ChoiceInput> choices,@Size(max=30) List<@NotBlank @Size(max=200) String> keywords,@Size(max=30) List<@NotBlank @Size(max=500) String> aliases){}
 record ImportBatch(@NotBlank @Size(max=60) String subject,@NotEmpty @Size(max=200) List<@NotNull @Valid ImportDraft> questions){}
 record ImportResult(int created,int skipped){}
 private final Users users;private final Subjects subjects;private final Questions questions;
 BankController(Users u,Subjects s,Questions q){users=u;subjects=s;questions=q;}
 private UUID owner(Principal p){return UUID.fromString(p.getName());}
 private Subject subject(UUID id,Principal p){return subjects.findByIdAndOwnerId(id,owner(p)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"과목을 찾을 수 없어요."));}
 private Question question(UUID id,Principal p){return questions.findByIdAndSubjectOwnerId(id,owner(p)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"문제를 찾을 수 없어요."));}
 private SubjectView view(Subject s){return new SubjectView(s.getId(),s.getName());}
 private QuestionView view(Question q){return new QuestionView(q.id,q.subject.getId(),q.subject.getName(),q.type,q.title,q.answer,q.explanation,q.choices.stream().map(c->new ChoiceInput(c.id,c.text)).toList(),List.copyOf(q.keywords),List.copyOf(q.aliases));}
 @GetMapping("/subjects") List<SubjectView> subjects(Principal p){return subjects.findByOwnerIdOrderByName(owner(p)).stream().map(this::view).toList();}
 @GetMapping("/subjects/{id}") SubjectView getSubject(@PathVariable UUID id,Principal p){return view(subject(id,p));}
 @PostMapping("/subjects") @ResponseStatus(HttpStatus.CREATED) SubjectView createSubject(@Valid @RequestBody SubjectInput input,Principal p){
  String name=input.name().trim();if(subjects.existsByOwnerIdAndName(owner(p),name))throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 이름의 과목이 있어요.");
  return view(subjects.saveAndFlush(new Subject(users.findById(owner(p)).orElseThrow(),name)));
 }
 @PutMapping("/subjects/{id}") SubjectView updateSubject(@PathVariable UUID id,@Valid @RequestBody SubjectInput input,Principal p){Subject s=subject(id,p);String name=input.name().trim();if(!s.getName().equals(name)&&subjects.existsByOwnerIdAndName(owner(p),name))throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 이름의 과목이 있어요.");s.setName(name);return view(s);}
 @DeleteMapping("/subjects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteSubject(@PathVariable UUID id,Principal p){Subject s=subject(id,p);questions.deleteAll(questions.findBySubjectIdAndSubjectOwnerId(id,owner(p)));questions.flush();subjects.delete(s);}
 @GetMapping("/questions") List<QuestionView> questions(Principal p){return questions.findBySubjectOwnerIdOrderByTitle(owner(p)).stream().map(this::view).toList();}
 @GetMapping("/questions/{id}") QuestionView getQuestion(@PathVariable UUID id,Principal p){return view(question(id,p));}
 @PostMapping("/questions") @ResponseStatus(HttpStatus.CREATED) QuestionView create(@Valid @RequestBody QuestionInput input,Principal p){Question q=new Question(subject(input.subjectId(),p));apply(q,input);return view(questions.saveAndFlush(q));}
 @PutMapping("/questions/{id}") QuestionView update(@PathVariable UUID id,@Valid @RequestBody QuestionInput input,Principal p){Question q=question(id,p);q.subject=subject(input.subjectId(),p);apply(q,input);return view(q);}
 @DeleteMapping("/questions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id,Principal p){questions.delete(question(id,p));}
 private String signature(Question q){
  String answer=q.type.equals("MC")?q.choices.stream().filter(c->c.id.equals(q.answer)).map(c->c.text).findFirst().orElse(q.answer):q.answer;
  return List.of(q.type,q.title,answer,q.choices.stream().map(c->c.text).sorted().toList()).toString();
 }
 @PostMapping("/questions/import") ImportResult importQuestions(@Valid @RequestBody ImportBatch input,Principal p){
  String name=input.subject().trim();
  Subject s=subjects.findByOwnerIdOrderByName(owner(p)).stream().filter(x->x.getName().equals(name)).findFirst().orElseGet(()->subjects.saveAndFlush(new Subject(users.findById(owner(p)).orElseThrow(),name)));
  Set<String> existing=new HashSet<>();questions.findBySubjectIdAndSubjectOwnerId(s.getId(),owner(p)).forEach(q->existing.add(signature(q)));
  List<Question> additions=new ArrayList<>();int skipped=0;
  for(ImportDraft draft:input.questions()){
   Question q=new Question(s);
   apply(q,new QuestionInput(s.getId(),draft.type(),draft.title(),draft.answer(),draft.explanation(),draft.choices(),draft.keywords(),draft.aliases()));
   if(existing.add(signature(q)))additions.add(q);else skipped++;
  }
  questions.saveAllAndFlush(additions);return new ImportResult(additions.size(),skipped);
 }
 private void apply(Question q,QuestionInput i){
  List<ChoiceInput> choices=i.choices()==null?List.of():i.choices();
  if(i.type().equals("MC")&&(choices.size()<2||choices.stream().map(ChoiceInput::id).distinct().count()!=choices.size()||choices.stream().noneMatch(c->c.id().equals(i.answer()))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"선지는 2개 이상이고 정답은 등록한 선지여야 해요.");
  if(i.type().equals("OX")&&!Set.of("O","X").contains(i.answer()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OX 정답은 O 또는 X여야 해요.");
  q.type=i.type();q.title=i.title().trim();q.answer=i.answer().trim();q.explanation=i.explanation()==null?"":i.explanation();q.choices.clear();q.keywords.clear();q.aliases.clear();
  if(i.type().equals("MC"))choices.forEach(c->q.choices.add(new Choice(c.id(),c.text().trim())));
  if(i.type().equals("ESSAY")&&i.keywords()!=null)q.keywords.addAll(i.keywords().stream().map(String::trim).distinct().toList());
  if(i.type().equals("SHORT")&&i.aliases()!=null)q.aliases.addAll(i.aliases().stream().map(String::trim).distinct().toList());
 }
}
