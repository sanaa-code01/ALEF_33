import { Pipe, PipeTransform } from '@angular/core';

/** "2026-11-07" → "07/11/2026" */
@Pipe({ name: 'frDate' })
export class FrDatePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '-';
    const [y, m, d] = value.substring(0, 10).split('-');
    return `${d}/${m}/${y}`;
  }
}

/** Today as "yyyy-MM-dd", for date inputs */
export function today(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
