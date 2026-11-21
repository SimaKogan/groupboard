package com.groupboard;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity public class Student {
    @Id @GeneratedValue public Long id;
    @NotBlank public String fullName;
    @Email @NotBlank public String email;
    @NotNull public Long groupId;
}
