import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Customer, CustomerRequest } from './models';

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/customers`;

  list(search = '') {
    const params = search ? new HttpParams().set('search', search) : undefined;
    return this.http.get<Customer[]>(this.url, { params });
  }
  create(body: CustomerRequest) {
    return this.http.post<Customer>(this.url, body);
  }
  update(id: number, body: CustomerRequest) {
    return this.http.put<Customer>(`${this.url}/${id}`, body);
  }
  delete(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
