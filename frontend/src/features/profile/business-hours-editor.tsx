"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { profileApi, adminProfileApi } from "@/lib/api/profile";
import { ApiClientError } from "@/lib/api/client";
import type { BusinessHour } from "@/lib/types/profile";

const DAY_LABELS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

function withAllDays(hours: BusinessHour[]): BusinessHour[] {
  const byDay = new Map(hours.map((h) => [h.dayOfWeek, h]));
  return DAY_LABELS.map((_, i) => {
    const dayOfWeek = i + 1;
    return byDay.get(dayOfWeek) ?? { dayOfWeek, opensAt: null, closesAt: null, closed: true };
  });
}

export function BusinessHoursEditor({
  initialHours,
  clientUuid,
}: {
  initialHours: BusinessHour[];
  clientUuid?: string;
}) {
  const [hours, setHours] = useState<BusinessHour[]>(() => withAllDays(initialHours));
  const queryClient = useQueryClient();

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = hours.map((h) =>
        h.closed ? { ...h, opensAt: null, closesAt: null } : h
      );
      return clientUuid ? adminProfileApi.updateBusinessHours(clientUuid, payload) : profileApi.updateBusinessHours(payload);
    },
    onSuccess: (saved) => {
      setHours(withAllDays(saved));
      toast.success("Opening hours saved");
      queryClient.invalidateQueries({ queryKey: clientUuid ? ["admin", "profile", clientUuid] : ["client", "profile"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save opening hours");
    },
  });

  const updateDay = (index: number, patch: Partial<BusinessHour>) => {
    setHours((prev) => prev.map((h, i) => (i === index ? { ...h, ...patch } : h)));
  };

  const copyMondayToAll = () => {
    const monday = hours[0];
    setHours((prev) => prev.map((h, i) => (i === 0 ? h : { ...h, opensAt: monday.opensAt, closesAt: monday.closesAt, closed: monday.closed })));
  };

  const hasInvalidRange = hours.some((h) => !h.closed && h.opensAt && h.closesAt && h.opensAt === h.closesAt);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Opening hours</CardTitle>
        <CardDescription>
          Drives the &quot;Open now&quot; badge on your public profile - visitors see it computed live, not a manual toggle.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {hours.map((day, index) => (
          <div key={day.dayOfWeek} className="flex flex-wrap items-center gap-3">
            <span className="w-24 shrink-0 text-sm font-medium">{DAY_LABELS[index]}</span>
            <label className="flex shrink-0 items-center gap-2 text-sm text-muted-foreground">
              <input
                type="checkbox"
                className="size-4"
                checked={day.closed}
                onChange={(e) => updateDay(index, { closed: e.target.checked })}
              />
              Closed
            </label>
            {!day.closed && (
              <>
                <Input
                  type="time"
                  className="w-32"
                  value={day.opensAt ?? ""}
                  onChange={(e) => updateDay(index, { opensAt: e.target.value })}
                />
                <span className="text-sm text-muted-foreground">to</span>
                <Input
                  type="time"
                  className="w-32"
                  value={day.closesAt ?? ""}
                  onChange={(e) => updateDay(index, { closesAt: e.target.value })}
                />
              </>
            )}
          </div>
        ))}

        {hasInvalidRange && (
          <p className="text-sm text-destructive">Opening and closing time can&apos;t be the same.</p>
        )}

        <div className="flex items-center justify-between pt-2">
          <Button type="button" variant="outline" size="sm" onClick={copyMondayToAll}>
            Copy Monday to all days
          </Button>
          <Button
            type="button"
            size="sm"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending || hasInvalidRange}
          >
            {saveMutation.isPending && <Loader2 className="size-4 animate-spin" />}
            Save hours
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
