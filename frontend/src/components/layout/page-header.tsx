import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function PageHeader({
  title,
  description,
  backHref,
  titleExtra,
  actions,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  /** Renders a back icon button before the title, e.g. "/admin/clients". */
  backHref?: string;
  /** Extra inline content next to the title, e.g. a status badge. */
  titleExtra?: ReactNode;
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col justify-between gap-4 sm:flex-row sm:items-center", className)}>
      <div className="flex items-start gap-3">
        {backHref && (
          <Button asChild variant="outline" size="icon" className="mt-0.5 shrink-0">
            <Link href={backHref} aria-label="Back">
              <ArrowLeft className="size-4" />
            </Link>
          </Button>
        )}
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
            {titleExtra}
          </div>
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
      </div>

      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}
