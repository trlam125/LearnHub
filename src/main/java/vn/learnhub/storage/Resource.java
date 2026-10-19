package vn.learnhub.storage;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "learning_resource")
@Proxy(lazy = false)
@Getter
@Setter
public class Resource extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    @NotNull
    public Lesson lesson;
    @Column(name = "title", nullable = false, length = 180)
    @Size(max = 180)
    @NotBlank
    public String title = "";
    @Column(name = "stored_name", nullable = false, length = 100)
    @Size(max = 100)
    public String storedName = "";
    @Column(name = "original_name", nullable = false, length = 200)
    @Size(max = 200)
    public String originalName = "";
    @Column(name = "allow_download", nullable = false)
    public boolean allowDownload = false;
    @Column(name = "reviewed", nullable = false)
    public boolean reviewed = false;

    @Column(name = "material_type", nullable = false, length = 12)
    @Pattern(regexp = "document|video")
    public String materialType = "document";
    @Column(name = "external_url", nullable = false, length = 2048)
    @Size(max = 2048)
    public String externalUrl = "";
    @Column(name = "embed_url", nullable = false, length = 2048)
    @Size(max = 2048)
    public String embedUrl = "";
    @Column(name = "content_type", nullable = false, length = 120)
    public String contentType = "application/pdf";
    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes = 0;

    public boolean isExternal() {
        return externalUrl != null && !externalUrl.isBlank();
    }

    public boolean isVideo() {
        return "video".equals(materialType);
    }

}
