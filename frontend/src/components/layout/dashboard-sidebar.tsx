"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Menu, Nfc, type LucideIcon } from "lucide-react";

import { cn } from "@/lib/utils";
import { brand } from "@/lib/config/brand";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";

export interface SidebarNavItem {
  label: string;
  href?: string;
  icon: LucideIcon;
}

export function DashboardSidebar({
  title,
  items,
}: {
  title: string;
  items: SidebarNavItem[];
}) {
  const pathname = usePathname();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <>
      <aside className="hidden w-64 shrink-0 border-r border-sidebar-border bg-sidebar md:flex md:flex-col print:hidden">
        <SidebarBrand title={title} />
        <SidebarNav items={items} pathname={pathname} />
      </aside>

      <Dialog open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
        <DialogTrigger asChild>
          <button
            type="button"
            className="fixed bottom-4 left-4 z-40 flex size-12 items-center justify-center rounded-full bg-sidebar text-sidebar-foreground shadow-lg ring-1 ring-sidebar-border md:hidden print:hidden"
            aria-label="Open navigation"
          >
            <Menu className="size-5" />
          </button>
        </DialogTrigger>
        <DialogContent className="max-h-[calc(100vh-1rem)] w-[calc(100vw-1rem)] max-w-sm gap-0 overflow-hidden bg-sidebar p-0 text-sidebar-foreground">
          <DialogHeader className="border-b border-sidebar-border px-5 py-4 text-left">
            <DialogTitle className="text-sidebar-foreground">{brand.name}</DialogTitle>
            <DialogDescription className="text-sidebar-foreground/60">{title} navigation</DialogDescription>
          </DialogHeader>
          <SidebarNav items={items} pathname={pathname} onNavigate={() => setMobileNavOpen(false)} mobile />
        </DialogContent>
      </Dialog>
    </>
  );
}

function SidebarBrand({ title }: { title: string }) {
  return (
    <div className="flex h-16 items-center gap-2 border-b border-sidebar-border px-5 font-semibold tracking-tight text-sidebar-foreground">
      <span className="flex size-8 items-center justify-center rounded-md bg-sidebar-primary text-sidebar-primary-foreground">
        <Nfc className="size-4" />
      </span>
      <div className="flex flex-col leading-tight">
        <span>{brand.name}</span>
        <span className="text-xs font-normal text-sidebar-foreground/50">{title}</span>
      </div>
    </div>
  );
}

function SidebarNav({
  items,
  pathname,
  onNavigate,
  mobile = false,
}: {
  items: SidebarNavItem[];
  pathname: string;
  onNavigate?: () => void;
  mobile?: boolean;
}) {
  return (
    <nav className={cn("flex-1 space-y-0.5 overflow-y-auto px-3 py-4", mobile && "pb-8")}>
      {items.map((item) => {
        const isActive = item.href ? pathname === item.href || pathname.startsWith(`${item.href}/`) : false;

        if (!item.href) {
          return (
            <div
              key={item.label}
              className="flex cursor-not-allowed items-center justify-between rounded-md px-3 py-2 text-sm text-sidebar-foreground/35"
              aria-disabled="true"
              title="Not available yet"
            >
              <span className="flex items-center gap-2">
                <item.icon className="size-4" />
                {item.label}
              </span>
              <span className="rounded-full bg-sidebar-foreground/10 px-1.5 py-0.5 text-[10px] font-medium text-sidebar-foreground/60">Soon</span>
            </div>
          );
        }

        return (
          <Link
            key={item.label}
            href={item.href}
            onClick={onNavigate}
            className={cn(
              "flex min-h-11 items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              isActive
                ? "bg-sidebar-accent text-sidebar-accent-foreground"
                : "text-sidebar-foreground/80 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            )}
          >
            <item.icon className="size-4" />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
