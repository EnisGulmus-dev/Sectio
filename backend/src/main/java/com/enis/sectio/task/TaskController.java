package com.enis.sectio.task;
import com.enis.sectio.common.web.PageResponse; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.time.Instant;
@RestController public class TaskController{private final TaskService s;public TaskController(TaskService s){this.s=s;}
 @GetMapping("/api/v1/projects/{pid}/tasks") PageResponse<TaskResponse> list(@PathVariable Long pid,Authentication a,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return s.listProject(pid,a.getName(),page,size);}
 @PostMapping("/api/v1/projects/{pid}/tasks") @ResponseStatus(HttpStatus.CREATED) TaskResponse create(@PathVariable Long pid,@Valid @RequestBody TaskInput r,Authentication a){return s.create(pid,a.getName(),r.assigneeId(),r.title(),r.description(),r.priority(),r.dueAt());}
 @GetMapping("/api/v1/tasks/my") PageResponse<TaskResponse> mine(Authentication a,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return s.mine(a.getName(),page,size);}
 @GetMapping("/api/v1/tasks/{id}") TaskResponse get(@PathVariable Long id,Authentication a){return s.get(id,a.getName());}
 @PatchMapping("/api/v1/tasks/{id}") TaskResponse update(@PathVariable Long id,@Valid @RequestBody TaskInput r,Authentication a){return s.update(id,a.getName(),r.assigneeId(),r.title(),r.description(),r.priority(),r.dueAt());}
 @PatchMapping("/api/v1/tasks/{id}/status") TaskResponse status(@PathVariable Long id,@Valid @RequestBody StatusInput r,Authentication a){return s.status(id,a.getName(),r.status());}
 @DeleteMapping("/api/v1/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id,Authentication a){s.delete(id,a.getName());}
 public record TaskInput(Long assigneeId,@NotBlank @Size(max=200)String title,@Size(max=5000)String description,@NotNull TaskPriority priority,Instant dueAt){} public record StatusInput(@NotNull TaskStatus status){}
}
