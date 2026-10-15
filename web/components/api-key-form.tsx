"use client";

import { useSyncExternalStore } from "react";

const STORAGE_KEY = "relayops.apiKey";
const listeners = new Set<() => void>();

function emit() {
  for (const listener of listeners) {
    listener();
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function readKey() {
  if (typeof window === "undefined") {
    return "";
  }
  return window.localStorage.getItem(STORAGE_KEY) ?? "";
}

export function useApiKey() {
  const apiKey = useSyncExternalStore(subscribe, readKey, () => "");

  function save(value: string) {
    const trimmed = value.trim();
    window.localStorage.setItem(STORAGE_KEY, trimmed);
    emit();
  }

  return { apiKey, save };
}

export function ApiKeyForm() {
  const { apiKey, save } = useApiKey();

  return (
    <form
      className="panel row"
      onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        save(String(form.get("apiKey") ?? ""));
      }}
    >
      <input
        style={{ flex: 1, minWidth: 220 }}
        name="apiKey"
        placeholder="Bearer API key (rok_…)"
        defaultValue={apiKey}
        key={apiKey}
        autoComplete="off"
      />
      <button type="submit">Save key</button>
    </form>
  );
}
