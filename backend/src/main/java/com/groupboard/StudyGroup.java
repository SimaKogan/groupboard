package com.groupboard;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
@Entity public class StudyGroup {
    @Id @GeneratedValue public Long id;
    @NotBlank public String name;
    @NotBlank public String course;
    static StudyGroup of(String n, String c) { var g = new StudyGroup(); g.name = n; g.course = c; return g; }
}
