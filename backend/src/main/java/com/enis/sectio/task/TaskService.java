package com.enis.sectio.task;
import com.enis.sectio.common.web.PageResponse; import com.enis.sectio.project.*; import com.enis.sectio.user.*; import org.springframework.data.domain.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.Instant;
@Service public class TaskService{
 private final TaskRepository tasks; private final ProjectService projects; private final UserRepository users;
 public TaskService(TaskRepository t,ProjectService p,UserRepository u){tasks=t;projects=p;users=u;}
 @Transactional(readOnly=true) public PageResponse<TaskResponse> listProject(Long pid,String email,int page,int size){projects.accessibleEntity(pid,email);return PageResponse.from(tasks.findByProjectId(pid,page(page,size)),TaskResponse::from);}
 @Transactional(readOnly=true) public PageResponse<TaskResponse> mine(String email,int page,int size){return PageResponse.from(tasks.findByAssigneeEmail(email,page(page,size)),TaskResponse::from);}
 @Transactional(readOnly=true) public TaskResponse get(Long id,String email){return TaskResponse.from(accessible(id,email));}
 @Transactional public TaskResponse create(Long pid,String email,Long assigneeId,String title,String description,TaskPriority priority,Instant due){Project p=projects.accessibleEntity(pid,email);active(p);User actor=projects.actor(email);User assignee=assignee(p,assigneeId);return TaskResponse.from(tasks.save(new Task(p,actor,assignee,title,description,priority,due)));}
 @Transactional public TaskResponse update(Long id,String email,Long assigneeId,String title,String description,TaskPriority priority,Instant due){Task t=accessible(id,email);active(t.getProject());t.update(assignee(t.getProject(),assigneeId),title,description,priority,due);return TaskResponse.from(t);}
 @Transactional public TaskResponse status(Long id,String email,TaskStatus status){Task t=accessible(id,email);active(t.getProject());t.changeStatus(status);return TaskResponse.from(t);}
 @Transactional public void delete(Long id,String email){Task t=accessible(id,email);active(t.getProject());tasks.delete(t);}
 private Task accessible(Long id,String email){Task t=tasks.findById(id).orElseThrow(()->err("TASK_NOT_FOUND","Task was not found",HttpStatus.NOT_FOUND));projects.accessibleEntity(t.getProject().getId(),email);return t;}
 private User assignee(Project p,Long id){if(id==null)return null;User u=users.findById(id).orElseThrow(()->err("USER_NOT_FOUND","Assignee was not found",HttpStatus.NOT_FOUND));if(!u.isActive()||!projects.isMember(p,u))throw err("INVALID_ASSIGNEE","Assignee must be an active project member",HttpStatus.CONFLICT);return u;}
 private static void active(Project p){if(p.isArchived())throw err("PROJECT_ARCHIVED","Archived project cannot be changed",HttpStatus.CONFLICT);} private static Pageable page(int p,int s){return PageRequest.of(Math.max(p,0),Math.min(Math.max(s,1),100),Sort.by("updatedAt").descending());}
 private static ProjectException err(String c,String m,HttpStatus s){return new ProjectException(c,m,s);}
}
