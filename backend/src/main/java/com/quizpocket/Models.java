package com.quizpocket;
import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name="app_users")
class AppUser {
 @Id UUID id; @Column(nullable=false,unique=true,length=254) String email;
 @Column(name="display_name",nullable=false,length=60) String displayName;
 @Column(name="password_hash",nullable=false,length=100) String passwordHash;
 protected AppUser() {}
 AppUser(String email,String name,String hash){id=UUID.randomUUID();this.email=email;displayName=name;passwordHash=hash;}
}
@Entity @Table(name="subjects",uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","name"}))
class Subject {
 @Id UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id") AppUser owner;
 @Column(nullable=false,length=60) String name;
 public UUID getId(){return id;}
 public String getName(){return name;}
 public void setName(String name){this.name=name;}
 protected Subject(){}
 Subject(AppUser owner,String name){id=UUID.randomUUID();this.owner=owner;this.name=name;}
}
@Embeddable
class Choice {
 @Column(name="choice_id",nullable=false,length=60) String id;
 @Column(nullable=false,columnDefinition="text") String text;
 protected Choice(){} Choice(String id,String text){this.id=id;this.text=text;}
}
@Entity @Table(name="questions")
class Question {
 @Id UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subject_id") Subject subject;
 @Column(nullable=false,length=10) String type;
 @Column(nullable=false,columnDefinition="text") String title;
 @Column(nullable=false,columnDefinition="text") String answer;
 @Column(nullable=false,columnDefinition="text") String explanation;
 @ElementCollection @CollectionTable(name="question_choices",joinColumns=@JoinColumn(name="question_id")) @OrderColumn(name="position") List<Choice> choices=new ArrayList<>();
 @ElementCollection @CollectionTable(name="question_keywords",joinColumns=@JoinColumn(name="question_id")) @Column(name="keyword",length=200) @OrderColumn(name="position") List<String> keywords=new ArrayList<>();
 @ElementCollection @CollectionTable(name="question_aliases",joinColumns=@JoinColumn(name="question_id")) @Column(name="alias",length=500) @OrderColumn(name="position") List<String> aliases=new ArrayList<>();
 protected Question(){} Question(Subject s){id=UUID.randomUUID();subject=s;}
}
