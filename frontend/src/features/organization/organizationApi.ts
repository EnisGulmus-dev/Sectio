import { apiRequest } from "../../shared/api/httpClient";
export type CompanyTeam = { id:number; name:string };
export type OrganizationPerson = { id:number; name:string; jobTitle:string };
export type Relationship = { userId:number; managerId:number|null; managerName:string|null; teams:CompanyTeam[] };
const base = "/api/v1/organization";
export const organizationApi = {
  teams: () => apiRequest<CompanyTeam[]>(`${base}/teams`),
  createTeam: (name:string, userIds:number[] = []) => apiRequest<CompanyTeam>(`${base}/teams`, { method:"POST", body:JSON.stringify({name,userIds}) }),
  renameTeam: (id:number, name:string) => apiRequest<CompanyTeam>(`${base}/teams/${id}`, { method:"PATCH", body:JSON.stringify({name}) }),
  deleteTeam: (id:number) => apiRequest<void>(`${base}/teams/${id}`, { method:"DELETE" }),
  managers: () => apiRequest<OrganizationPerson[]>(`${base}/managers`),
  reports: () => apiRequest<OrganizationPerson[]>(`${base}/reports`),
  get: (id:number) => apiRequest<Relationship>(`${base}/users/${id}`),
  assign: (id:number, managerId:number|null, teamIds:number[]) => apiRequest<Relationship>(`${base}/users/${id}`, { method:"PUT", body:JSON.stringify({managerId,teamIds}) }),
};
