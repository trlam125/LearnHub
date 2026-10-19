package vn.learnhub.course;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "teacher")
@Proxy(lazy = false)
@Getter
@Setter
public class Teacher extends BaseEntity {
    @Column(name = "language", nullable = false, length = 4)
    @NotBlank
    @Pattern(regexp = "zh|en|both")
    public String language = "zh";

    @Column(name = "name", nullable = false, length = 120)
    @Size(max = 120)
    @NotBlank
    public String name = "";
    @Column(name = "role_text", nullable = false, length = 160)
    @Size(max = 160)
    public String roleText = "";
    @Column(name = "bio", nullable = false, columnDefinition = "text")
    public String bio = "";
    @Column(name = "education", nullable = false, columnDefinition = "text")
    public String education = "";
    @Column(name = "certificates", nullable = false, columnDefinition = "text")
    public String certificates = "";
    @Column(name = "experience", nullable = false, columnDefinition = "text")
    public String experience = "";
    @Column(name = "philosophy", nullable = false, columnDefinition = "text")
    public String philosophy = "";
    @Column(name = "portrait_url", nullable = false, length = 500)
    @Size(max = 500)
    public String portraitUrl = "";
    @Column(name = "facebook", nullable = false, length = 500)
    @Size(max = 500)
    public String facebook = "";
    @Column(name = "zalo", nullable = false, length = 500)
    @Size(max = 500)
    public String zalo = "";
    @Column(name = "community", nullable = false, length = 500)
    @Size(max = 500)
    public String community = "";
    @Column(name = "contact_email", nullable = false, length = 150)
    @Size(max = 150)
    public String contactEmail = "";
    @Column(name = "published", nullable = false)
    public boolean published = false;
    @Column(name = "position", nullable = false)
    public int position = 1;

}
