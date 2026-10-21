import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
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

        service.uploadDocument(file, { title: 'Rechnung', type: 'RECEIPT', tags: ['a', 'b'] }).subscribe();

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
