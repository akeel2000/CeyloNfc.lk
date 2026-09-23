"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, CheckCircle2, Loader2, Nfc, Trash2, UserRound } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { clientsApi } from "@/lib/api/clients";
import { nfcCardsApi } from "@/lib/api/nfc";
import { subscriptionsApi } from "@/lib/api/subscriptions";
import { ApiClientError } from "@/lib/api/client";
import { clientUpdateSchema, type ClientUpdateFormValues } from "@/lib/schemas/client";
import { ClientStatusBadge } from "@/features/clients/client-status-badge";
import { NfcStatusBadge } from "@/features/nfc/nfc-status-badge";
import { SubscriptionStatusBadge } from "@/features/subscriptions/subscription-status-badge";
import { AssignSubscriptionDialog } from "@/features/subscriptions/assign-subscription-dialog";
import type { Client } from "@/lib/types/client";

export function ClientDetailContent({ uuid }: { uuid: string }) {
  const queryClient = useQueryClient();

  const { data: client, isLoading } = useQuery({
    queryKey: ["clients", uuid],
    queryFn: () => clientsApi.get(uuid),
  });

  if (isLoading || !client) {
    return <DetailSkeleton />;
  }

  return <ClientDetailForm client={client} onChanged={() => queryClient.invalidateQueries({ queryKey: ["clients"] })} />;
}

function ClientDetailForm({ client, onChanged }: { client: Client; onChanged: () => void }) {
  const router = useRouter();
  const [deleteOpen, setDeleteOpen] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<ClientUpdateFormValues>({
    resolver: zodResolver(clientUpdateSchema),
    defaultValues: {
      displayName: client.displayName,
      email: client.email,
      phone: client.phone ?? "",
    },
  });

  const updateMutation = useMutation({
    mutationFn: (values: ClientUpdateFormValues) => clientsApi.update(client.uuid, values),
    onSuccess: () => {
      toast.success("Client updated");
      onChanged();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update client");
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: string) => clientsApi.updateStatus(client.uuid, status),
    onSuccess: (updated) => {
      toast.success(`Client ${updated.status === "ACTIVE" ? "activated" : "suspended"}`);
      onChanged();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => clientsApi.delete(client.uuid),
    onSuccess: () => {
      toast.success("Client deleted");
      onChanged();
      router.push("/admin/clients");
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to delete client");
      setDeleteOpen(false);
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/admin/clients"
        title={client.displayName}
        description={`${client.type} client`}
        titleExtra={<ClientStatusBadge status={client.status} />}
        actions={
          <>
            <Button asChild variant="outline">
              <Link href={`/admin/profile?clientUuid=${client.uuid}`}>
                <UserRound className="size-4" />
                Edit Profile
              </Link>
            </Button>
            {client.status === "SUSPENDED" ? (
              <Button
                variant="outline"
                onClick={() => statusMutation.mutate("ACTIVE")}
                disabled={statusMutation.isPending}
              >
                {statusMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <CheckCircle2 className="size-4" />}
                Activate
              </Button>
            ) : (
              client.status === "ACTIVE" && (
                <Button
                  variant="outline"
                  onClick={() => statusMutation.mutate("SUSPENDED")}
                  disabled={statusMutation.isPending}
                >
                  {statusMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />}
                  Suspend
                </Button>
              )
            )}
            <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
              <Trash2 className="size-4" />
              Delete
            </Button>
          </>
        }
      />

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Delete this client?"
        description={`This permanently removes ${client.displayName}'s access - their login is disabled and all active sessions are revoked immediately. Their historical records (orders, audit trail) are kept, not erased.`}
        confirmLabel="Delete client"
        destructive
        pending={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
      />

      <Card>
        <CardHeader>
          <CardTitle>Overview</CardTitle>
          <CardDescription>Basic client account details.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
            className="grid gap-4 sm:grid-cols-2"
            noValidate
          >
            <div className="space-y-2">
              <Label htmlFor="displayName">Display name</Label>
              <Input id="displayName" aria-invalid={Boolean(errors.displayName)} {...register("displayName")} />
              {errors.displayName && <p className="text-sm text-destructive">{errors.displayName.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" aria-invalid={Boolean(errors.email)} {...register("email")} />
              {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Phone</Label>
              <Input id="phone" {...register("phone")} />
            </div>
            <div className="flex items-end sm:col-span-2">
              <Button type="submit" disabled={!isDirty || updateMutation.isPending}>
                {updateMutation.isPending && <Loader2 className="size-4 animate-spin" />}
                Save changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <ClientSubscriptionSection clientUuid={client.uuid} />
      <ClientNfcCardsSection clientUuid={client.uuid} />
    </div>
  );
}

function ClientSubscriptionSection({ clientUuid }: { clientUuid: string }) {
  const { data: subscription, isLoading } = useQuery({
    queryKey: ["subscription", clientUuid],
    queryFn: async () => {
      try {
        return await subscriptionsApi.getForClient(clientUuid);
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
  });

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div>
          <CardTitle>Subscription</CardTitle>
          <CardDescription>Package plan and card/feature limits for this client.</CardDescription>
        </div>
        {!isLoading && <AssignSubscriptionDialog clientUuid={clientUuid} existing={subscription} />}
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : !subscription ? (
          <p className="text-sm text-muted-foreground">
            No subscription assigned yet - card and feature limits are unmanaged until a plan is assigned.
          </p>
        ) : (
          <div className="flex flex-wrap items-center gap-x-8 gap-y-3">
            <div>
              <p className="text-xs text-muted-foreground">Plan</p>
              <p className="font-medium">{subscription.plan.name}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Status</p>
              <SubscriptionStatusBadge status={subscription.status} />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Cards used</p>
              <p className="font-medium">
                {subscription.cardsUsed} / {subscription.plan.cardLimit ?? "Unlimited"}
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function ClientNfcCardsSection({ clientUuid }: { clientUuid: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ["nfc-cards", { clientUuid }],
    queryFn: () => nfcCardsApi.list({ clientUuid, size: 50 }),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>NFC Cards</CardTitle>
        <CardDescription>Cards assigned to this client.</CardDescription>
      </CardHeader>
      <CardContent className={data && data.content.length > 0 ? "-mx-6 -mb-6 border-t border-border p-0" : undefined}>
        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={Nfc}
            title="No NFC cards assigned"
            description="Register one from the NFC Cards page."
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Serial number</TableHead>
                <TableHead>Taps</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((card) => (
                <TableRow key={card.uuid}>
                  <TableCell className="font-medium">
                    <Link href={`/admin/nfc-cards/${card.uuid}`} className="hover:underline">
                      {card.serialNumber}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{card.totalTaps} taps</TableCell>
                  <TableCell>
                    <NfcStatusBadge status={card.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
