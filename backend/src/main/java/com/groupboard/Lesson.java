package com.groupboard;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity public class Lesson {
    @Id @GeneratedValue public Long id;
    @NotNull public Long groupId;
    @NotBlank public String subject;
    public String room;
    @Min(1) @Max(5) public int weekday;
    @Min(7) @Max(20) public int startHour;
    @Min(8) @Max(21) public int endHour;
}
