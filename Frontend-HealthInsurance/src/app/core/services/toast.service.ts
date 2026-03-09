import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
    id: number;
    message: string;
    type: 'success' | 'error' | 'warning' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
    private counter = 0;
    private toastsSubject = new BehaviorSubject<Toast[]>([]);
    toasts$ = this.toastsSubject.asObservable();

    show(message: string, type: Toast['type'] = 'info', duration = 4000): void {
        const toast: Toast = { id: ++this.counter, message, type };
        const current = this.toastsSubject.value;
        this.toastsSubject.next([...current, toast]);

        setTimeout(() => this.dismiss(toast.id), duration);
    }

    success(message: string): void { this.show(message, 'success'); }
    error(message: string): void { this.show(message, 'error', 5000); }
    warning(message: string): void { this.show(message, 'warning'); }

    dismiss(id: number): void {
        const filtered = this.toastsSubject.value.filter(t => t.id !== id);
        this.toastsSubject.next(filtered);
    }
}
