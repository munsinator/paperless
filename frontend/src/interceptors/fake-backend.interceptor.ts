import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of } from 'rxjs';
import { Document } from '../models/document.model';

// Fake In-Memory Datenbank
let mockDocuments: Document[] = [
    {
        id: 'doc-001',
        title: 'Mietvertrag_Wohnung_2026.pdf',
        category: 'Mietvertrag',
        createdAt: '2026-10-01T10:00:00Z',
        tags: ['Wohnen', 'Vertrag'],
        summary: 'Mietvertrag für die Wohnung ab 01.10.2026.',
        ocrText: 'MIETVERTRAG zwischen...'
    },
    {
        id: 'doc-002',
        title: 'Stromrechnung_Q3_2026.pdf',
        category: 'Rechnung',
        createdAt: '2026-09-28T14:30:00Z',
        tags: ['Finanzen', 'Rechnung'],
        summary: 'Stromrechnung für Q3 2026 über 142,50 €.',
        ocrText: 'Wien Energie GmbH...'
    }
];

export const fakeBackendInterceptor: HttpInterceptorFn = (req, next) => {
    const { url, method, body } = req;

    // GET /api/documents
    if (url === '/api/documents' && method === 'GET') {
        const query = req.params.get('query')?.trim().toLocaleLowerCase();
        const documents = query
            ? mockDocuments.filter(document =>
                [document.title, document.summary, document.ocrText]
                    .some(value => value?.toLocaleLowerCase().includes(query))
            )
            : mockDocuments;
        return of(new HttpResponse({ status: 200, body: documents })).pipe(delay(500));
    }

    // GET /api/documents/:id
    if (url.match(/\/api\/documents\/.+$/) && method === 'GET') {
        const id = url.split('/').pop();
        const doc = mockDocuments.find(d => d.id === id);
        if (doc) {
            return of(new HttpResponse({ status: 200, body: doc })).pipe(delay(300));
        }
        return of(new HttpResponse({ status: 404, body: { message: 'Nicht gefunden' } }));
    }

    // PUT /api/documents/:id
    if (url.match(/\/api\/documents\/.+$/) && method === 'PUT') {
        const id = url.split('/').pop();
        const index = mockDocuments.findIndex(d => d.id === id);
        if (index !== -1) {
            mockDocuments[index] = { ...mockDocuments[index], ...(body as Partial<Document>) };
            return of(new HttpResponse({ status: 200, body: mockDocuments[index] })).pipe(delay(400));
        }
    }

    // DELETE /api/documents/:id
    if (url.match(/\/api\/documents\/.+$/) && method === 'DELETE') {
        const id = url.split('/').pop();
        mockDocuments = mockDocuments.filter(d => d.id !== id);
        return of(new HttpResponse({ status: 200 })).pipe(delay(400));
    }

    return next(req);
};