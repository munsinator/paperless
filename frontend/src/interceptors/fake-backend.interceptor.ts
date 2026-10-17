import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';
import { Document } from '../models/document.model';
import { environment } from '../environment';

// Fake In-Memory Datenbank
let mockDocuments: Document[] = [
    {
        id: 'doc-001',
        title: 'Mietvertrag_Wohnung_2026.pdf',
        documentType: 'CONTRACT',
        createdAt: '2026-10-01T10:00:00Z',
        tags: ['Wohnen', 'Vertrag'],
        summary: 'Mietvertrag für die Wohnung ab 01.10.2026.',
        ocrText: 'MIETVERTRAG zwischen...'
    },
    {
        id: 'doc-002',
        title: 'Stromrechnung_Q3_2026.pdf',
        documentType: 'RECEIPT',
        createdAt: '2026-09-28T14:30:00Z',
        tags: ['Finanzen', 'Rechnung'],
        summary: 'Stromrechnung für Q3 2026 über 142,50 €.',
        ocrText: 'Wien Energie GmbH...'
    }
];

export const fakeBackendInterceptor: HttpInterceptorFn = (req, next) => {
    if (!environment.useFakeBackend) return next(req);

    const { url, method, body } = req;
    const documentsUrl = `${environment.apiUrl.replace(/\/$/, '')}/document`;
    if (url === documentsUrl
        && method === 'POST'
        && typeof FormData !== 'undefined'
        && body instanceof FormData) {
        return next(req);
    }

    const resourcePath = url.startsWith(`${documentsUrl}/`)
        ? url.slice(documentsUrl.length + 1)
        : '';
    const isDocumentId = resourcePath.length > 0 && !resourcePath.includes('/');

    // GET /api/document
    if (url === documentsUrl && method === 'GET') {
        const query = req.params.get('query')?.trim().toLocaleLowerCase();
        const documents = query
            ? mockDocuments.filter(document =>
                [document.title, document.summary, document.ocrText]
                    .some(value => value?.toLocaleLowerCase().includes(query))
            )
            : mockDocuments;
        return of(new HttpResponse({ status: 200, body: documents })).pipe(delay(500));
    }

    // POST /api/document
    if (url === documentsUrl && method === 'POST') {
        const documentBody = body as { title: string; type?: Document['documentType']; tags?: string[] };
        const document: Document = {
            id: `doc-${Date.now()}`,
            title: documentBody.title,
            documentType: documentBody.type,
            createdAt: new Date().toISOString(),
            tags: documentBody.tags ?? []
        };
        mockDocuments = [document, ...mockDocuments];
        return of(new HttpResponse({ status: 201, body: document })).pipe(delay(400));
    }

    // GET /api/document/:id
    if (isDocumentId && method === 'GET') {
        const id = decodeURIComponent(resourcePath);
        const doc = mockDocuments.find(d => d.id === id);
        if (doc) {
            return of(new HttpResponse({ status: 200, body: doc })).pipe(delay(300));
        }
        return of(new HttpResponse({ status: 404, body: { message: 'Nicht gefunden' } }));
    }

    // PUT /api/document/:id
    if (isDocumentId && method === 'PUT') {
        const id = decodeURIComponent(resourcePath);
        const index = mockDocuments.findIndex(d => d.id === id);
        if (index !== -1) {
            mockDocuments[index] = { ...mockDocuments[index], ...(body as Partial<Document>) };
            return of(new HttpResponse({ status: 200, body: mockDocuments[index] })).pipe(delay(400));
        }
        return of(new HttpResponse({ status: 404, body: { message: 'Nicht gefunden' } }));
    }

    // DELETE /api/document/:id
    if (isDocumentId && method === 'DELETE') {
        const id = decodeURIComponent(resourcePath);
        if (!mockDocuments.some(document => document.id === id)) {
            return of(new HttpResponse({ status: 404, body: { message: 'Nicht gefunden' } }));
        }
        mockDocuments = mockDocuments.filter(d => d.id !== id);
        return of(new HttpResponse({ status: 200 })).pipe(delay(400));
    }

    return next(req);
};