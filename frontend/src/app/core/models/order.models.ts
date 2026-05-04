export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface Customer {
  id: number;
  name: string;
  email: string;
  city: string;
  orderCount: number;
  lifetimeValue: number;
}

export interface OrderSummary {
  id: number;
  orderDate: string;
  customerName: string;
  totalAmount: number;
  status: OrderStatus;
}

export interface OrderItem {
  productName: string;
  category: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderDetail {
  id: number;
  orderDate: string;
  totalAmount: number;
  status: OrderStatus;
  customer: Customer;
  itemCount: number;
  items: OrderItem[];
  subtotal: number;
  shippingAmount: number;
  paymentMethod: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface OrderCreateRequest {
  customerId: number;
  orderDate: string;
  status: OrderStatus;
  paymentMethod: string;
  shippingAmount: number;
  items: OrderItemRequest[];
}
