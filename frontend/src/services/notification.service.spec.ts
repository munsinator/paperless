import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
    afterEach(() => vi.useRealTimers());

    it('adds and dismisses notifications', () => {
        const service = new NotificationService();
        service.error('Fehler');
        const [item] = service.notifications();

        expect(item.message).toBe('Fehler');
        service.dismiss(item.id);
        expect(service.notifications()).toEqual([]);
    });

    it('does not show the same message twice', () => {
        const service = new NotificationService();
        service.error('Fehler');
        service.error('Fehler');

        expect(service.notifications().length).toBe(1);
    });

    it('removes notifications automatically', () => {
        vi.useFakeTimers();
        const service = new NotificationService();
        service.success('OK');

        vi.advanceTimersByTime(6000);
        expect(service.notifications()).toEqual([]);
    });

    it('maps http errors but skips 403 (reported by the interceptor)', () => {
        const service = new NotificationService();
        service.httpError(new HttpErrorResponse({ status: 403 }), 'save');
        expect(service.notifications()).toEqual([]);

        service.httpError(new HttpErrorResponse({ status: 500 }), 'delete');
        expect(service.notifications()[0].message).toContain('Serverfehler');
    });
});
