import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Product } from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getAll() {
    return this.http.get<Product[]>(`${environment.apiBaseUrl}/products`);
  }
}
