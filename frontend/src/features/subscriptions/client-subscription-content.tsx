"use client";

import { useQuery } from "@tanstack/react-query";
import { CreditCard } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatCurrency, formatDate } from "@/lib/format";
import { subscriptionsApi } from "@/lib/api/subscriptions";
import { ApiClientError } from "@/lib/api/client";
import { SubscriptionStatusBadge } from "@/features/subscriptions/subscription-status-badge";

function formatLimit(value: number | null): string {
  return value === null ? "Unlimited" : String(value);
}

export function ClientSubscriptionContent() {
  const { data: subscription, isLoading } = useQuery({
    queryKey: ["subscription", "own"],
    queryFn: async () => {
      try {
        return await subscriptionsApi.getOwn();
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Subscription" description="Your current plan and usage limits." />

      <Card>
        <CardContent className={!isLoading && subscription ? "pt-6" : "p-0"}>
          {isLoading ? (
            <ListSkeleton />
          ) : !subscription ? (
            <EmptyState
              icon={CreditCard}
              title="No plan assigned yet"
              description="Contact your account admin to get set up on a package."
            />
          ) : (
            <div className="space-y-6">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-xs text-muted-foreground">Plan</p>
                  <p className="text-xl font-semibold">{subscription.plan.name}</p>
                  {subscription.plan.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{subscription.plan.description}</p>
                  )}
                </div>
                <SubscriptionStatusBadge status={subscription.status} />
              </div>

              <div className="grid gap-4 border-t border-border pt-4 sm:grid-cols-2 md:grid-cols-4">
                <div>
                  <p className="text-xs text-muted-foreground">NFC cards</p>
                  <p className="font-medium">
                    {subscription.cardsUsed} / {formatLimit(subscription.plan.cardLimit)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Profiles</p>
                  <p className="font-medium">{formatLimit(subscription.plan.profileLimit)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Google Review locations</p>
                  <p className="font-medium">{formatLimit(subscription.plan.reviewLocationLimit)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Menus</p>
                  <p className="font-medium">{formatLimit(subscription.plan.menuLimit)}</p>
                </div>
              </div>

              <div className="flex flex-wrap gap-x-8 gap-y-2 border-t border-border pt-4 text-sm">
                <p>
                  <span className="text-muted-foreground">Price: </span>
                  {formatCurrency(subscription.plan.price)} / {subscription.plan.billingPeriod.toLowerCase()}
                </p>
                <p>
                  <span className="text-muted-foreground">Premium templates: </span>
                  {subscription.plan.premiumTemplates ? "Included" : "Not included"}
                </p>
                {subscription.renewalDate && (
                  <p>
                    <span className="text-muted-foreground">Renews: </span>
                    {formatDate(subscription.renewalDate)}
                  </p>
                )}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
