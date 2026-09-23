import { UtensilsCrossed, Star } from "lucide-react";

import { EmptyState } from "@/components/ui/empty-state";
import { brand } from "@/lib/config/brand";
import type { Menu } from "@/lib/types/menu";

export function PublicMenuView({ menu }: { menu: Menu }) {
  const activeCategories = menu.categories.filter((c) => c.active);

  return (
    <div className="mx-auto min-h-full max-w-lg bg-background pb-16">
      <div className="relative overflow-hidden border-b border-border px-6 py-12 text-center">
        <div className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-b from-primary/10 via-secondary/40 to-background" />
        <span className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/20">
          <UtensilsCrossed className="size-7" />
        </span>
        <h1 className="mt-4 text-3xl font-semibold tracking-tight">{menu.name}</h1>
        {menu.description && (
          <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">{menu.description}</p>
        )}
      </div>

      <div className="space-y-8 px-4 py-6">
        {activeCategories.length === 0 ? (
          <EmptyState
            icon={UtensilsCrossed}
            title="This menu is empty right now"
            description="Check back soon - items will appear here once they're added."
          />
        ) : (
          activeCategories.map((category) => (
            <section key={category.uuid}>
              <div className="mb-3 flex items-center gap-2">
                <span className="h-4 w-1 rounded-full bg-primary" aria-hidden="true" />
                <h2 className="text-lg font-semibold tracking-tight">{category.name}</h2>
              </div>
              <div className="space-y-3">
                {category.items.map((item) => (
                  <div
                    key={item.uuid}
                    className={`flex items-start justify-between gap-4 rounded-xl border border-border bg-card p-4 shadow-sm transition-shadow hover:shadow-md ${
                      item.available ? "" : "opacity-50"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      {item.image && (
                        // eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images
                        <img
                          src={item.image}
                          alt=""
                          className="size-14 shrink-0 rounded-lg object-cover"
                        />
                      )}
                      <div>
                        <p className="flex items-center gap-2 font-medium">
                          {item.name}
                          {item.featured && <Star className="size-3.5 fill-warning text-warning" />}
                        </p>
                        {item.description && (
                          <p className="mt-1 text-sm text-muted-foreground">{item.description}</p>
                        )}
                        {!item.available && (
                          <p className="mt-1 text-xs font-medium text-destructive">Unavailable</p>
                        )}
                      </div>
                    </div>
                    <span className="shrink-0 font-semibold">
                      {menu.currency} {item.price.toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          ))
        )}
      </div>

      <p className="text-center text-xs text-muted-foreground">Powered by {brand.name}</p>
    </div>
  );
}
