import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";

import { clearCsrfToken } from "../shared/api/httpClient";
import { server } from "./server";

// jsdom does not implement the native dialog methods. Model only the open/close
// state needed by interaction tests; focus trapping and the top layer require a browser.
if (typeof HTMLDialogElement.prototype.showModal !== "function") {
  Object.defineProperty(HTMLDialogElement.prototype, "showModal", {
    configurable: true,
    value(this: HTMLDialogElement) {
      this.setAttribute("open", "");
    },
  });
}

if (typeof HTMLDialogElement.prototype.close !== "function") {
  Object.defineProperty(HTMLDialogElement.prototype, "close", {
    configurable: true,
    value(this: HTMLDialogElement, returnValue?: string) {
      if (returnValue !== undefined) this.returnValue = returnValue;
      this.removeAttribute("open");
      this.dispatchEvent(new Event("close"));
    },
  });
}

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
