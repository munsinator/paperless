import { Component, inject } from '@angular/core';
import { NotificationService } from '../../services/notification.service';

@Component({
    selector: 'app-toast',
    standalone: true,
    template: `
    <div class="fixed bottom-4 right-4 left-4 sm:left-auto sm:w-96 z-[10000] flex flex-col gap-2 pointer-events-none">
        @for (item of notificationService.notifications(); track item.id) {
        <div [attr.role]="item.type === 'error' ? 'alert' : 'status'"
            [class]="item.type === 'error'
                ? 'bg-red-50 border-red-200 text-red-800'
                : 'bg-emerald-50 border-emerald-200 text-emerald-800'"
            class="pointer-events-auto flex items-start justify-between gap-3 border rounded-md px-3 py-2.5 text-xs font-medium shadow">
            <span>{{ item.message }}</span>
            <button type="button" (click)="notificationService.dismiss(item.id)" aria-label="Meldung schließen"
                class="text-base leading-none opacity-60 hover:opacity-100 cursor-pointer">&times;</button>
        </div>
        }
    </div>
    `
})
export class ToastComponent {
    readonly notificationService = inject(NotificationService);
}
