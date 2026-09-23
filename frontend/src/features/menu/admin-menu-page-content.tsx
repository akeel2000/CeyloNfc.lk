"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { UtensilsCrossed } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/layout/page-header";
import { ClientPicker } from "@/components/admin/client-picker";
import { MenuEditorContent } from "@/features/menu/menu-editor-content";

export function AdminMenuPageContent() {
  const searchParams = useSearchParams();
  const [clientUuid, setClientUuid] = useState<string>(searchParams.get("clientUuid") ?? "");

  return (
    <div className="space-y-6">
      <PageHeader title="Menus" description="Edit a client's menu on their behalf." />

      <ClientPicker value={clientUuid} onChange={setClientUuid} />

      {!clientUuid ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-16 text-center">
            <UtensilsCrossed className="size-8 text-muted-foreground" />
            <p className="text-sm font-medium">Select a client</p>
            <p className="max-w-xs text-sm text-muted-foreground">
              Choose a client above to view and edit their menu.
            </p>
          </CardContent>
        </Card>
      ) : (
        <MenuEditorContent key={clientUuid} clientUuid={clientUuid} />
      )}
    </div>
  );
}
