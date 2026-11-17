import type { JobTitle, UserRole } from "../auth/types";
import type { TaskStatus } from "../projects/types";

export type UserWorkHistoryEntry = {
  id: number;
  taskId: number | null;
  projectId: number | null;
  taskTitle: string;
  projectName: string;
  statusAtEnd: TaskStatus;
  endedAt: string;
  reason: "USER_DELETED" | "PROJECT_MEMBER_REMOVED";
};

export type UserWorkTrackingScope = "ALL" | "CURRENT" | "ENDED";
export type UserWorkTrackingEntry = {
  id: string;
  taskId: number | null;
  projectId: number | null;
  taskTitle: string;
  projectName: string;
  status: TaskStatus;
  assignmentState: "CURRENT" | "ENDED";
  activityAt: string;
  completedAt: string | null;
  endReason: "USER_DELETED" | "PROJECT_MEMBER_REMOVED" | null;
};

export type ManagedUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  jobTitle: JobTitle;
  active: boolean;
  mustChangePassword: boolean;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
};

export type CreateUserInput = {
  name: string;
  email: string;
  temporaryPassword: string;
  role: UserRole;
  jobTitle: JobTitle;
  managerId?: number | null;
  teamIds?: number[];
};

export type UserDeletionImpact = {
  ownedProjects: number;
  projectMemberships: number;
  assignedTasks: number;
  createdTasks: number;
  affectedTasks: number;
};
