"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { UserRound } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/layout/page-header";
import { ClientPicker } from "@/components/admin/client-picker";
import { ProfileEditorContent } from "@/features/profile/profile-editor-content";

export function AdminProfilePageContent() {
  const searchParams = useSearchParams();
  const [clientUuid, setClientUuid] = useState<string>(searchParams.get("clientUuid") ?? "");

  return (
    <div className="space-y-6">
      <PageHeader
        title="Profiles"
        description="Edit a client's digital profile or company profile on their behalf."
      />

      <ClientPicker value={clientUuid} onChange={setClientUuid} />

      {!clientUuid ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-16 text-center">
            <UserRound className="size-8 text-muted-foreground" />
            <p className="text-sm font-medium">Select a client</p>
            <p className="max-w-xs text-sm text-muted-foreground">
              Choose a client above to view and edit their profile.
            </p>
          </CardContent>
        </Card>
      ) : (
        <ProfileEditorContent key={clientUuid} clientUuid={clientUuid} />
      )}
    </div>
  );
}
