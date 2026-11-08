import { Pipe, PipeTransform } from '@angular/core';

const FORMAT = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** 95800 → "95 800,00" (same format as the PDFs) */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return value == null ? '' : FORMAT.format(value);
  }
}
