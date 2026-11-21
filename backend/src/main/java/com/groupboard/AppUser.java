package com.groupboard;
import jakarta.persistence.*;
@Entity public class AppUser {
    @Id @GeneratedValue public Long id;
    @Column(unique = true) public String username;
    public String passwordHash, role, displayName;
    static AppUser of(String u, String h, String r, String n) { var a = new AppUser(); a.username = u; a.passwordHash = h; a.role = r; a.displayName = n; return a; }
}
