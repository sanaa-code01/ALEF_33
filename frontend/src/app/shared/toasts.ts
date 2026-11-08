import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="toasts">
      @for (t of toastService.toasts(); track t.id) {
        <div class="toast" [class.error]="t.kind === 'error'" (click)="toastService.dismiss(t.id)">
          {{ t.text }}
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly toastService = inject(ToastService);
}
