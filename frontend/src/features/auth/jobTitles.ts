import type { JobTitle, UserRole } from "./types";

export const jobTitleLabels: Record<JobTitle, string> = {
  SOFTWARE_DEVELOPER: "Yazılım geliştiricisi",
  SOFTWARE_ENGINEER: "Yazılım mühendisi",
  SENIOR_SOFTWARE_ENGINEER: "Kıdemli yazılım mühendisi",
  TEAM_LEADER: "Takım lideri",
  PROJECT_MANAGER: "Proje yöneticisi",
  PROJECT_OWNER: "Proje sahibi",
};

export const jobTitleOptions = Object.entries(jobTitleLabels) as [JobTitle, string][];

const projectCreationTitles = new Set<JobTitle>(["TEAM_LEADER", "PROJECT_MANAGER", "PROJECT_OWNER"]);

export function canCreateProjects(role: UserRole, jobTitle: JobTitle) {
  return role === "ADMIN" || projectCreationTitles.has(jobTitle);
}
