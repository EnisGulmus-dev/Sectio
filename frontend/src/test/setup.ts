import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";

import { clearCsrfToken } from "../shared/api/httpClient";
import { server } from "./server";

const nativeFetch = globalThis.fetch;

globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) => {
  const resolvedInput = typeof input === "string" && input.startsWith("/")
    ? new URL(input, "http://localhost")
    : input;
  return nativeFetch(resolvedInput, init);
}) as typeof fetch;

beforeAll(() => server.listen());

afterEach(() => {
  cleanup();
  clearCsrfToken();
  server.resetHandlers();
});

afterAll(() => server.close());
