"use client";

import Link from "next/link";
import { useState } from "react";

import { ApiKeyForm, useApiKey } from "@/components/api-key-form";
import { Shell } from "@/components/shell";
import { apiFetch, type EventItem } from "@/lib/api";

export default function DlqPage() {
  const { apiKey } = useApiKey();
  const [events, setEvents] = useState<EventItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!apiKey) {
      setError("Save an API key first");
      setEvents([]);
      return;
    }
    setError(null);
    try {
      const data = await apiFetch<EventItem[]>("/v1/dlq", apiKey);
      setEvents(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load DLQ");
    }
  }

  return (
    <Shell title="Dead letter queue">
      <p>
        Terminal failures after the retry budget is exhausted. Replay from the
        event detail page once the destination is healthy again.
      </p>
      <ApiKeyForm />
      <div className="panel row">
        <button type="button" onClick={() => void load()}>
          Load DLQ
        </button>
      </div>
      {error ? <p className="error">{error}</p> : null}
      {events.length === 0 && !error ? <p>Click “Load DLQ”.</p> : null}
      {events.length > 0 ? (
        <section className="panel">
          <table>
            <thead>
              <tr>
                <th>Event</th>
                <th>Attempts</th>
                <th>Last error</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id}>
                  <td>
                    <Link href={`/events/${event.id}`}>
                      <code>{event.idempotencyKey}</code>
                    </Link>
                  </td>
                  <td>{event.attemptCount}</td>
                  <td>{event.lastError ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}
    </Shell>
  );
}
