package vn.learnhub.quiz;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "choice_option")
@Proxy(lazy = false)
@Getter
@Setter
public class Choice extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @NotNull
    public Question question;
    @Column(name = "text", nullable = false, length = 500)
    @Size(max = 500)
    public String text = "";
    @Column(name = "correct", nullable = false)
    public boolean correct = false;

}
