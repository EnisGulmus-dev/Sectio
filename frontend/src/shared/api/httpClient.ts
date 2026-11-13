import { ApiClientError, type ApiErrorPayload } from "./apiError";

type CsrfTokenResponse = {
  headerName: string;
  parameterName: string;
  token: string;
};

let csrfToken: CsrfTokenResponse | null = null;

async function readError(response: Response): Promise<ApiErrorPayload> {
  try {
    return (await response.json()) as ApiErrorPayload;
  } catch {
    return {
      code: "UNEXPECTED_RESPONSE",
      message: "Sunucu beklenmeyen bir cevap döndürdü.",
      fieldErrors: {},
    };
  }
}

export async function refreshCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await fetch("/api/v1/auth/csrf", {
    credentials: "include",
    headers: { Accept: "application/json" },
  });

  if (!response.ok) {
    throw new ApiClientError(response.status, await readError(response));
  }

  csrfToken = (await response.json()) as CsrfTokenResponse;
  return csrfToken;
}

export function clearCsrfToken() {
  csrfToken = null;
}

async function ensureCsrfToken() {
  return csrfToken ?? refreshCsrfToken();
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const stateChanging = !["GET", "HEAD", "OPTIONS"].includes(method);
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.body != null) {
    headers.set("Content-Type", "application/json");
  }

  if (stateChanging) {
    const token = await ensureCsrfToken();
    headers.set(token.headerName, token.token);
  }

  const response = await fetch(path, {
    ...init,
    credentials: "include",
    headers,
  });

  if (!response.ok) {
    throw new ApiClientError(response.status, await readError(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
