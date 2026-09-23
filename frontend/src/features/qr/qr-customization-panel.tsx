"use client";

import { useId, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import QRCode from "qrcode";
import { Download, Loader2, X } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { QrImage, DEFAULT_QR_STYLE, type QrStyle } from "@/features/qr/qr-image";

const ERROR_CORRECTION_OPTIONS: { value: QrStyle["errorCorrectionLevel"]; label: string }[] = [
  { value: "L", label: "Low (~7% recovery)" },
  { value: "M", label: "Medium (~15% recovery)" },
  { value: "Q", label: "Quartile (~25% recovery)" },
  { value: "H", label: "High (~30% recovery) - needed for a logo" },
];

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/** Injects a centered white-backed <image> into a QRCode.toString svg output, matching what
 *  QrImage draws on canvas - kept in sync manually since the svg and canvas renderers are two
 *  separate code paths in the `qrcode` library with no shared logo-overlay support. */
function embedLogoInSvg(svg: string, logoDataUrl: string, size: number): string {
  const logoSize = size * 0.22;
  const pad = logoSize * 0.16;
  const x = (size - logoSize) / 2;
  const y = (size - logoSize) / 2;
  const overlay = `<rect x="${x - pad}" y="${y - pad}" width="${logoSize + pad * 2}" height="${logoSize + pad * 2}" fill="white"/><image href="${logoDataUrl}" x="${x}" y="${y}" width="${logoSize}" height="${logoSize}"/>`;
  return svg.replace("</svg>", `${overlay}</svg>`);
}

export function QrCustomizationPanel({ publicUrl, fileName }: { publicUrl: string; fileName: string }) {
  const canvasId = useId();
  const [style, setStyle] = useState<QrStyle>(DEFAULT_QR_STYLE);

  const setLogo = async (file: File | null) => {
    if (!file) {
      setStyle((prev) => ({ ...prev, logoDataUrl: null }));
      return;
    }
    const dataUrl = await readFileAsDataUrl(file);
    setStyle((prev) => ({
      ...prev,
      logoDataUrl: dataUrl,
      errorCorrectionLevel: prev.errorCorrectionLevel === "H" ? prev.errorCorrectionLevel : "H",
    }));
  };

  const downloadPng = () => {
    const canvas = document.querySelector<HTMLCanvasElement>(`[data-qr-canvas="${canvasId}"] canvas`);
    if (!canvas) return;
    const link = document.createElement("a");
    link.download = `${fileName}.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
  };

  const svgMutation = useMutation({
    mutationFn: async () => {
      let svg = await QRCode.toString(publicUrl, {
        type: "svg",
        margin: 1,
        color: { dark: style.foreground, light: style.background },
        errorCorrectionLevel: style.errorCorrectionLevel,
      });
      if (style.logoDataUrl) {
        svg = embedLogoInSvg(svg, style.logoDataUrl, 200);
      }
      return svg;
    },
    onSuccess: (svg) => {
      const blob = new Blob([svg], { type: "image/svg+xml" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.download = `${fileName}.svg`;
      link.href = url;
      link.click();
      URL.revokeObjectURL(url);
    },
    onError: () => {
      toast.error("Failed to generate SVG");
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-col items-center gap-3" data-qr-canvas={canvasId}>
        <QrImage value={publicUrl} size={200} style={style} />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <Label htmlFor={`${canvasId}-fg`}>Foreground</Label>
          <Input
            id={`${canvasId}-fg`}
            type="color"
            className="h-10 p-1"
            value={style.foreground}
            onChange={(e) => setStyle((prev) => ({ ...prev, foreground: e.target.value }))}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`${canvasId}-bg`}>Background</Label>
          <Input
            id={`${canvasId}-bg`}
            type="color"
            className="h-10 p-1"
            value={style.background}
            onChange={(e) => setStyle((prev) => ({ ...prev, background: e.target.value }))}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label>Error correction</Label>
        <Select
          value={style.errorCorrectionLevel}
          onValueChange={(value) =>
            setStyle((prev) => ({ ...prev, errorCorrectionLevel: value as QrStyle["errorCorrectionLevel"] }))
          }
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {ERROR_CORRECTION_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor={`${canvasId}-logo`}>Center logo (optional)</Label>
        <div className="flex items-center gap-2">
          <Input
            id={`${canvasId}-logo`}
            type="file"
            accept="image/png,image/jpeg,image/svg+xml"
            onChange={(e) => setLogo(e.target.files?.[0] ?? null)}
          />
          {style.logoDataUrl && (
            <Button type="button" variant="ghost" size="icon" onClick={() => setLogo(null)}>
              <X className="size-4" />
            </Button>
          )}
        </div>
      </div>

      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={downloadPng}>
          <Download className="size-4" />
          PNG
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => svgMutation.mutate()}
          disabled={svgMutation.isPending}
        >
          {svgMutation.isPending ? <Loader2 className="size-4 animate-spin" /> : <Download className="size-4" />}
          SVG
        </Button>
      </div>
    </div>
  );
}
