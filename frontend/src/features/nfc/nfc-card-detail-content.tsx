"use client";

import Link from "next/link";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, CheckCircle2, Link2, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { ClientCombobox } from "@/components/admin/client-combobox";
import { formatDateTime } from "@/lib/format";
import { nfcCardsApi, destinationsApi } from "@/lib/api/nfc";
import { clientsApi } from "@/lib/api/clients";
import { adminReviewLocationsApi } from "@/lib/api/review";
import { ApiClientError } from "@/lib/api/client";
import { nfcCardAssignSchema, type NfcCardAssignFormValues } from "@/lib/schemas/nfc";
import { NfcStatusBadge } from "@/features/nfc/nfc-status-badge";
import { ReplaceNfcCardDialog } from "@/features/nfc/replace-nfc-card-dialog";
import type { NfcCard } from "@/lib/types/nfc";

export function NfcCardDetailContent({ uuid }: { uuid: string }) {
  const queryClient = useQueryClient();

  const { data: card, isLoading } = useQuery({
    queryKey: ["nfc-cards", uuid],
    queryFn: () => nfcCardsApi.get(uuid),
  });

  if (isLoading || !card) {
    return <DetailSkeleton />;
  }

  return <NfcCardDetail card={card} onChanged={() => queryClient.invalidateQueries({ queryKey: ["nfc-cards"] })} />;
}

function NfcCardDetail({ card, onChanged }: { card: NfcCard; onChanged: () => void }) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<NfcCardAssignFormValues>({
    resolver: zodResolver(nfcCardAssignSchema),
    defaultValues: { destinationType: "WEBSITE" },
  });
  const destinationType = useWatch({ control, name: "destinationType" });
  const selectedClientUuid = useWatch({ control, name: "clientUuid" });
  const { data: selectedClient } = useQuery({
    queryKey: ["clients", selectedClientUuid],
    queryFn: () => clientsApi.get(selectedClientUuid!),
    enabled: Boolean(selectedClientUuid),
  });
  const isUrlBasedType = ["WEBSITE", "WHATSAPP", "SOCIAL", "CUSTOM_URL"].includes(destinationType);
  const isGoogleReview = destinationType === "GOOGLE_REVIEW";

  const { data: reviewLocations } = useQuery({
    queryKey: ["admin", "google-reviews", selectedClientUuid],
    queryFn: () => adminReviewLocationsApi.listForClient(selectedClientUuid),
    enabled: Boolean(selectedClientUuid) && isGoogleReview,
  });

  const assignMutation = useMutation({
    mutationFn: async (values: NfcCardAssignFormValues) => {
      const destination = await destinationsApi.create({
        clientUuid: values.clientUuid,
        name: values.destinationName,
        type: values.destinationType,
        externalUrl: values.externalUrl,
        googleReviewLocationUuid: values.googleReviewLocationUuid,
      });
      return nfcCardsApi.assign(card.uuid, values.clientUuid, destination.uuid);
    },
    onSuccess: () => {
      toast.success("Card assigned and activated");
      onChanged();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to assign card");
    },
  });

  const statusMutation = useMutation({
    mutationFn: (action: "activate" | "suspend") =>
      action === "activate" ? nfcCardsApi.activate(card.uuid) : nfcCardsApi.suspend(card.uuid),
    onSuccess: (updated) => {
      toast.success(`Card ${updated.status === "ACTIVE" ? "activated" : "suspended"}`);
      onChanged();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  const isAssigned = Boolean(card.clientUuid && card.destinationUuid);

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/admin/nfc-cards"
        title={card.serialNumber}
        description={
          <>
            {card.totalTaps} tap{card.totalTaps === 1 ? "" : "s"}
            {card.lastTappedAt && ` · last tapped ${formatDateTime(card.lastTappedAt)}`}
          </>
        }
        titleExtra={<NfcStatusBadge status={card.status} />}
        actions={
          <>
            {isAssigned && card.status === "SUSPENDED" && (
              <Button variant="outline" onClick={() => statusMutation.mutate("activate")} disabled={statusMutation.isPending}>
                {statusMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
                Activate
              </Button>
            )}
            {card.status === "ACTIVE" && (
              <Button variant="outline" onClick={() => statusMutation.mutate("suspend")} disabled={statusMutation.isPending}>
                {statusMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />}
                Suspend
              </Button>
            )}
            {isAssigned && card.status !== "REPLACED" && (
              <ReplaceNfcCardDialog cardUuid={card.uuid} onReplaced={onChanged} />
            )}
          </>
        }
      />

      {isAssigned ? (
        <Card>
          <CardHeader>
            <CardTitle>Assignment</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <p className="text-muted-foreground">Client</p>
              <Link href={`/admin/clients/${card.clientUuid}`} className="font-medium hover:underline">
                {card.clientDisplayName}
              </Link>
            </div>
            <div>
              <p className="text-muted-foreground">Destination</p>
              <p className="flex items-center gap-1 font-medium">
                <Link2 className="size-3.5" />
                {card.destinationName}
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Assign this card</CardTitle>
            <CardDescription>
              Pick a client and where the card should redirect to. This also activates the card.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form
              onSubmit={handleSubmit((values) => assignMutation.mutate(values))}
              className="grid gap-4 sm:grid-cols-2"
              noValidate
            >
              <div className="space-y-2 sm:col-span-2">
                <Label>Client</Label>
                <Controller
                  name="clientUuid"
                  control={control}
                  render={({ field }) => (
                    <ClientCombobox value={field.value} onChange={field.onChange} placeholder="Search clients..." />
                  )}
                />
                {errors.clientUuid && <p className="text-sm text-destructive">{errors.clientUuid.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="destinationName">Destination name</Label>
                <Input
                  id="destinationName"
                  placeholder="e.g. Main Website"
                  aria-invalid={Boolean(errors.destinationName)}
                  {...register("destinationName")}
                />
                {errors.destinationName && (
                  <p className="text-sm text-destructive">{errors.destinationName.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Destination type</Label>
                <Controller
                  name="destinationType"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {selectedClient?.type === "INDIVIDUAL" && (
                          <SelectItem value="PROFILE">Digital Profile</SelectItem>
                        )}
                        {selectedClient?.type === "BUSINESS" && (
                          <SelectItem value="COMPANY_PROFILE">Company Profile</SelectItem>
                        )}
                        <SelectItem value="VCARD">Download vCard</SelectItem>
                        <SelectItem value="MENU">Menu</SelectItem>
                        <SelectItem value="GOOGLE_REVIEW">Google Review</SelectItem>
                        <SelectItem value="WEBSITE">Website</SelectItem>
                        <SelectItem value="WHATSAPP">WhatsApp</SelectItem>
                        <SelectItem value="SOCIAL">Social</SelectItem>
                        <SelectItem value="CUSTOM_URL">Custom URL</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>

              {isUrlBasedType ? (
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="externalUrl">
                    {destinationType === "WHATSAPP" ? "WhatsApp link" : "URL"}
                  </Label>
                  <Input
                    id="externalUrl"
                    placeholder="https://..."
                    aria-invalid={Boolean(errors.externalUrl)}
                    {...register("externalUrl")}
                  />
                  {errors.externalUrl && <p className="text-sm text-destructive">{errors.externalUrl.message}</p>}
                </div>
              ) : isGoogleReview ? (
                <div className="space-y-2 sm:col-span-2">
                  <Label>Review location</Label>
                  <Controller
                    name="googleReviewLocationUuid"
                    control={control}
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a location" />
                        </SelectTrigger>
                        <SelectContent>
                          {reviewLocations?.map((loc) => (
                            <SelectItem key={loc.uuid} value={loc.uuid}>
                              {loc.businessName}
                              {loc.locationName ? ` - ${loc.locationName}` : ""}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {errors.googleReviewLocationUuid && (
                    <p className="text-sm text-destructive">{errors.googleReviewLocationUuid.message}</p>
                  )}
                  {selectedClientUuid && !reviewLocations?.length && (
                    <p className="text-xs text-muted-foreground">
                      This client has no Google Review locations yet.
                    </p>
                  )}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground sm:col-span-2">
                  This will point to the client&apos;s {destinationType === "MENU" ? "menu" : "digital profile"} -
                  it stays in sync automatically whenever they update it.
                </p>
              )}

              <div className="sm:col-span-2">
                <Button type="submit" disabled={assignMutation.isPending}>
                  {assignMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                  Assign & Activate
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
