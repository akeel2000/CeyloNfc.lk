export interface DailyPoint {
  date: string;
  nfcTaps: number;
  qrScans: number;
}

export interface DeviceStat {
  device: string;
  count: number;
}

export interface TopItem {
  label: string;
  count: number;
}

export interface AnalyticsSummary {
  rangeDays: number;
  totalNfcTaps: number;
  totalQrScans: number;
  totalProfileViews: number;
  dailySeries: DailyPoint[];
  deviceBreakdown: DeviceStat[];
  topCards: TopItem[];
}
