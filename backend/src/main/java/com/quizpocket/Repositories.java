package com.quizpocket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface Users extends JpaRepository<AppUser,UUID>{Optional<AppUser> findByEmail(String email);}
interface Subjects extends JpaRepository<Subject,UUID>{
 List<Subject> findByOwnerIdOrderByName(UUID owner);
 Optional<Subject> findByIdAndOwnerId(UUID id,UUID owner);
 boolean existsByOwnerIdAndName(UUID owner,String name);
}
interface Questions extends JpaRepository<Question,UUID>{
 List<Question> findBySubjectOwnerIdOrderByTitle(UUID owner);
 List<Question> findBySubjectIdAndSubjectOwnerId(UUID subject,UUID owner);
 Optional<Question> findByIdAndSubjectOwnerId(UUID id,UUID owner);
}
