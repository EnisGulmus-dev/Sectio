import { apiRequest } from "../../shared/api/httpClient";
export type Channel = { id:number; name:string; description:string|null; visibility:"PUBLIC"|"PRIVATE"; ownerId:number; member:boolean; createdAt:string };
export type ChannelMember = { userId:number; name:string; jobTitle:string; joinedAt:string };
export type Candidate = { userId:number; name:string; jobTitle:string };
export type Message = { id:number; channelId:number; authorId:number; authorName:string; displayName:string|null; avatarId:string|null; hasPhoto:boolean; body:string|null; createdAt:string; editedAt:string|null; deleted:boolean; eventType?:"MEMBER_LEFT"|"MEMBER_REMOVED"|null; subjectName?:string|null };
const base = "/api/v1/channels";
export const channelsApi = {
  list: () => apiRequest<Channel[]>(base),
  create: (input: { name:string; description:string; visibility:"PUBLIC"|"PRIVATE" }) => apiRequest<Channel>(base, {method:"POST",body:JSON.stringify(input)}),
  join: (id:number) => apiRequest<void>(`${base}/${id}/join`, {method:"POST"}),
  remove: (id:number, userId:number) => apiRequest<void>(`${base}/${id}/members/${userId}`, {method:"DELETE"}),
  add: (id:number, userId:number) => apiRequest<void>(`${base}/${id}/members/${userId}`, {method:"PUT"}),
  members: (id:number) => apiRequest<ChannelMember[]>(`${base}/${id}/members`),
  candidates: (id:number) => apiRequest<Candidate[]>(`${base}/${id}/member-candidates`),
  transfer: (id:number, userId:number) => apiRequest<void>(`${base}/${id}/owner/${userId}`, {method:"PUT"}),
  delete: (id:number) => apiRequest<void>(`${base}/${id}`, {method:"DELETE"}),
  messages: (id:number, beforeId?:number) => apiRequest<Message[]>(`${base}/${id}/messages?limit=50${beforeId ? `&beforeId=${beforeId}` : ""}`),
  send: (id:number, body:string) => apiRequest<Message>(`${base}/${id}/messages`, {method:"POST",body:JSON.stringify({body})}),
  edit: (id:number, messageId:number, body:string) => apiRequest<Message>(`${base}/${id}/messages/${messageId}`, {method:"PATCH",body:JSON.stringify({body})}),
  deleteMessage: (id:number, messageId:number) => apiRequest<void>(`${base}/${id}/messages/${messageId}`, {method:"DELETE"}),
};
