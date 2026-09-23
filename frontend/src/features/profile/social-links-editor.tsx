"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { profileApi, adminProfileApi } from "@/lib/api/profile";
import { ApiClientError } from "@/lib/api/client";
import type { SocialLink, SocialPlatform } from "@/lib/types/profile";

const PLATFORMS: SocialPlatform[] = [
  "FACEBOOK",
  "INSTAGRAM",
  "LINKEDIN",
  "TIKTOK",
  "YOUTUBE",
  "X",
  "TELEGRAM",
  "WHATSAPP",
  "CUSTOM",
];

export function SocialLinksEditor({
  initialLinks,
  clientUuid,
}: {
  initialLinks: SocialLink[];
  clientUuid?: string;
}) {
  const [links, setLinks] = useState<SocialLink[]>(initialLinks);
  const queryClient = useQueryClient();

  const saveMutation = useMutation({
    mutationFn: () => (clientUuid ? adminProfileApi.updateSocialLinks(clientUuid, links) : profileApi.updateSocialLinks(links)),
    onSuccess: (saved) => {
      setLinks(saved);
      toast.success("Social links saved");
      queryClient.invalidateQueries({ queryKey: clientUuid ? ["admin", "profile", clientUuid] : ["client", "profile"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to save social links");
    },
  });

  const addLink = () => {
    setLinks((prev) => [...prev, { platform: "FACEBOOK", url: "", displayOrder: prev.length, enabled: true }]);
  };

  const updateLink = (index: number, patch: Partial<SocialLink>) => {
    setLinks((prev) => prev.map((link, i) => (i === index ? { ...link, ...patch } : link)));
  };

  const removeLink = (index: number) => {
    setLinks((prev) => prev.filter((_, i) => i !== index).map((link, i) => ({ ...link, displayOrder: i })));
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Social links</CardTitle>
        <CardDescription>Shown as icons on your public profile.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {links.map((link, index) => (
          <div key={index} className="flex items-center gap-2">
            <Select value={link.platform} onValueChange={(value) => updateLink(index, { platform: value as SocialPlatform })}>
              <SelectTrigger className="w-40 shrink-0">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PLATFORMS.map((platform) => (
                  <SelectItem key={platform} value={platform}>
                    {platform}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input
              placeholder="https://..."
              value={link.url}
              onChange={(e) => updateLink(index, { url: e.target.value })}
            />
            <Button type="button" variant="ghost" size="icon" onClick={() => removeLink(index)}>
              <Trash2 className="size-4" />
            </Button>
          </div>
        ))}

        <div className="flex items-center justify-between pt-2">
          <Button type="button" variant="outline" size="sm" onClick={addLink}>
            <Plus className="size-4" />
            Add link
          </Button>
          <Button type="button" size="sm" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            {saveMutation.isPending && <Loader2 className="size-4 animate-spin" />}
            Save links
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
