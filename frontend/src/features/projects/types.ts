export type PageResponse<T>={content:T[];page:number;size:number;totalElements:number;totalPages:number;first:boolean;last:boolean};
export type Project={id:number;ownerId:number;ownerName:string;name:string;description:string|null;archived:boolean;createdAt:string;updatedAt:string};
export type ProjectMember={userId:number;name:string;email:string;role:"ADMIN"|"EMPLOYEE";joinedAt:string};
export type ProjectMemberCandidate={id:number;name:string;email:string;role:"ADMIN"|"EMPLOYEE"};
export type TaskStatus="TODO"|"IN_PROGRESS"|"DONE"; export type TaskPriority="LOW"|"MEDIUM"|"HIGH"|"CRITICAL";
export type Task={id:number;projectId:number;projectName:string;createdById:number;createdByName:string;assigneeId:number|null;assigneeName:string|null;title:string;description:string|null;status:TaskStatus;priority:TaskPriority;dueAt:string|null;completedAt:string|null;createdAt:string;updatedAt:string};
