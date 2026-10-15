import Link from "next/link";

export function Shell({
  children,
  title,
}: {
  children: React.ReactNode;
  title: string;
}) {
  return (
    <main>
      <nav>
        <Link href="/">RelayOps</Link>
        <Link href="/events">Events</Link>
        <Link href="/dlq">DLQ</Link>
        <Link href="/settings">Settings</Link>
      </nav>
      <h1>{title}</h1>
      {children}
    </main>
  );
}
