"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Star, MapPin, ExternalLink } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { ListToolbar } from "@/components/layout/list-toolbar";
import { ClientPicker } from "@/components/admin/client-picker";
import { adminReviewLocationsApi } from "@/lib/api/review";
import { AdminReviewLocationDialog } from "@/features/reviews/admin-review-location-dialog";

export function AdminReviewsPageContent() {
  const [clientUuid, setClientUuid] = useState<string>("");

  const { data: locations, isLoading } = useQuery({
    queryKey: ["admin", "google-reviews", clientUuid],
    queryFn: () => adminReviewLocationsApi.listForClient(clientUuid),
    enabled: Boolean(clientUuid),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Google Reviews"
        description="Manage Google Review links on behalf of a client."
        actions={<AdminReviewLocationDialog clientUuid={clientUuid} />}
      />

      <ListToolbar filters={<ClientPicker value={clientUuid} onChange={setClientUuid} />} />

      {!clientUuid ? (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Star}
              title="Select a client"
              description="Choose a client above to view and manage their Google Review locations."
            />
          </CardContent>
        </Card>
      ) : isLoading ? (
        <Card>
          <CardContent className="p-0">
            <ListSkeleton />
          </CardContent>
        </Card>
      ) : !locations || locations.length === 0 ? (
        <Card>
          <CardContent className="p-0">
            <EmptyState
              icon={Star}
              title="No locations yet"
              description="Add a location to start sending customers to this client's Google Review page."
              action={<AdminReviewLocationDialog clientUuid={clientUuid} />}
            />
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {locations.map((location) => (
            <Card key={location.uuid}>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{location.businessName}</CardTitle>
                  {location.locationName && (
                    <p className="text-sm text-muted-foreground">{location.locationName}</p>
                  )}
                </div>
                <Badge variant={location.active ? "success" : "secondary"}>
                  {location.active ? "Active" : "Inactive"}
                </Badge>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {location.address && (
                  <p className="flex items-start gap-2 text-muted-foreground">
                    <MapPin className="mt-0.5 size-3.5 shrink-0" />
                    {location.address}
                  </p>
                )}
                <a
                  href={location.googleReviewUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-2 text-primary hover:underline"
                >
                  <ExternalLink className="size-3.5" />
                  View review link
                </a>
                <div className="pt-2">
                  <AdminReviewLocationDialog clientUuid={clientUuid} existing={location} />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
