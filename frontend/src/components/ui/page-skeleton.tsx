import { Skeleton } from "@/components/ui/skeleton";

/** Loading shape for a list page's table/card body — pair with Card/CardContent(p-0). */
export function ListSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-3 p-6">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-10 w-full" />
      ))}
    </div>
  );
}

/** Loading shape for a detail page — title row + a content block. */
export function DetailSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Skeleton className="h-8 w-8 rounded-md" />
        <Skeleton className="h-8 w-64" />
      </div>
      <Skeleton className="h-64 w-full" />
    </div>
  );
}
