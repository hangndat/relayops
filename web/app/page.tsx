import { Shell } from "@/components/shell";
import { ApiKeyForm } from "@/components/api-key-form";

async function fetchHealth(baseUrl: string) {
  try {
    const response = await fetch(`${baseUrl}/actuator/health`, { cache: "no-store" });
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
    <Shell title="RelayOps">
      <p>
        Operations console for at-least-once webhook delivery: inspect history,
        open the DLQ, and replay terminal failures.
      </p>
      <section className="panel">
        <p>
          API health: <code>{health.status}</code> — {health.detail}
        </p>
        <p>
          Target: <code>{apiBase}</code>
        </p>
      </section>
      <ApiKeyForm />
    </Shell>
  );
}
