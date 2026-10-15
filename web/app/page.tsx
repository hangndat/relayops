async function fetchHealth(baseUrl: string) {
  try {
    const response = await fetch(`${baseUrl}/actuator/health`, {
      cache: "no-store",
    });
    if (!response.ok) {
      return { status: "down", detail: `HTTP ${response.status}` };
    }
    const body = (await response.json()) as { status?: string };
    return { status: body.status ?? "unknown", detail: "reachable" };
  } catch (error) {
    return {
      status: "down",
      detail: error instanceof Error ? error.message : "unreachable",
    };
  }
}

export default async function HomePage() {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  const health = await fetchHealth(apiBase);

  return (
    <main>
      <h1>RelayOps</h1>
      <p>
        Day 04 reliability core: signed ingest, idempotent accept path, async
        delivery with retry/backoff.
      </p>

      <section className="panel">
        <p className="status">
          API health: <code>{health.status}</code>
        </p>
        <p>
          Target: <code>{apiBase}</code> — {health.detail}
        </p>
        <p>
          Use the API to bootstrap a client, register destinations, and inspect
          `/v1/events/{"{id}"}` for attempt history. Full operations UI lands on
          Day 05.
        </p>
      </section>
    </main>
  );
}
