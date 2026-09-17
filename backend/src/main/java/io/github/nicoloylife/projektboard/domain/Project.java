package io.github.nicoloylife.projektboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Projekt mit Leitung und zugeordneten Mitgliedern. Archivieren ist eine Statusänderung,
 * kein Löschen. Der Fortschritt wird aus den Aufgaben berechnet und nicht gespeichert.
 */
@Entity
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @ManyToMany
    @JoinTable(
            name = "project_member",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Project() {
    }

    public Project(String name, String description, User manager, Tenant tenant) {
        this.name = name;
        this.description = description;
        this.manager = manager;
        this.tenant = tenant;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public boolean isArchived() {
        return status == ProjectStatus.ARCHIVED;
    }

    public User getManager() {
        return manager;
    }

    public Set<User> getMembers() {
        return members;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Leitung und Mitglieder sind für das Projekt berechtigt. */
    public boolean isAccessibleBy(User user) {
        return manager.getId().equals(user.getId())
                || members.stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    public boolean isManagedBy(User user) {
        return manager.getId().equals(user.getId());
    }
}
