package fpt.training.qltv.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "authors")
@SQLRestriction("deleted = false")
public class Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books = new HashSet<>();
}
