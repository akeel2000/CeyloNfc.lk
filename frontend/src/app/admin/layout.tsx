"use client";

import type { ReactNode } from "react";
import {
  LayoutDashboard,
  Users,
  UserRound,
  Nfc,
  PenLine,
  QrCode,
  Star,
  UtensilsCrossed,
  LayoutTemplate,
  Package,
  CreditCard,
  Wallet,
  ShoppingCart,
  Contact,
  BarChart3,
  LifeBuoy,
  UserCog,
  ScrollText,
  Settings,
} from "lucide-react";

import { DashboardSidebar, type SidebarNavItem } from "@/components/layout/dashboard-sidebar";
import { DashboardHeader } from "@/components/layout/dashboard-header";
import { RoleGuard } from "@/components/layout/role-guard";

const ADMIN_NAV: SidebarNavItem[] = [
  { label: "Dashboard", href: "/admin/dashboard", icon: LayoutDashboard },
  { label: "Clients", href: "/admin/clients", icon: Users },
  { label: "Profiles", href: "/admin/profile", icon: UserRound },
  { label: "NFC Cards", href: "/admin/nfc-cards", icon: Nfc },
  { label: "Write NFC", href: "/admin/write-nfc", icon: PenLine },
  { label: "QR Codes", href: "/admin/qr-codes", icon: QrCode },
  { label: "Google Reviews", href: "/admin/google-reviews", icon: Star },
  { label: "Menus", href: "/admin/menu", icon: UtensilsCrossed },
  { label: "Templates", href: "/admin/templates", icon: LayoutTemplate },
  { label: "Products", href: "/admin/products", icon: Package },
  { label: "Packages", href: "/admin/packages", icon: CreditCard },
  { label: "Subscriptions", href: "/admin/subscriptions", icon: Wallet },
  { label: "Orders", href: "/admin/orders", icon: ShoppingCart },
  { label: "Leads", href: "/admin/leads", icon: Contact },
  { label: "Analytics", href: "/admin/analytics", icon: BarChart3 },
  { label: "Support", href: "/admin/support", icon: LifeBuoy },
  { label: "Users", href: "/admin/users", icon: UserCog },
  { label: "Audit Logs", href: "/admin/audit-logs", icon: ScrollText },
  { label: "Settings", href: "/admin/settings", icon: Settings },
];

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <RoleGuard allowedRoles={["SUPER_ADMIN", "ADMIN"]}>
      <div className="flex min-h-full">
        <DashboardSidebar title="Admin" items={ADMIN_NAV} />
        <div className="flex min-w-0 flex-1 flex-col">
          <DashboardHeader />
          <main className="flex-1 bg-secondary/30 p-4 sm:p-6">{children}</main>
        </div>
      </div>
    </RoleGuard>
  );
}
