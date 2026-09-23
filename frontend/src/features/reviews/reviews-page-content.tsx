"use client";

import { useQuery } from "@tanstack/react-query";
import { Star, MapPin, ExternalLink } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/layout/page-header";
import { reviewLocationsApi } from "@/lib/api/review";
import { ReviewLocationDialog } from "@/features/reviews/review-location-dialog";

export function ReviewsPageContent() {
  const { data: locations, isLoading } = useQuery({
    queryKey: ["client", "google-reviews"],
    queryFn: () => reviewLocationsApi.listOwn(),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Google Reviews"
        description="Manage the Google Review links your NFC cards and QR codes send customers to."
        actions={<ReviewLocationDialog />}
      />

      {isLoading ? (
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
              description="Add a location to start sending customers to your Google Review page."
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
                  <ReviewLocationDialog existing={location} />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
