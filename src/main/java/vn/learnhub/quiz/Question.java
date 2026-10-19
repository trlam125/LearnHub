package vn.learnhub.quiz;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "question")
@Proxy(lazy = false)
@Getter
@Setter
public class Question extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @NotNull
    public Quiz quiz;
    @Column(name = "text", nullable = false, columnDefinition = "text")
    public String text = "";
    @Column(name = "kind", nullable = false, length = 10)
    @Size(max = 10)
    public String kind = "single";
    @Column(name = "points", nullable = false)
    @Min(1)
    public int points = 1;
    @Column(name = "explanation", nullable = false, columnDefinition = "text")
    public String explanation = "";
    @Column(name = "position", nullable = false)
    public int position = 1;

}
