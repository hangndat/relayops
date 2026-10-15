export type DeliveryStatus =
  | "RECEIVED"
  | "QUEUED"
  | "DELIVERING"
  | "SUCCEEDED"
  | "FAILED_RETRYABLE"
  | "FAILED_TERMINAL";

export type Attempt = {
  id: string;
  attemptNumber: number;
  startedAt: string;
  finishedAt: string | null;
  httpStatus: number | null;
  success: boolean;
  errorMessage: string | null;
};

export type EventItem = {
  id: string;
  destinationId: string;
  idempotencyKey: string;
  status: DeliveryStatus;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
  attempts: Attempt[];
};

function apiBase() {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
}

export async function apiFetch<T>(
  path: string,
  apiKey: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}
