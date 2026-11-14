import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { DocumentService } from '../../services/document.service';
import { NotificationService } from '../../services/notification.service';
import { DocumentDetailComponent } from './document-detail.component';

describe('DocumentDetailComponent download', () => {
    const documentService = {
        getDocumentById: vi.fn(),
        downloadDocument: vi.fn()
    };
    const notifications = { httpError: vi.fn() };
    const savedFilenames: string[] = [];

    const pdf = new Blob(['%PDF-1.7'], { type: 'application/pdf' });

    // preview$: Antwort, die die Seite beim Öffnen für die Vorschau bekommt (Standard: die PDF ist da)
    async function createComponent(preview$: Observable<Blob> = of(pdf)) {
        TestBed.configureTestingModule({ // Testumgebung: providers bestimmen, welche Objekte Angular der Komponente zur Verfügung stellt
            providers: [
                provideRouter([]), // Angular Router
                { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'abc-123' })) } }, // simuliert /documents/abc-123
                { provide: DocumentService, useValue: documentService }, // "verwende stattdessen unser selbst erstelltes Mock-Objekt"
                { provide: NotificationService, useValue: notifications } // "verwende stattdessen unser selbst erstelltes Mock-Objekt"
            ]
        });
        documentService.getDocumentById.mockReturnValue(of({ id: 'abc-123', title: 'Rechnung', createdAt: '', tags: [] }));
        // die Seite lädt die PDF schon beim Öffnen für die Vorschau
        documentService.downloadDocument.mockReturnValue(preview$);
        const fixture = TestBed.createComponent(DocumentDetailComponent); // Instanz deiner DocumentDetailComponent
        fixture.detectChanges(); // Lifecycle-Methoden wie ngOnInit()
        await fixture.whenStable(); // warten, bis die asynchronen Vorgänge der Komponente abgeschlossen sind: z.B. bis das Observable mit den simulierten Dokumentinformationen verarbeitet wurde
        return fixture;
    }

    beforeEach(() => {
        vi.clearAllMocks();
        savedFilenames.length = 0;
        // aus dem Blob eine URL erstellen; 'about:blank', weil jsdom das <iframe> der Vorschau wirklich laden würde
        // (bei einer erfundenen 'blob:...'-URL kommt ein SecurityError wegen localStorage)
        URL.createObjectURL = vi.fn(() => 'about:blank');
        // gibt eine zuvor erstellte temporäre Blob-URL wieder frei (Die Blob-URL hält eine Verbindung zu den PDF-Daten im Speicher des Browsers)
        URL.revokeObjectURL = vi.fn();
        // Klick auf Download-Link simulieren
        vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
            savedFilenames.push(this.download);
        });
    });

    it('downloads the pdf and saves it as "<title>.pdf"', async () => {
        const component = (await createComponent()).componentInstance;
        documentService.downloadDocument.mockReturnValue(of(pdf));

        component.onDownload();

        expect(documentService.downloadDocument).toHaveBeenCalledWith('abc-123');
        expect(savedFilenames).toEqual(['Rechnung.pdf']);
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('about:blank');
        expect(component.isDownloading()).toBe(false);
    });

    it('shows an error notification when the download fails', async () => {
        const component = (await createComponent()).componentInstance;
        const error = new HttpErrorResponse({ status: 503 });
        documentService.downloadDocument.mockReturnValue(throwError(() => error));

        component.onDownload();

        expect(notifications.httpError).toHaveBeenCalledWith(error, 'download');
        expect(savedFilenames).toEqual([]);
        expect(component.isDownloading()).toBe(false);
    });

    it('creates a preview url from the loaded pdf', async () => {
        const component = (await createComponent()).componentInstance;

        expect(URL.createObjectURL).toHaveBeenCalledWith(pdf);
        expect(component.safeFileUrl()).not.toBeNull();
    });

    it('has no preview when the pdf cannot be loaded', async () => {
        const component = (await createComponent(throwError(() => new HttpErrorResponse({ status: 404 })))).componentInstance;

        expect(component.safeFileUrl()).toBeNull();
        expect(URL.createObjectURL).not.toHaveBeenCalled();
    });

    it('releases the preview url when the page is left', async () => {
        const fixture = await createComponent();

        fixture.destroy();

        expect(URL.revokeObjectURL).toHaveBeenCalledWith('about:blank');
    });
});
