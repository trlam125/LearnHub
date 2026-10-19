package vn.learnhub.quiz;

import org.hibernate.annotations.Proxy;

import lombok.Getter;
import lombok.Setter;

import vn.learnhub.shared.persistence.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.*;

@Entity
@Table(name = "game_topic")
@Proxy(lazy = false)
@Getter
@Setter
public class GameTopic extends BaseEntity {
    @Column(name = "title", nullable = false, length = 120)
    @Size(max = 120)
    @NotBlank
    public String title = "";
    @Column(name = "description", nullable = false, length = 300)
    @Size(max = 300)
    public String description = "";
    @Column(name = "symbol", nullable = false, length = 4)
    @Size(max = 4)
    public String symbol = "游";
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @NotNull
    public Quiz quiz;
    @Column(name = "published", nullable = false)
    public boolean published = false;
    @Column(name = "position", nullable = false)
    public int position = 1;

}
