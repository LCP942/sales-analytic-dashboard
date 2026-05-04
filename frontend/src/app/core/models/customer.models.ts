export interface CustomerSummary {
  id: number;
  name: string;
  email: string;
  city: string;
  orderCount: number;
  lifetimeValue: number;
}

export interface CustomerCreateRequest {
  name: string;
  email: string;
  city: string;
}
