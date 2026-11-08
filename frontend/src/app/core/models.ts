// TypeScript versions of the backend DTOs (same field names as the JSON)

export type Role = 'ADMIN' | 'EMPLOYEE';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface Customer {
  id: number;
  name: string;
  ice: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
}
export type CustomerRequest = Omit<Customer, 'id'>;

export interface Product {
  id: number;
  reference: string;
  designation: string;
  description: string | null;
  unitPriceTTC: number;
  unitPriceHT: number;
  vatRate: number;
  active: boolean;
}
export interface ProductRequest {
  reference: string;
  designation: string;
  description: string | null;
  unitPriceTTC: number;
  vatRate: number | null;
  active: boolean;
}

/** Error body returned by the backend (Spring ProblemDetail) */
export interface ApiProblem {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
