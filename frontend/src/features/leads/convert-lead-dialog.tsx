"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Copy, Loader2, UserPlus, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { leadsApi } from "@/lib/api/leads";
import { ApiClientError } from "@/lib/api/client";
import type { Lead, LeadConvertResult } from "@/lib/types/lead";

export function ConvertLeadDialog({ lead }: { lead: Lead }) {
  const [open, setOpen] = useState(false);
  const [clientType, setClientType] = useState<"INDIVIDUAL" | "BUSINESS">(lead.company ? "BUSINESS" : "INDIVIDUAL");
  const [result, setResult] = useState<LeadConvertResult | null>(null);
  const queryClient = useQueryClient();

  const displayName = lead.company && lead.company.trim() ? lead.company : lead.name;

  const convertMutation = useMutation({
    mutationFn: () => leadsApi.convert(lead.uuid, clientType),
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to convert lead");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) {
      setResult(null);
    }
  };

  const copyPassword = async () => {
    if (!result) return;
    await navigator.clipboard.writeText(result.client.temporaryPassword);
    toast.success("Temporary password copied");
  };

  // Guarded on `!open` rather than unconditionally: converting invalidates the leads list,
  // which refetches and updates `lead.status` to CONVERTED while this dialog is still open
  // showing the just-created client's one-time temporary password. Hiding unconditionally
  // would unmount the dialog - and that password - before the admin ever saw it.
  if (lead.status === "CONVERTED" && !open) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <Button variant="ghost" size="icon" onClick={() => setOpen(true)} title="Convert to client">
        <UserPlus className="size-3.5" />
      </Button>
      <DialogContent>
        {result ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-success" />
                Converted to client
              </DialogTitle>
              <DialogDescription>
                Share this temporary password with {result.client.client.displayName} - it will
                not be shown again. They will be required to change it on first login.
              </DialogDescription>
            </DialogHeader>
            <div className="flex items-center gap-2 rounded-md border border-border bg-secondary/50 px-3 py-2">
              <code className="flex-1 text-sm">{result.client.temporaryPassword}</code>
              <Button type="button" size="icon" variant="ghost" onClick={copyPassword}>
                <Copy className="size-4" />
              </Button>
            </div>
            <DialogFooter>
              <Button asChild variant="outline">
                <Link href={`/admin/clients/${result.client.client.uuid}`}>View client</Link>
              </Button>
              <Button onClick={() => handleOpenChange(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Convert to client</DialogTitle>
              <DialogDescription>
                Creates a client account using this lead&apos;s details - {displayName} ({lead.email})
                {lead.phone ? `, ${lead.phone}` : ""}. Marks the lead as converted.
              </DialogDescription>
            </DialogHeader>

            <div className="mt-4 space-y-2">
              <Label>Client type</Label>
              <Select value={clientType} onValueChange={(value) => setClientType(value as "INDIVIDUAL" | "BUSINESS")}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                  <SelectItem value="BUSINESS">Business</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
              <Button onClick={() => convertMutation.mutate()} disabled={convertMutation.isPending}>
                {convertMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                Convert
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
