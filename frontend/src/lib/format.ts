const CURRENCY_FORMATTER = new Intl.NumberFormat("en-LK", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

/** Formats a numeric amount using the platform's single currency convention (LKR, "Rs" prefix). */
export function formatCurrency(amount: number | string | null | undefined) {
  if (amount === null || amount === undefined || amount === "") return "Rs —";
  const value = typeof amount === "string" ? Number(amount) : amount;
  if (Number.isNaN(value)) return "Rs —";
  return `Rs ${CURRENCY_FORMATTER.format(value)}`;
}

export function formatDate(date: string | number | Date | null | undefined) {
  if (!date) return "—";
  const parsed = date instanceof Date ? date : new Date(date);
  if (Number.isNaN(parsed.getTime())) return "—";
  return parsed.toLocaleDateString("en-LK", { year: "numeric", month: "short", day: "numeric" });
}

export function formatDateTime(date: string | number | Date | null | undefined) {
  if (!date) return "—";
  const parsed = date instanceof Date ? date : new Date(date);
  if (Number.isNaN(parsed.getTime())) return "—";
  return parsed.toLocaleString("en-LK", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
