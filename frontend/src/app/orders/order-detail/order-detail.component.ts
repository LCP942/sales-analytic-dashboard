import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { OrderDetail } from '../../core/models/order.models';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { OrderTimelineComponent } from '../../shared/components/order-timeline/order-timeline.component';
import { CustomerCardComponent } from '../../shared/components/customer-card/customer-card.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    StatusBadgeComponent,
    OrderTimelineComponent,
    CustomerCardComponent,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent {
  order   = input.required<OrderDetail>();
  columns = ['product', 'category', 'qty', 'unitPrice', 'lineTotal'];

  private readonly router = inject(Router);

  back(): void {
    this.router.navigate(['/orders']);
  }

  downloadInvoice(): void {
    const o    = this.order();
    const blob = new Blob([this.buildInvoiceHtml(o)], { type: 'text/html' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `invoice-${o.id}.html`;
    a.click();
    URL.revokeObjectURL(url);
  }

  printInvoice(): void {
    const blob = new Blob([this.buildInvoiceHtml(this.order(), true)], { type: 'text/html' });
    window.open(URL.createObjectURL(blob), '_blank');
  }

  private buildInvoiceHtml(o: OrderDetail, autoPrint = false): string {
    const eur = (v: number) =>
      new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(v);

    const rows = o.items.map(i => `
      <tr>
        <td>${i.productName}</td>
        <td><span class="badge">${i.category}</span></td>
        <td class="r">× ${i.quantity}</td>
        <td class="r">${eur(i.unitPrice)}</td>
        <td class="r amount">${eur(i.lineTotal)}</td>
      </tr>`).join('');

    return `<!DOCTYPE html><html lang="fr"><head>
<meta charset="utf-8"><title>Invoice #${o.id}</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:system-ui,-apple-system,sans-serif;font-size:13px;color:#0f172a;background:#f8fafc;padding:32px}
.paper{background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:32px 36px;max-width:860px;margin:0 auto;display:flex;flex-direction:column;gap:24px}
.meta{display:flex;gap:40px}
.meta-label{font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.06em;color:#64748b;margin-bottom:4px}
.meta-name{font-weight:600;margin-bottom:2px}
.meta-info{color:#64748b;font-size:12px;line-height:1.5}
table{width:100%;border-collapse:collapse}
th{font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.05em;color:#64748b;padding:6px 8px;border-bottom:2px solid #e2e8f0;text-align:left}
td{padding:8px;border-bottom:1px solid #e2e8f0;font-size:13px;vertical-align:middle}
tr:last-child td{border-bottom:none}
tr:nth-child(even) td{background:#f8fafc}
.r{text-align:right}
.amount{font-weight:600}
.badge{display:inline-block;padding:2px 8px;border-radius:999px;background:#e2e8f0;color:#475569;font-size:11px;font-weight:500}
.totals{display:flex;flex-direction:column;gap:6px;align-items:flex-end;padding-top:8px;border-top:1px solid #e2e8f0}
.total-row{display:flex;gap:48px}
.total-row span:first-child{color:#64748b;min-width:80px;text-align:right}
.total-row span:last-child{min-width:100px;text-align:right}
.total-final{font-weight:700;font-size:15px;padding-top:6px;border-top:1px solid #e2e8f0}
.total-final span:last-child{color:#3b82f6}
.footer{text-align:center;font-size:12px;font-style:italic;color:#94a3b8;padding-top:8px;border-top:1px solid #e2e8f0}
@media print{body{padding:0;background:#fff}.paper{border:none;border-radius:0}}
</style></head>
<body><div class="paper">
  <div class="meta">
    <div>
      <div class="meta-label">BILL TO</div>
      <div class="meta-name">${o.customer.name}</div>
      <div class="meta-info">${o.customer.email}</div>
      <div class="meta-info">${o.customer.city}</div>
    </div>
    <div>
      <div class="meta-label">PAYMENT</div>
      <div class="meta-name">${o.paymentMethod}</div>
    </div>
  </div>
  <table>
    <thead><tr><th>Product</th><th>Category</th><th class="r">Qty</th><th class="r">Unit price</th><th class="r">Total</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <div class="totals">
    <div class="total-row"><span>Subtotal</span><span>${eur(o.subtotal)}</span></div>
    <div class="total-row"><span>Shipping</span><span>${o.shippingAmount === 0 ? 'Free' : eur(o.shippingAmount)}</span></div>
    <div class="total-row total-final"><span>Total</span><span>${eur(o.totalAmount)}</span></div>
  </div>
  <div class="footer">Thank you for your order.</div>
</div>${autoPrint ? '<script>window.onload=()=>window.print();<\/script>' : ''}</body></html>`;
  }
}
