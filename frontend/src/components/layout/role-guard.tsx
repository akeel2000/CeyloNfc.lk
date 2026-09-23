"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";

import { useAuth } from "@/lib/providers/auth-provider";

/**
 * UX-only route guard. The backend independently re-checks authentication,
 * role and tenant ownership on every request - this only avoids flashing
 * protected UI before redirecting an unauthenticated/unauthorized visitor.
 */
export function RoleGuard({ allowedRoles, children }: { allowedRoles: string[]; children: ReactNode }) {
  const { user, isLoading, isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    if (user && !user.roles.some((role) => allowedRoles.includes(role))) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, user, allowedRoles, router]);

  if (isLoading || !isAuthenticated || (user && !user.roles.some((role) => allowedRoles.includes(role)))) {
    return (
      <div className="flex min-h-full flex-1 items-center justify-center py-24">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return <>{children}</>;
}
