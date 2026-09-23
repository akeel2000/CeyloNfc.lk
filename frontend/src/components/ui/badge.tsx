import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        destructive: "border-transparent bg-destructive text-destructive-foreground",
        success: "border-transparent bg-success/15 text-success dark:text-success",
        warning: "border-transparent bg-warning/15 text-warning dark:text-warning",
        info: "border-transparent bg-info/15 text-info dark:text-info",
        outline: "text-foreground border-border",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

const dotColors: Record<string, string> = {
  default: "bg-primary-foreground",
  secondary: "bg-secondary-foreground",
  destructive: "bg-destructive-foreground",
  success: "bg-success",
  warning: "bg-warning",
  info: "bg-info",
  outline: "bg-foreground",
};

function Badge({
  className,
  variant,
  dot,
  children,
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants> & { dot?: boolean }) {
  return (
    <span
      data-slot="badge"
      className={cn(badgeVariants({ variant }), className)}
      {...props}
    >
      {dot && (
        <span
          className={cn("mr-1 size-1.5 rounded-full", dotColors[variant ?? "default"])}
          aria-hidden="true"
        />
      )}
      {children}
    </span>
  );
}

export { Badge, badgeVariants };
