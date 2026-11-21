package com.groupboard;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
interface UserRepo extends JpaRepository<AppUser, Long> { Optional<AppUser> findByUsername(String u); }
interface GroupRepo extends JpaRepository<StudyGroup, Long> { Page<StudyGroup> findByNameContainingIgnoreCase(String q, Pageable p); }
interface StudentRepo extends JpaRepository<Student, Long> { List<Student> findByGroupId(Long g); long countByGroupId(Long g); void deleteByGroupId(Long g); }
interface LessonRepo extends JpaRepository<Lesson, Long> { List<Lesson> findByGroupId(Long g); void deleteByGroupId(Long g); }
