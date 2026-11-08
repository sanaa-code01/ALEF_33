import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { User, UserRequest } from './models';

/** Account management (the backend only allows ADMIN) */
@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/users`;

  list() {
    return this.http.get<User[]>(this.url);
  }
  create(body: UserRequest) {
    return this.http.post<User>(this.url, body);
  }
  setEnabled(id: number, enabled: boolean) {
    return this.http.patch<User>(`${this.url}/${id}/enabled`, null, { params: new HttpParams().set('enabled', enabled) });
  }
}
