"use client";

import { useQuery } from "@tanstack/react-query";
import { LayoutTemplate } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ListSkeleton } from "@/components/ui/page-skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { templatesApi } from "@/lib/api/templates";
import { TemplateDialog } from "@/features/templates/template-dialog";

export function TemplatesPageContent() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["templates", "admin"],
    queryFn: () => templatesApi.listAdmin(),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Templates"
        description="Manage the profile template gallery clients can select from."
        actions={<TemplateDialog />}
      />

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <ListSkeleton />
          ) : isError ? (
            <div className="p-6 text-sm text-destructive">Failed to load templates.</div>
          ) : !data || data.length === 0 ? (
            <EmptyState
              icon={LayoutTemplate}
              title="No templates yet"
              description="Create a template to make it available in the client gallery."
              action={<TemplateDialog />}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Color</TableHead>
                  <TableHead>Layout</TableHead>
                  <TableHead>Access</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody zebra>
                {data
                  .slice()
                  .sort((a, b) => a.sortOrder - b.sortOrder)
                  .map((template) => (
                    <TableRow key={template.uuid}>
                      <TableCell className="font-medium">{template.name}</TableCell>
                      <TableCell>
                        <span className="inline-flex items-center gap-2">
                          <span
                            className="size-4 rounded-full border border-border"
                            style={{ backgroundColor: template.primaryColor }}
                          />
                          <span className="text-muted-foreground">{template.primaryColor}</span>
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{template.layout}</TableCell>
                      <TableCell>
                        <Badge variant={template.premium ? "warning" : "secondary"}>
                          {template.premium ? "Premium" : "Free"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={template.active ? "success" : "secondary"}>
                          {template.active ? "Active" : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <TemplateDialog existing={template} />
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
