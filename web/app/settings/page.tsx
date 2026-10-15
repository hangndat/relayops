"use client";

import { useState } from "react";

import { ApiKeyForm, useApiKey } from "@/components/api-key-form";
import { Shell } from "@/components/shell";
import { apiFetch } from "@/lib/api";

type Destination = {
  id: string;
  name: string;
  targetUrl: string;
  active: boolean;
  createdAt: string;
};

export default function SettingsPage() {
  const { apiKey } = useApiKey();
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [destinationId, setDestinationId] = useState("");
  const [secret, setSecret] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadDestinations() {
    if (!apiKey) {
      return;
    }
    setError(null);
    try {
      const data = await apiFetch<Destination[]>("/v1/destinations", apiKey);
      setDestinations(data);
      if (data[0] && !destinationId) {
        setDestinationId(data[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load destinations");
    }
  }

  async function rotateSecret() {
    if (!apiKey || !destinationId || secret.trim().length < 16) {
      setError("Destination and signing secret (min 16 chars) are required");
      return;
    }
    setError(null);
    setMessage(null);
    try {
      await apiFetch(`/v1/destinations/${destinationId}/rotate-secret`, apiKey, {
        method: "POST",
        body: JSON.stringify({ signingSecret: secret.trim() }),
      });
      setMessage("Signing secret rotated. Update producers to the new secret.");
      setSecret("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Rotate failed");
    }
  }

  return (
    <Shell title="Settings">
      <p>Manage API access and destination signing secrets.</p>
      <ApiKeyForm />
      <section className="panel">
        <h2>Destinations</h2>
        <div className="row">
          <button type="button" className="secondary" onClick={() => void loadDestinations()}>
            Load destinations
          </button>
        </div>
        {destinations.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>URL</th>
              </tr>
            </thead>
            <tbody>
              {destinations.map((destination) => (
                <tr key={destination.id}>
                  <td>{destination.name}</td>
                  <td>
                    <code>{destination.targetUrl}</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </section>
      <section className="panel">
        <h2>Rotate signing secret</h2>
        <div className="row">
          <select
            value={destinationId}
            onChange={(event) => setDestinationId(event.target.value)}
          >
            <option value="">Select destination</option>
            {destinations.map((destination) => (
              <option key={destination.id} value={destination.id}>
                {destination.name}
              </option>
            ))}
          </select>
          <input
            style={{ flex: 1, minWidth: 220 }}
            placeholder="New signing secret (min 16 chars)"
            value={secret}
            onChange={(event) => setSecret(event.target.value)}
          />
          <button type="button" onClick={() => void rotateSecret()}>
            Rotate
          </button>
        </div>
        {message ? <p>{message}</p> : null}
        {error ? <p className="error">{error}</p> : null}
      </section>
    </Shell>
  );
}
