package vn.learnhub.shared.persistence;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class UuidEntity {
    @Id
    public UUID id = UUID.randomUUID();

}
