import { useLayoutEffect, useState } from "react";

export function useTheme() {
  const [theme, setTheme] = useState<"midnight" | "cloud">(() => {
    try { return localStorage.getItem("sectio-theme") === "cloud" ? "cloud" : "midnight"; }
    catch { return "midnight"; }
  });

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme === "cloud" ? "light" : "dark";
    try { localStorage.setItem("sectio-theme", theme); } catch { /* Theme still works without storage. */ }
  }, [theme]);

  return { theme, toggleTheme: () => setTheme(current => current === "cloud" ? "midnight" : "cloud") };
}
