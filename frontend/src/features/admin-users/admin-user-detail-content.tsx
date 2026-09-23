"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, CheckCircle2, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { DetailSkeleton } from "@/components/ui/page-skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { adminUsersApi } from "@/lib/api/admin-users";
import { ApiClientError } from "@/lib/api/client";
import { AdminUserStatusBadge } from "@/features/admin-users/admin-user-status-badge";
import { useAuth } from "@/lib/providers/auth-provider";
import type { AdminUser, Permission } from "@/lib/types/admin-user";

export function AdminUserDetailContent({ uuid }: { uuid: string }) {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();

  const { data: user, isLoading } = useQuery({
    queryKey: ["admin-users", uuid],
    queryFn: () => adminUsersApi.get(uuid),
  });

  if (isLoading || !user) {
    return <DetailSkeleton />;
  }

  return (
    <AdminUserDetailView
      user={user}
      isSelf={currentUser?.uuid === user.uuid}
      onChanged={() => {
        queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      }}
    />
  );
}

function AdminUserDetailView({
  user,
  isSelf,
  onChanged,
}: {
  user: AdminUser;
  isSelf: boolean;
  onChanged: () => void;
}) {
  const isSuperAdmin = user.roles.includes("SUPER_ADMIN");

  const statusMutation = useMutation({
    mutationFn: (status: string) => adminUsersApi.updateStatus(user.uuid, status),
    onSuccess: (updated) => {
      toast.success(`User ${updated.status === "ACTIVE" ? "activated" : "suspended"}`);
      onChanged();
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update status");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        backHref="/admin/users"
        title={user.email}
        description={
          <span className="mt-1 flex flex-wrap gap-1">
            {user.roles.map((role) => (
              <Badge key={role} variant="secondary">
                {role}
              </Badge>
            ))}
          </span>
        }
        titleExtra={<AdminUserStatusBadge status={user.status} />}
        actions={
          !isSelf &&
          (user.status === "ACTIVE" ? (
            <Button
              variant="outline"
              onClick={() => statusMutation.mutate("DISABLED")}
              disabled={statusMutation.isPending}
            >
              {statusMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />}
              Disable
            </Button>
          ) : (
            <Button
              variant="outline"
              onClick={() => statusMutation.mutate("ACTIVE")}
              disabled={statusMutation.isPending}
            >
              {statusMutation.isPending ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <CheckCircle2 className="size-4" />
              )}
              Activate
            </Button>
          ))
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Permissions</CardTitle>
          <CardDescription>
            {isSuperAdmin
              ? "Super Admin has every permission automatically - overrides don't apply."
              : "Grant this Admin access to specific areas of the platform. Nothing is granted by default."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isSuperAdmin ? (
            <p className="text-sm text-muted-foreground">No permissions to configure.</p>
          ) : (
            <PermissionsEditor userUuid={user.uuid} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function PermissionsEditor({ userUuid }: { userUuid: string }) {
  const { data: permissions, isLoading } = useQuery({
    queryKey: ["admin-users", userUuid, "permissions"],
    queryFn: () => adminUsersApi.getPermissions(userUuid),
  });

  if (isLoading || !permissions) {
    return <Skeleton className="h-64 w-full" />;
  }

  return <PermissionsForm userUuid={userUuid} permissions={permissions} />;
}

/** Mounted only once `permissions` has loaded, so the initial selection can be derived
 *  straight from the query data via a lazy useState initializer instead of mirroring it
 *  into state through an effect (see docs/PROJECT_PROGRESS.md's client-combobox note for
 *  why - ESLint's react-hooks/set-state-in-effect correctly flags that pattern). */
function PermissionsForm({ userUuid, permissions }: { userUuid: string; permissions: Permission[] }) {
  const queryClient = useQueryClient();
  const initialGranted = permissions.filter((p) => p.granted).map((p) => p.code);
  const [selected, setSelected] = useState<Set<string>>(() => new Set(initialGranted));
  // Tracks the last-saved set so the dirty check stays correct after a save, without
  // depending on `permissions` (a prop that only updates once the parent's query refetches).
  const [savedSelected, setSavedSelected] = useState<Set<string>>(() => new Set(initialGranted));

  const updateMutation = useMutation({
    mutationFn: (codes: string[]) => adminUsersApi.updatePermissions(userUuid, codes),
    onSuccess: (updated) => {
      toast.success("Permissions updated");
      queryClient.setQueryData(["admin-users", userUuid, "permissions"], updated);
      const grantedNow = new Set(updated.filter((p) => p.granted).map((p) => p.code));
      setSelected(grantedNow);
      setSavedSelected(grantedNow);
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to update permissions");
    },
  });

  const toggle = (code: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(code)) {
        next.delete(code);
      } else {
        next.add(code);
      }
      return next;
    });
  };

  const isDirty =
    selected.size !== savedSelected.size || [...selected].some((code) => !savedSelected.has(code));

  return (
    <div className="space-y-4">
      <div className="grid gap-2 sm:grid-cols-2">
        {permissions.map((permission) => (
          <PermissionRow
            key={permission.code}
            permission={permission}
            checked={selected.has(permission.code)}
            onToggle={() => toggle(permission.code)}
          />
        ))}
      </div>
      <Button
        onClick={() => updateMutation.mutate(Array.from(selected))}
        disabled={!isDirty || updateMutation.isPending}
      >
        {updateMutation.isPending && <Loader2 className="size-4 animate-spin" />}
        Save changes
      </Button>
    </div>
  );
}

function PermissionRow({
  permission,
  checked,
  onToggle,
}: {
  permission: Permission;
  checked: boolean;
  onToggle: () => void;
}) {
  return (
    <label className="flex cursor-pointer items-start gap-3 rounded-md border border-border p-3 hover:bg-accent">
      <input
        type="checkbox"
        checked={checked}
        onChange={onToggle}
        className="mt-0.5 size-4 accent-primary"
      />
      <div>
        <p className="text-sm font-medium">{permission.code}</p>
        {permission.description && (
          <p className="text-xs text-muted-foreground">{permission.description}</p>
        )}
      </div>
    </label>
  );
}
