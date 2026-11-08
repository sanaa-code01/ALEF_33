import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  text: string;
  kind: 'success' | 'error';
}

/** Small messages in the corner ("Client enregistré", errors...) */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  success(text: string) {
    this.show(text, 'success');
  }
  error(text: string) {
    this.show(text, 'error', 6000);
  }

  dismiss(id: number) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private show(text: string, kind: Toast['kind'], duration = 3500) {
    const toast = { id: this.nextId++, text, kind };
    this.toasts.update(list => [...list, toast]);
    setTimeout(() => this.dismiss(toast.id), duration);
  }
}
