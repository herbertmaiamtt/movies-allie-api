package com.hertt.moviesallieapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watched_movies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_watched_movies_user_tmdb",
                        columnNames = {"user_id", "tmdb_id"}
                )
        },
        indexes = {
                @Index(name = "idx_watched_movies_user", columnList = "user_id"),
                @Index(name = "idx_watched_movies_user_watched_at", columnList = "user_id, watched_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class WatchedMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(length = 150)
    private String director;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "poster_path", length = 250)
    private String posterPath;

    @Column(name = "main_actors", columnDefinition = "TEXT")
    private String mainActors;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "watched_at", nullable = false, updatable = false)
    private LocalDateTime watchedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Callbacks JPA

    @PrePersist
    protected void onCreate(){
        this.watchedAt = LocalDateTime.now();
    }

    // Helpers

    public void attachTo(User user){
        this.user = user;
    }

    public void detachFromUser(){
        this.user = null;
    }

}
















