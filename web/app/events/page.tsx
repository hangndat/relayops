"use client";

import Link from "next/link";
import { useState } from "react";

import { ApiKeyForm, useApiKey } from "@/components/api-key-form";
import { Shell } from "@/components/shell";
import { apiFetch, type DeliveryStatus, type EventItem } from "@/lib/api";

export default function EventsPage() {
  const { apiKey } = useApiKey();
  const [status, setStatus] = useState<DeliveryStatus | "">("");
  const [events, setEvents] = useState<EventItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    if (!apiKey) {
      setError("Save an API key first");
      setEvents([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const query = status ? `?status=${status}` : "";
      const data = await apiFetch<EventItem[]>(`/v1/events${query}`, apiKey);
      setEvents(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load events");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Shell title="Delivery history">
      <p>Inspect inbound events and drill into attempt timelines.</p>
      <ApiKeyForm />
      <div className="panel row">
        <select
          value={status}
          onChange={(event) => setStatus(event.target.value as DeliveryStatus | "")}
        >
          <option value="">All statuses</option>
          <option value="QUEUED">QUEUED</option>
          <option value="DELIVERING">DELIVERING</option>
          <option value="SUCCEEDED">SUCCEEDED</option>
          <option value="FAILED_RETRYABLE">FAILED_RETRYABLE</option>
          <option value="FAILED_TERMINAL">FAILED_TERMINAL</option>
        </select>
        <button type="button" onClick={() => void load()} disabled={loading}>
          Load events
        </button>
      </div>
      {error ? <p className="error">{error}</p> : null}
      {events.length === 0 && !loading && !error ? <p>Click “Load events”.</p> : null}
      {events.length > 0 ? (
        <section className="panel">
          <table>
            <thead>
              <tr>
                <th>Status</th>
                <th>Idempotency</th>
                <th>Attempts</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id}>
                  <td>
                    <Link href={`/events/${event.id}`}>
                      <span
                        className={`badge ${
                          event.status === "SUCCEEDED"
                            ? "ok"
                            : event.status.startsWith("FAILED")
                              ? "bad"
                              : ""
                        }`}
                      >
                        {event.status}
                      </span>
                    </Link>
                  </td>
                  <td>
                    <code>{event.idempotencyKey}</code>
                  </td>
                  <td>{event.attemptCount}</td>
                  <td>{new Date(event.updatedAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}
    </Shell>
  );
}
