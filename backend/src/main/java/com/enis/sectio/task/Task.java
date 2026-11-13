package com.enis.sectio.task;
import com.enis.sectio.project.Project; import com.enis.sectio.user.User; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="tasks")
public class Task {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id") private Project project;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by_id") private User createdBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
 @Column(nullable=false,length=200) private String title; @Column(length=5000) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TaskStatus status;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TaskPriority priority;
 @Column(name="due_at") private Instant dueAt; @Column(name="completed_at") private Instant completedAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Task() {} public Task(Project p,User creator,User assignee,String title,String description,TaskPriority priority,Instant dueAt){this.project=p;this.createdBy=creator;this.status=TaskStatus.TODO;update(assignee,title,description,priority,dueAt);}
 @PrePersist void create(){var n=Instant.now();createdAt=n;updatedAt=n;} @PreUpdate void touch(){updatedAt=Instant.now();}
 public void update(User a,String t,String d,TaskPriority p,Instant due){assignee=a;title=t.trim();description=d==null||d.isBlank()?null:d.trim();priority=p;dueAt=due;}
 public void changeStatus(TaskStatus s){status=s;completedAt=s==TaskStatus.DONE?Instant.now():null;}
 public Long getId(){return id;} public Project getProject(){return project;} public User getCreatedBy(){return createdBy;} public User getAssignee(){return assignee;} public String getTitle(){return title;} public String getDescription(){return description;} public TaskStatus getStatus(){return status;} public TaskPriority getPriority(){return priority;} public Instant getDueAt(){return dueAt;} public Instant getCompletedAt(){return completedAt;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
