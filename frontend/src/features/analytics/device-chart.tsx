"use client";

import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell } from "recharts";

import type { DeviceStat } from "@/lib/types/analytics";

// Fixed hue-by-identity, never cycled - same device always gets the same color.
const DEVICE_COLOR: Record<string, string> = {
  desktop: "var(--chart-1)",
  mobile: "var(--chart-2)",
  tablet: "var(--chart-3)",
  unknown: "var(--muted-foreground)",
};

function label(device: string) {
  return device.charAt(0).toUpperCase() + device.slice(1);
}

export function DeviceChart({ data }: { data: DeviceStat[] }) {
  const chartData = data.map((d) => ({ ...d, label: label(d.device) }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={chartData} layout="vertical" margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" horizontal={false} />
        <XAxis type="number" allowDecimals={false} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
        <YAxis
          type="category"
          dataKey="label"
          tick={{ fill: "var(--foreground)", fontSize: 13 }}
          axisLine={false}
          tickLine={false}
          width={70}
        />
        <Tooltip
          contentStyle={{
            background: "var(--popover)",
            border: "1px solid var(--border)",
            borderRadius: 8,
            color: "var(--popover-foreground)",
            fontSize: 13,
          }}
        />
        <Bar dataKey="count" name="Taps/Scans" radius={[0, 4, 4, 0]} maxBarSize={28}>
          {chartData.map((entry) => (
            <Cell key={entry.device} fill={DEVICE_COLOR[entry.device] ?? "var(--muted-foreground)"} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
