import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
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

    async function createComponent() {
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
        documentService.downloadDocument.mockReturnValue(of(new Blob(['%PDF-1.7'], { type: 'application/pdf' })));
        const fixture = TestBed.createComponent(DocumentDetailComponent); // Instanz deiner DocumentDetailComponent
        fixture.detectChanges(); // Lifecycle-Methoden wie ngOnInit()
        await fixture.whenStable(); // warten, bis die asynchronen Vorgänge der Komponente abgeschlossen sind: z.B. bis das Observable mit den simulierten Dokumentinformationen verarbeitet wurde
        return fixture.componentInstance; // eigentliche Komponenteninstanz
    }

    beforeEach(() => {
        vi.clearAllMocks();
        savedFilenames.length = 0;
        // aus dem Blob eine URL erstellen
        URL.createObjectURL = vi.fn(() => 'blob:test');
        // gibt eine zuvor erstellte temporäre Blob-URL wieder frei (Die Blob-URL hält eine Verbindung zu den PDF-Daten im Speicher des Browsers)
        URL.revokeObjectURL = vi.fn();
        // Klick auf Download-Link simulieren
        vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
            savedFilenames.push(this.download);
        });
    });

    it('downloads the pdf and saves it as "<title>.pdf"', async () => {
        const component = await createComponent();
        documentService.downloadDocument.mockReturnValue(of(new Blob(['%PDF-1.7'], { type: 'application/pdf' })));

        component.onDownload();

        expect(documentService.downloadDocument).toHaveBeenCalledWith('abc-123');
        expect(savedFilenames).toEqual(['Rechnung.pdf']);
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
        expect(component.isDownloading()).toBe(false);
    });

    it('shows an error notification when the download fails', async () => {
        const component = await createComponent();
        const error = new HttpErrorResponse({ status: 503 });
        documentService.downloadDocument.mockReturnValue(throwError(() => error));

        component.onDownload();

        expect(notifications.httpError).toHaveBeenCalledWith(error, 'download');
        expect(savedFilenames).toEqual([]);
        expect(component.isDownloading()).toBe(false);
    });
});
