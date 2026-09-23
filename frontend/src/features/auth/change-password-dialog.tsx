"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { authApi } from "@/lib/api/auth";
import { ApiClientError } from "@/lib/api/client";
import { useLogout } from "@/lib/providers/auth-provider";
import { changePasswordSchema, type ChangePasswordFormValues } from "@/lib/schemas/auth";

/**
 * Shared by the admin and client dashboards (mounted once in DashboardHeader) - the same
 * account-security surface either role needs, rather than a separate settings page per role.
 * `forced` (driven by UserMe.mustChangePassword - true for every account created with a
 * temporary password, both AdminUserService and ClientService) hides the close affordances and
 * blocks escape/outside-click dismissal, since a temporary password must actually be rotated
 * before the account is used, not just optionally offered.
 */
export function ChangePasswordDialog({
  open,
  onOpenChange,
  forced = false,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  forced?: boolean;
}) {
  const router = useRouter();
  const logout = useLogout();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({ resolver: zodResolver(changePasswordSchema) });

  const handleOpenChange = (next: boolean) => {
    if (forced && !next) return; // Non-dismissible while a password change is required.
    onOpenChange(next);
    if (!next) reset();
  };

  const onSubmit = async (values: ChangePasswordFormValues) => {
    try {
      await authApi.changePassword(values);
      toast.success("Password changed - please sign in again");
      // The backend revokes every refresh token for this account on a password change
      // (see AuthService.changePassword), so the current session won't survive its next
      // silent refresh anyway - logging out immediately avoids a confusing later forced logout.
      await logout();
      router.push("/login");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to change password");
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        hideCloseButton={forced}
        onEscapeKeyDown={(e) => forced && e.preventDefault()}
        onPointerDownOutside={(e) => forced && e.preventDefault()}
      >
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <DialogHeader>
            <DialogTitle>{forced ? "Set a new password" : "Change password"}</DialogTitle>
            <DialogDescription>
              {forced
                ? "Your account was created with a temporary password. Choose a new one to continue."
                : "You'll need to sign in again afterward."}
            </DialogDescription>
          </DialogHeader>

          <div className="mt-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="currentPassword">Current password</Label>
              <Input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.currentPassword)}
                {...register("currentPassword")}
              />
              {errors.currentPassword && (
                <p className="text-sm text-destructive">{errors.currentPassword.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="newPassword">New password</Label>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={Boolean(errors.newPassword)}
                {...register("newPassword")}
              />
              {errors.newPassword && <p className="text-sm text-destructive">{errors.newPassword.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirmPassword">Confirm new password</Label>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                aria-invalid={Boolean(errors.confirmPassword)}
                {...register("confirmPassword")}
              />
              {errors.confirmPassword && (
                <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
              )}
            </div>
          </div>

          <DialogFooter className="mt-6">
            {!forced && (
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                Cancel
              </Button>
            )}
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="size-4 animate-spin" />}
              Change password
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
