package com.groupboard;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class App {
    public static void main(String[] a) { SpringApplication.run(App.class, a); }

    @Bean CommandLineRunner seed(UserRepo users, GroupRepo groups, PasswordEncoder enc,
            @Value("${app.seed.admin-password}") String ap, @Value("${app.seed.teacher-password}") String tp) {
        return x -> {
            if (users.count() > 0) return;
            users.save(AppUser.of("admin", enc.encode(ap), "ADMIN", "Administrator"));
            users.save(AppUser.of("teacher", enc.encode(tp), "TEACHER", "Teacher"));
            groups.save(StudyGroup.of("Web Development 1", "Frontend"));
            groups.save(StudyGroup.of("Data Science 2", "Python"));
        };
    }
}
