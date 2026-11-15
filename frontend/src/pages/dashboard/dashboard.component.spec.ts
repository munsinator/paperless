import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { Document } from '../../models/document.model';
import { DocumentService } from '../../services/document.service';
import { DashboardComponent } from './dashboard.component';

function document(id: string, title: string): Document {
    return { id, title, createdAt: '2026-11-15' };
}

describe('DashboardComponent', () => {
    let getDocuments: ReturnType<typeof vi.fn<(query?: string) => Observable<Document[]>>>;
    let harness: RouterTestingHarness; // für z.B. await harness.navigateByUrl('/dashboard?q=hello', DashboardComponent);

    // waitForStable=false für Anfragen, die absichtlich offen bleiben (die App wäre sonst nie "stabil")
    async function openDashboard(url: string, { waitForStable = true } = {}): Promise<HTMLElement> {
        TestBed.configureTestingModule({
            providers: [
                provideRouter([{ path: 'dashboard', component: DashboardComponent }], withComponentInputBinding()),
                { provide: DocumentService, useValue: { getDocuments } }
            ]
        });
        harness = await RouterTestingHarness.create();
        await harness.navigateByUrl(url, DashboardComponent);
        if (waitForStable) await harness.fixture.whenStable();
        harness.detectChanges();
        return harness.routeNativeElement as HTMLElement;
    }

    beforeEach(() => {
        getDocuments = vi.fn<(query?: string) => Observable<Document[]>>().mockReturnValue(of([]));
    });

    it('loads all documents when the url has no search term', async () => {
        getDocuments.mockReturnValue(of([document('1', 'Alpha')]));

        const page = await openDashboard('/dashboard');

        expect(getDocuments).toHaveBeenCalledWith('');
        expect(page.textContent).toContain('Alpha');
    });

    it('searches with the term from the url and keeps the order of the results', async () => {
        getDocuments.mockReturnValue(of([document('2', 'Zeta'), document('1', 'Alpha')]));

        const page = await openDashboard('/dashboard?q=hello');

        expect(getDocuments).toHaveBeenCalledWith('hello');
        const titles = Array.from(page.querySelectorAll('app-document-card a')).map(link => link.textContent?.trim());
        expect(titles.indexOf('Zeta')).toBeLessThan(titles.indexOf('Alpha'));
    });

    it('uses the last value when the url repeats the search parameter', async () => {
        const page = await openDashboard('/dashboard?q=a&q=zebra');

        expect(getDocuments).toHaveBeenCalledWith('zebra');
        expect((page.querySelector('input[name="query"]') as HTMLInputElement).value).toBe('zebra');
    });

    it('prefills the search field from the url', async () => {
        const page = await openDashboard('/dashboard?q=hello');

        expect((page.querySelector('input[name="query"]') as HTMLInputElement).value).toBe('hello');
    });

    it('shows a spinner while the search is loading', async () => {
        getDocuments.mockReturnValue(NEVER); // Anfrage bleibt offen, das Backend hat noch nicht geantwortet

        const page = await openDashboard('/dashboard?q=hello', { waitForStable: false });

        expect(page.querySelector('.animate-spin')).toBeTruthy();
        expect(page.querySelector('app-document-card')).toBeNull();
        expect(page.textContent).not.toContain('Keine Treffer');
    });

    it('shows a no-results message for a search without hits', async () => {
        const page = await openDashboard('/dashboard?q=hello');

        expect(page.textContent).toContain('Keine Treffer für „hello“.');
    });

    it('shows that the search is unavailable when the backend answers with 503', async () => {
        getDocuments.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));

        const page = await openDashboard('/dashboard?q=hello');

        expect(page.textContent).toContain('Die Suche ist derzeit nicht verfügbar.');
    });

    it('writes a new search term into the url', async () => {
        await openDashboard('/dashboard');

        harness.routeDebugElement!.componentInstance.onSearch('hello');
        await harness.fixture.whenStable();

        expect(TestBed.inject(Router).url).toBe('/dashboard?q=hello');
        expect(getDocuments).toHaveBeenLastCalledWith('hello');
    });

    it('removes the search term from the url when the search is cleared', async () => {
        await openDashboard('/dashboard?q=hello');

        harness.routeDebugElement!.componentInstance.onSearch('');
        await harness.fixture.whenStable();

        expect(TestBed.inject(Router).url).toBe('/dashboard');
        expect(getDocuments).toHaveBeenLastCalledWith('');
    });
});
