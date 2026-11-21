package com.groupboard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class Api {
    public record Login(@NotBlank String username, @NotBlank String password) {}
    public record Session(String token, String role, String name) {}
    public record GroupView(Long id, String name, String course, long students) {}
    public record PageView<T>(List<T> items, int page, int pages, long total) {}

    private final UserRepo users; private final GroupRepo groups; private final StudentRepo students;
    private final LessonRepo lessons; private final PasswordEncoder enc; private final Jwt jwt;
    Api(UserRepo u, GroupRepo g, StudentRepo s, LessonRepo l, PasswordEncoder e, Jwt j) { users = u; groups = g; students = s; lessons = l; enc = e; jwt = j; }

    @PostMapping("/auth/login")
    Session login(@Valid @RequestBody Login in) {
        var u = users.findByUsername(in.username()).filter(x -> enc.matches(in.password(), x.passwordHash))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong username or password"));
        return new Session(jwt.issue(u), u.role, u.displayName);
    }

    @GetMapping("/groups")
    PageView<GroupView> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "6") int size) {
        var p = groups.findByNameContainingIgnoreCase(q, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name")));
        return new PageView<>(p.map(g -> new GroupView(g.id, g.name, g.course, students.countByGroupId(g.id))).getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @PostMapping("/groups") @PreAuthorize("hasRole('ADMIN')")
    StudyGroup addGroup(@Valid @RequestBody StudyGroup g) { g.id = null; return groups.save(g); }

    @DeleteMapping("/groups/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    void delGroup(@PathVariable Long id) { students.deleteByGroupId(id); lessons.deleteByGroupId(id); groups.deleteById(id); }

    @GetMapping("/groups/{id}/students") List<Student> students(@PathVariable Long id) { return students.findByGroupId(id); }

    @PostMapping("/students") @PreAuthorize("hasRole('ADMIN')")
    Student addStudent(@Valid @RequestBody Student s) { s.id = null; return students.save(s); }

    @DeleteMapping("/students/{id}") @PreAuthorize("hasRole('ADMIN')")
    void delStudent(@PathVariable Long id) { students.deleteById(id); }

    @GetMapping("/groups/{id}/lessons") List<Lesson> lessons(@PathVariable Long id) { return lessons.findByGroupId(id); }

    @PostMapping("/lessons") @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    Lesson addLesson(@Valid @RequestBody Lesson l) {
        if (l.endHour <= l.startHour) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson must end after it starts");
        boolean clash = lessons.findByGroupId(l.groupId).stream().anyMatch(o -> o.weekday == l.weekday && o.startHour < l.endHour && l.startHour < o.endHour);
        if (clash) throw new ResponseStatusException(HttpStatus.CONFLICT, "This group already has a lesson at that time");
        l.id = null; return lessons.save(l);
    }

    @DeleteMapping("/lessons/{id}") @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    void delLesson(@PathVariable Long id) { lessons.deleteById(id); }

    @RestControllerAdvice static class Errors {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
            var f = e.getBindingResult().getFieldErrors().stream().findFirst().map((FieldError x) -> x.getField() + ": " + x.getDefaultMessage()).orElse("Invalid input");
            return ResponseEntity.badRequest().body(Map.of("message", f));
        }
        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<Map<String, String>> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason()))); }
        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        ResponseEntity<Map<String, String>> denied(Exception e) { return ResponseEntity.status(403).body(Map.of("message", "Your role can't do this")); }
    }
}
