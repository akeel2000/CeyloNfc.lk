"use client";

import { useEffect, useRef } from "react";
import QRCode from "qrcode";

export interface QrStyle {
  foreground: string;
  background: string;
  errorCorrectionLevel: "L" | "M" | "Q" | "H";
  /** Data URL of a logo to overlay at the center. Only meaningful at errorCorrectionLevel "H" -
   *  that's the only level with enough redundancy (~30%) to stay scannable once a chunk of the
   *  center is covered, so callers should steer the level select there when a logo is set. */
  logoDataUrl?: string | null;
}

export const DEFAULT_QR_STYLE: QrStyle = {
  foreground: "#0a0a0a",
  background: "#ffffff",
  errorCorrectionLevel: "M",
};

export function QrImage({ value, size = 160, style = DEFAULT_QR_STYLE }: { value: string; size?: number; style?: QrStyle }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let cancelled = false;

    QRCode.toCanvas(canvas, value, {
      width: size,
      margin: 1,
      color: { dark: style.foreground, light: style.background },
      errorCorrectionLevel: style.errorCorrectionLevel,
    })
      .then(() => {
        if (cancelled || !style.logoDataUrl) return;
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        const logo = new Image();
        logo.onload = () => {
          if (cancelled) return;
          const logoSize = size * 0.22;
          const pad = logoSize * 0.16;
          const x = (size - logoSize) / 2;
          const y = (size - logoSize) / 2;
          ctx.fillStyle = style.background;
          ctx.fillRect(x - pad, y - pad, logoSize + pad * 2, logoSize + pad * 2);
          ctx.drawImage(logo, x, y, logoSize, logoSize);
        };
        logo.src = style.logoDataUrl;
      })
      .catch(() => undefined);

    return () => {
      cancelled = true;
    };
  }, [value, size, style.foreground, style.background, style.errorCorrectionLevel, style.logoDataUrl]);

  return <canvas ref={canvasRef} width={size} height={size} className="rounded-md border border-border" />;
}
