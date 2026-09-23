"use client";

import { useQuery } from "@tanstack/react-query";
import { ShoppingBag } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { formatCurrency } from "@/lib/format";
import { productsApi } from "@/lib/api/products";
import { ProductDialog } from "@/features/products/product-dialog";

export function ProductsPageContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["products"],
    queryFn: () => productsApi.list(),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Products"
        description="Physical cards and add-ons available to order for clients."
        actions={<ProductDialog />}
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load products.</div>
          ) : !data || data.length === 0 ? (
            <EmptyState
              icon={ShoppingBag}
              title="No products yet"
              description="Add a product to start creating orders for clients."
              action={<ProductDialog />}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>SKU</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Price</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody zebra>
                {data.map((product) => (
                  <TableRow key={product.uuid}>
                    <TableCell className="font-medium">{product.name}</TableCell>
                    <TableCell className="text-muted-foreground">{product.sku}</TableCell>
                    <TableCell className="text-muted-foreground">{product.type}</TableCell>
                    <TableCell>{formatCurrency(product.price)}</TableCell>
                    <TableCell>
                      <Badge variant={product.active ? "success" : "secondary"}>
                        {product.active ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <ProductDialog existing={product} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
