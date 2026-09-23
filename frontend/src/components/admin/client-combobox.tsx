"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, Loader2, Check } from "lucide-react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { clientsApi } from "@/lib/api/clients";

/**
 * Searchable client picker for forms where the client list can grow past what fits in a
 * plain dropdown (the assign form's `<Select>` capped at 100 clients, filtered client-side -
 * fine at small scale, but doesn't scale). Queries `clientsApi.list({ search })` server-side
 * as the admin types instead, debounced. No Radix Popover/cmdk in this project's dependency
 * tree yet (see TECHNICAL_DECISIONS.md - shadcn CLI hung, design system is hand-built), so
 * this is a small hand-built input + absolutely-positioned dropdown rather than pulling in a
 * new combobox library for one field.
 */
export function ClientCombobox({
  value,
  onChange,
  placeholder = "Search clients...",
}: {
  value: string | undefined;
  onChange: (clientUuid: string) => void;
  placeholder?: string;
}) {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [manualLabel, setManualLabel] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 250);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const { data, isFetching } = useQuery({
    queryKey: ["clients", "combobox", debouncedQuery],
    queryFn: () => clientsApi.list({ search: debouncedQuery || undefined, size: 20, status: "ACTIVE" }),
    enabled: open,
  });

  // Resolves the label when `value` arrives pre-set from outside (e.g. editing an existing
  // assignment) rather than through selectClient below, which already knows the label -
  // derived directly from query data during render rather than mirrored into state via an
  // effect, since it's fully determined by `value` and `manualLabel`.
  const { data: selectedClient } = useQuery({
    queryKey: ["clients", value],
    queryFn: () => clientsApi.get(value!),
    enabled: Boolean(value) && !manualLabel,
  });
  const resolvedLabel = value ? (manualLabel ?? selectedClient?.displayName ?? null) : null;

  const selectClient = (clientUuid: string, displayName: string) => {
    onChange(clientUuid);
    setManualLabel(displayName);
    setQuery("");
    setOpen(false);
  };

  const displayValue = open ? query : (resolvedLabel ?? "");

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          className="pl-9"
          placeholder={resolvedLabel ?? placeholder}
          value={displayValue}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
        />
      </div>

      {open && (
        <div className="absolute z-50 mt-1 max-h-64 w-full overflow-auto rounded-md border border-border bg-popover p-1 text-popover-foreground shadow-md">
          {isFetching ? (
            <div className="flex items-center justify-center gap-2 py-4 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin" />
              Searching...
            </div>
          ) : !data?.content.length ? (
            <p className="py-4 text-center text-sm text-muted-foreground">No clients found.</p>
          ) : (
            data.content.map((client) => (
              <button
                key={client.uuid}
                type="button"
                className={cn(
                  "flex w-full items-center justify-between rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent hover:text-accent-foreground",
                  client.uuid === value && "bg-accent/50"
                )}
                onClick={() => selectClient(client.uuid, client.displayName)}
              >
                <span>
                  {client.displayName}
                  <span className="ml-2 text-xs text-muted-foreground">{client.email}</span>
                </span>
                {client.uuid === value && <Check className="size-4" />}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
