"use client";

import type { ReactNode } from "react";
import {
  LayoutDashboard,
  User,
  Nfc,
  QrCode,
  Star,
  UtensilsCrossed,
  LayoutTemplate,
  BarChart3,
  CreditCard,
  ShoppingCart,
  LifeBuoy,
} from "lucide-react";

import { DashboardSidebar, type SidebarNavItem } from "@/components/layout/dashboard-sidebar";
import { DashboardHeader } from "@/components/layout/dashboard-header";
import { RoleGuard } from "@/components/layout/role-guard";

const CLIENT_NAV: SidebarNavItem[] = [
  { label: "Dashboard", href: "/client/dashboard", icon: LayoutDashboard },
  { label: "My Profile", href: "/client/profile", icon: User },
  { label: "NFC Cards", href: "/client/nfc-cards", icon: Nfc },
  { label: "QR Codes", href: "/client/qr-codes", icon: QrCode },
  { label: "Google Reviews", href: "/client/google-reviews", icon: Star },
  { label: "Menus", href: "/client/menu", icon: UtensilsCrossed },
  { label: "Templates", href: "/client/templates", icon: LayoutTemplate },
  { label: "Analytics", href: "/client/analytics", icon: BarChart3 },
  { label: "Subscription", href: "/client/subscription", icon: CreditCard },
  { label: "Orders", href: "/client/orders", icon: ShoppingCart },
  { label: "Support", href: "/client/support", icon: LifeBuoy },
];

export default function ClientLayout({ children }: { children: ReactNode }) {
  return (
    <RoleGuard allowedRoles={["CLIENT"]}>
      <div className="flex min-h-full">
        <DashboardSidebar title="Client" items={CLIENT_NAV} />
        <div className="flex min-w-0 flex-1 flex-col">
          <DashboardHeader />
          <main className="flex-1 bg-white p-4 sm:p-6">{children}</main>
        </div>
      </div>
    </RoleGuard>
  );
}
