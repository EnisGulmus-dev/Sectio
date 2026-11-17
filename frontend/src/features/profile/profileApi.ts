import { apiRequest } from "../../shared/api/httpClient";
import type { JobTitle } from "../auth/types";

export type Profile = { userId: number; name: string; jobTitle: JobTitle; displayName: string | null; biography: string | null; timeZone: string | null; avatarId: string | null; hasPhoto: boolean };
export const profileApi = {
  get: () => apiRequest<Profile>("/api/v1/profile"),
  update: (input: { displayName: string; biography: string; timeZone: string }) => apiRequest<Profile>("/api/v1/profile", { method: "PATCH", body: JSON.stringify(input) }),
  avatar: (avatarId: string | null) => apiRequest<Profile>("/api/v1/profile/avatar", { method: "PUT", body: JSON.stringify({ avatarId }) }),
  upload: (file: File) => { const body = new FormData(); body.append("file", file); return apiRequest<Profile>("/api/v1/profile/photo", { method: "POST", body }); },
  removePhoto: () => apiRequest<Profile>("/api/v1/profile/photo", { method: "DELETE" }),
  commit: (data: { displayName:string; biography:string; timeZone:string; imageMode:"KEEP"|"INITIALS"|"AVATAR"|"PHOTO"; avatarId:string|null }, file?:File) => {
    const body = new FormData(); body.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
    if (file) body.append("file", file);
    return apiRequest<Profile>("/api/v1/profile/commit", { method: "POST", body });
  },
};
