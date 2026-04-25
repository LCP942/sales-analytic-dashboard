/** Mirrors com.lp.salesdashboard.dto.KpiMetricsDto */
export interface KpiMetrics {
  revenue: number;
  orderCount: number;
  avgOrderValue: number;
  revenueGrowth: number;
  orderGrowth: number;
  avgOrderGrowth: number;
}

/** Mirrors RevenuePointDto — label is ISO date or YYYY-MM depending on granularity */
export interface DataPoint {
  label: string;
  revenue: number;
}

/** Mirrors TopProductDto */
export interface TopProduct {
  name: string;
  revenue: number;
}

/** Mirrors CategoryBreakdownDto */
export interface CategoryBreakdown {
  category: string;
  itemCount: number;
}

/** Mirrors WeekdayStatDto */
export interface WeekdayStat {
  day: string;
  orderCount: number;
  revenue: number;
}
