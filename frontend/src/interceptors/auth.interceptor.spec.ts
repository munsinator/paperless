import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

describe('authInterceptor', () => {
    let http: HttpClient;
    let httpMock: HttpTestingController;
    let notifications: NotificationService;
    let logout: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        logout = vi.fn();
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                provideHttpClient(withInterceptors([authInterceptor])),
                provideHttpClientTesting(),
                { provide: AuthService, useValue: { token: () => 'jwt', logout } }
            ]
        });
        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        notifications = TestBed.inject(NotificationService);
    });

    it('shows a message on 403 but does not log out', () => {
        http.get('/api/document/1').subscribe({ error: () => undefined });
        httpMock.expectOne('/api/document/1').flush(null, { status: 403, statusText: 'Forbidden' });

        expect(logout).not.toHaveBeenCalled();
        expect(notifications.notifications().length).toBe(1);
        expect(notifications.notifications()[0].type).toBe('error');
    });

    it('logs out on 401', () => {
        http.get('/api/document').subscribe({ error: () => undefined });
        httpMock.expectOne('/api/document').flush(null, { status: 401, statusText: 'Unauthorized' });

        expect(logout).toHaveBeenCalledTimes(1);
    });

    it('ignores errors of the auth endpoints', () => {
        http.post('/api/sessions', {}).subscribe({ error: () => undefined });
        httpMock.expectOne('/api/sessions').flush(null, { status: 403, statusText: 'Forbidden' });

        expect(logout).not.toHaveBeenCalled();
        expect(notifications.notifications().length).toBe(0);
    });
});
