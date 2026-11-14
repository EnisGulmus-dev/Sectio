import { describe, expect, it } from "vitest";

import { canCreateProjects, jobTitleLabels } from "./jobTitles";

describe("company job titles", () => {
  it("allows project creation only for team leadership and above", () => {
    expect(canCreateProjects("EMPLOYEE", "SOFTWARE_DEVELOPER")).toBe(false);
    expect(canCreateProjects("EMPLOYEE", "SOFTWARE_ENGINEER")).toBe(false);
    expect(canCreateProjects("EMPLOYEE", "SENIOR_SOFTWARE_ENGINEER")).toBe(false);
    expect(canCreateProjects("EMPLOYEE", "TEAM_LEADER")).toBe(true);
    expect(canCreateProjects("EMPLOYEE", "PROJECT_MANAGER")).toBe(true);
    expect(canCreateProjects("EMPLOYEE", "PROJECT_OWNER")).toBe(true);
    expect(canCreateProjects("ADMIN", "SOFTWARE_DEVELOPER")).toBe(true);
  });

  it("has a visible Turkish label for every title", () => {
    expect(Object.values(jobTitleLabels)).toEqual([
      "Yazılım geliştiricisi",
      "Yazılım mühendisi",
      "Kıdemli yazılım mühendisi",
      "Takım lideri",
      "Proje yöneticisi",
      "Proje sahibi",
    ]);
  });
});
