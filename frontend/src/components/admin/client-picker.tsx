"use client";

import { useQuery } from "@tanstack/react-query";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { clientsApi } from "@/lib/api/clients";

/**
 * Simple client selector shared across admin pages that manage a per-client resource
 * (QR codes, Google Review locations, orders). A plain Select is fine at the client counts
 * this platform runs at today - a searchable combobox is a documented remainder item for
 * when the client list grows large (see docs/PROJECT_PROGRESS.md).
 */
export function ClientPicker({
  value,
  onChange,
  placeholder = "Select a client",
}: {
  value: string | undefined;
  onChange: (clientUuid: string) => void;
  placeholder?: string;
}) {
  const { data } = useQuery({
    queryKey: ["clients", "picker"],
    queryFn: () => clientsApi.list({ size: 100 }),
  });

  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="sm:w-64">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {data?.content.map((client) => (
          <SelectItem key={client.uuid} value={client.uuid}>
            {client.displayName}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
