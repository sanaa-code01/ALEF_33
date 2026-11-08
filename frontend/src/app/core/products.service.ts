import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Product, ProductRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/products`;

  list(activeOnly = false) {
    return this.http.get<Product[]>(this.url, { params: new HttpParams().set('activeOnly', activeOnly) });
  }
  create(body: ProductRequest) {
    return this.http.post<Product>(this.url, body);
  }
  update(id: number, body: ProductRequest) {
    return this.http.put<Product>(`${this.url}/${id}`, body);
  }
  deactivate(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
