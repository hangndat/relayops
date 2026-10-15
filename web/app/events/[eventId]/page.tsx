"use client";

import { useParams } from "next/navigation";
import { useState } from "react";

import { ApiKeyForm, useApiKey } from "@/components/api-key-form";
import { Shell } from "@/components/shell";
import { apiFetch, type EventItem } from "@/lib/api";

export default function EventDetailPage() {
  const params = useParams<{ eventId: string }>();
  const { apiKey } = useApiKey();
  const [event, setEvent] = useState<EventItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!apiKey || !params.eventId) {
      setError("Save an API key first");
      return;
    }
    setError(null);
    try {
      const data = await apiFetch<EventItem>(`/v1/events/${params.eventId}`, apiKey);
      setEvent(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load event");
    }
  }

  async function replay() {
    if (!apiKey || !params.eventId) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const data = await apiFetch<EventItem>(`/v1/events/${params.eventId}/replay`, apiKey, {
        method: "POST",
      });
      setEvent(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Replay failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell title="Event detail">
      <ApiKeyForm />
      <div className="panel row">
        <button type="button" className="secondary" onClick={() => void load()}>
          Load event
        </button>
        <button
          type="button"
          onClick={() => void replay()}
          disabled={busy || !event || event.status === "SUCCEEDED" || event.status === "DELIVERING"}
        >
          Replay delivery
        </button>
      </div>
      {error ? <p className="error">{error}</p> : null}
      {!event ? <p>Click “Load event”.</p> : null}
      {event ? (
        <>
          <section className="panel">
            <p>
              Status: <code>{event.status}</code>
            </p>
            <p>
              Idempotency: <code>{event.idempotencyKey}</code>
            </p>
            <p>
              Attempts: <code>{event.attemptCount}</code>
            </p>
            {event.lastError ? <p className="error">Last error: {event.lastError}</p> : null}
          </section>
          <section className="panel">
            <h2>Attempts</h2>
            {event.attempts.length === 0 ? <p>No attempts recorded yet.</p> : null}
            {event.attempts.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>HTTP</th>
                    <th>Result</th>
                    <th>Error</th>
                    <th>Finished</th>
                  </tr>
                </thead>
                <tbody>
                  {event.attempts.map((attempt) => (
                    <tr key={attempt.id}>
                      <td>{attempt.attemptNumber}</td>
                      <td>{attempt.httpStatus ?? "—"}</td>
                      <td>{attempt.success ? "ok" : "fail"}</td>
                      <td>{attempt.errorMessage ?? "—"}</td>
                      <td>
                        {attempt.finishedAt
                          ? new Date(attempt.finishedAt).toLocaleString()
                          : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : null}
          </section>
        </>
      ) : null}
    </Shell>
  );
}
