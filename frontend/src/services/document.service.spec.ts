import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DocumentType } from '../models/document.model';
import { DocumentService } from './document.service';

describe('DocumentService', () => {
    let service: DocumentService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()]
        });
        service = TestBed.inject(DocumentService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('uploads the pdf and metadata as multipart form data', () => {
        const file = new File(['%PDF-1.7'], 'rechnung.pdf', { type: 'application/pdf' });

        service.uploadDocument(file, { title: 'Rechnung', type: DocumentType.RECEIPT, tags: ['a', 'b'] }).subscribe();

        const req = httpMock.expectOne('/api/document');
        expect(req.request.method).toBe('POST');
        const body = req.request.body as FormData;
        expect(body instanceof FormData).toBe(true);
        expect((body.get('file') as File).name).toBe('rechnung.pdf');
        expect(body.get('title')).toBe('Rechnung');
        expect(body.get('type')).toBe('RECEIPT');
        expect(body.getAll('tags')).toEqual(['a', 'b']);
        req.flush({ id: '1', title: 'Rechnung' });
    });

    it('maps a 204 response to an empty document list', () => {
        let result: unknown;
        service.getDocuments().subscribe(documents => result = documents);
        httpMock.expectOne('/api/document').flush(null, { status: 204, statusText: 'No Content' });
        expect(result).toEqual([]);
        httpMock.verify();
    });

    it('downloads the stored pdf as a blob', () => {
        let result: Blob | undefined;

        service.downloadDocument('abc-123').subscribe(blob => (result = blob));

        const req = httpMock.expectOne('/api/document/abc-123/data');
        expect(req.request.method).toBe('GET');
        expect(req.request.responseType).toBe('blob');
        const pdf = new Blob(['%PDF-1.7'], { type: 'application/pdf' });
        req.flush(pdf); // simulieren die Antwort vom Server selbst
        expect(result).toBe(pdf);
    });

    it('sends the search term as query parameter', () => {
        service.getDocuments('hello').subscribe();

        const req = httpMock.expectOne(request => request.url === '/api/document');
        expect(req.request.method).toBe('GET');
        expect(req.request.params.get('query')).toBe('hello');
        req.flush([]);
    });

    it('sends no query parameter without a search term', () => {
        service.getDocuments().subscribe();

        const req = httpMock.expectOne('/api/document');
        expect(req.request.params.has('query')).toBe(false);
        req.flush([]);
    });

    it('keeps the order of the search results from the backend', () => {
        let titles: string[] = [];

        service.getDocuments('hello').subscribe(documents => (titles = documents.map(document => document.title)));

        // das Backend liefert die Treffer nach Relevanz, das Frontend darf nicht umsortieren
        httpMock.expectOne(request => request.url === '/api/document').flush([
            { id: '2', title: 'Zeta' },
            { id: '1', title: 'Alpha' }
        ]);
        expect(titles).toEqual(['Zeta', 'Alpha']);
    });

    it('omits the type when none is selected', () => {
        const file = new File(['%PDF-1.7'], 'x.pdf', { type: 'application/pdf' });

        service.uploadDocument(file, { title: 'X', tags: [] }).subscribe();

        const req = httpMock.expectOne('/api/document');
        const body = req.request.body as FormData;
        expect(body.has('type')).toBe(false);
        expect(body.has('tags')).toBe(false);
        req.flush({ id: '1', title: 'X' });
    });
});
