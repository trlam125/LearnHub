package vn.learnhub.portal;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "planned_product")
@Proxy(lazy = false)
@Getter
@Setter
public class PlannedProduct extends BaseEntity {
    @Column(name = "language", nullable = false, length = 2)
    @NotBlank
    @Pattern(regexp = "zh|en")
    public String language = "zh";

    @Column(name = "title", nullable = false, length = 120)
    @Size(max = 120)
    @NotBlank
    public String title = "";
    @Column(name = "symbol", nullable = false, length = 4)
    @Size(max = 4)
    public String symbol = "建";
    @Column(name = "description", nullable = false, columnDefinition = "text")
    public String description = "";
    @Column(name = "published", nullable = false)
    public boolean published = true;
    @Column(name = "position", nullable = false)
    public int position = 1;

}
