"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { KeyRound, LogOut, User } from "lucide-react";
import { toast } from "sonner";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth, useLogout } from "@/lib/providers/auth-provider";
import { NotificationBell } from "@/features/notifications/notification-bell";
import { ChangePasswordDialog } from "@/features/auth/change-password-dialog";

function initialsFor(email: string) {
  return email.slice(0, 2).toUpperCase();
}

export function DashboardHeader() {
  const { user } = useAuth();
  const router = useRouter();
  const logout = useLogout();
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  // Every account is created with a forced temporary password (AdminUserService.createAdminUser,
  // ClientService.createClient both set mustChangePassword: true) - this is the enforcement of
  // that flag, not just an optional menu item, so the dialog is also open whenever it's true,
  // independent of whether the "Change password" menu item was ever clicked.
  const isPasswordChangeForced = Boolean(user?.mustChangePassword);
  const passwordDialogIsOpen = passwordDialogOpen || isPasswordChangeForced;

  const handleLogout = async () => {
    await logout();
    toast.success("Signed out");
    router.push("/login");
  };

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-background px-4 sm:px-6 print:hidden">
      <div />

      <div className="flex items-center gap-3">
        <NotificationBell />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring">
              <Avatar className="ring-1 ring-primary">
                <AvatarFallback className="bg-foreground text-primary">
                  {user ? initialsFor(user.email) : <User className="size-4" />}
                </AvatarFallback>
              </Avatar>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>
              <div className="flex flex-col">
                <span className="text-sm font-medium">{user?.email}</span>
                <span className="text-xs text-muted-foreground">{user?.roles.join(", ")}</span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => setPasswordDialogOpen(true)}>
              <KeyRound className="size-4" />
              Change password
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleLogout} variant="destructive">
              <LogOut className="size-4" />
              Log out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <ChangePasswordDialog
        open={passwordDialogIsOpen}
        onOpenChange={setPasswordDialogOpen}
        forced={isPasswordChangeForced}
      />
    </header>
  );
}
