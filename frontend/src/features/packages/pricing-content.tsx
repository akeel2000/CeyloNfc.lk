"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Building2, Check, PackageSearch, Sparkles, User } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { packagesApi } from "@/lib/api/packages";
import type { PackagePlan } from "@/lib/types/commerce";

function formatLimit(label: string, value: number | null): string | null {
  if (value === null) return `Unlimited ${label}`;
  if (value === 0) return null;
  return `${value} ${label}${value === 1 ? "" : "s"}`;
}

function PlanCard({ plan, highlighted }: { plan: PackagePlan; highlighted: boolean }) {
  const features = [
    formatLimit("NFC card", plan.cardLimit),
    formatLimit("profile", plan.profileLimit),
    formatLimit("menu", plan.menuLimit),
    plan.premiumTemplates ? "Premium templates" : null,
  ].filter((f): f is string => Boolean(f));

  return (
    <Card
      className={cn(
        "relative flex h-full flex-col",
        highlighted && "border-primary shadow-lg shadow-primary/10 ring-1 ring-primary"
      )}
    >
      {highlighted && (
        <span className="absolute -top-3 left-1/2 flex -translate-x-1/2 items-center gap-1 rounded-full bg-primary px-3 py-1 text-xs font-medium text-primary-foreground">
          <Sparkles className="size-3" />
          Most popular
        </span>
      )}
      <CardHeader>
        <CardTitle>{plan.name}</CardTitle>
        {plan.description && <CardDescription>{plan.description}</CardDescription>}
        <div className="mt-4 flex items-baseline gap-1">
          <span className="text-3xl font-semibold tracking-tight">Rs {plan.price.toFixed(0)}</span>
          {plan.billingPeriod !== "ONE_TIME" && (
            <span className="text-sm text-muted-foreground">
              / {plan.billingPeriod === "MONTHLY" ? "month" : "year"}
            </span>
          )}
        </div>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col justify-between gap-6">
        <ul className="space-y-2 text-sm">
          {features.map((feature) => (
            <li key={feature} className="flex items-center gap-2">
              <Check className="size-4 shrink-0 text-success" />
              {feature}
            </li>
          ))}
        </ul>
        <Button asChild className="w-full" variant={highlighted ? "default" : "outline"}>
          <Link href="/#contact">Get started</Link>
        </Button>
      </CardContent>
    </Card>
  );
}

function PlanGroup({
  title,
  description,
  Icon,
  plans,
}: {
  title: string;
  description: string;
  Icon: typeof User;
  plans: PackagePlan[];
}) {
  return (
    <div>
      <div className="flex items-center gap-2.5">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
          <Icon className="size-4" />
        </span>
        <div>
          <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
      <div className="mt-6 grid gap-6 sm:grid-cols-2">
        {plans.map((plan, index) => (
          <PlanCard key={plan.uuid} plan={plan} highlighted={index === plans.length - 1 && plans.length > 1} />
        ))}
      </div>
    </div>
  );
}

export function PricingContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["packages", "public"],
    queryFn: () => packagesApi.listPublic(),
  });

  if (isLoading) {
    return (
      <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-96 w-full" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <p className="mt-12 text-center text-sm text-destructive">
        Failed to load pricing. Please try again shortly.
      </p>
    );
  }

  const plans = (data ?? []).slice().sort((a, b) => a.sortOrder - b.sortOrder);

  if (plans.length === 0) {
    return (
      <div className="mt-12 flex flex-col items-center gap-2 py-16 text-center">
        <PackageSearch className="size-8 text-muted-foreground" />
        <p className="text-sm font-medium">Pricing is being finalized</p>
        <p className="max-w-xs text-sm text-muted-foreground">
          Reach out via the contact section below and we&apos;ll help you pick a plan.
        </p>
        <Button asChild className="mt-2">
          <Link href="/#contact">Contact us</Link>
        </Button>
      </div>
    );
  }

  const individualPlans = plans.filter((p) => /individual/i.test(p.name));
  const companyPlans = plans.filter((p) => /company|business/i.test(p.name));
  const otherPlans = plans.filter((p) => !individualPlans.includes(p) && !companyPlans.includes(p));

  return (
    <div className="mt-12 space-y-14">
      {individualPlans.length > 0 && (
        <PlanGroup
          title="For individuals"
          description="One NFC card, one profile - perfect for freelancers and professionals."
          Icon={User}
          plans={individualPlans}
        />
      )}
      {companyPlans.length > 0 && (
        <PlanGroup
          title="For businesses"
          description="A branded company profile with menu and review-location support."
          Icon={Building2}
          plans={companyPlans}
        />
      )}
      {otherPlans.length > 0 && (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {otherPlans.map((plan) => (
            <PlanCard key={plan.uuid} plan={plan} highlighted={false} />
          ))}
        </div>
      )}
    </div>
  );
}
