import { Component, input } from '@angular/core';
import { DocumentLine } from '../core/models';
import { MoneyPipe } from './money.pipe';

/** Read-only table of lines, shared by quotes and orders */
@Component({
  selector: 'app-lines-table',
  imports: [MoneyPipe],
  template: `
    <table class="table">
      <thead>
        <tr><th>Désignation</th><th class="num">Qté</th><th class="num">P.U TTC</th><th class="num">TVA</th><th class="num">Total TTC</th></tr>
      </thead>
      <tbody>
        @for (l of lines(); track l.id) {
          <tr>
            <td>{{ l.designation }}</td>
            <td class="num">{{ l.quantity }}</td>
            <td class="num">{{ l.unitPriceTTC | money }}</td>
            <td class="num">{{ l.vatRate }} %</td>
            <td class="num strong">{{ l.lineTotalTTC | money }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class LinesTable {
  readonly lines = input.required<DocumentLine[]>();
}
