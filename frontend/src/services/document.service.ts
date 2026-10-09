import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../environment';
import { Document } from '../models/document.model';

type ApiDocument = {
    id: string;
    title: string;
    documentType?: string | null;
    content?: string | null;
    created?: string | null;
    tags?: string[] | null;
    summary?: string | null;
    fileUrl?: string | null;
};

type DocumentResponse = Document | ApiDocument;

export interface CreateDocumentRequest {
    title: string;
    type?: 'RECEIPT' | 'CONTRACT' | 'BANK_STATEMENT' | 'CERTIFICATE' | 'NOTE' | 'PASSPORT' | 'OTHER';
}

export interface UploadDocumentRequest extends CreateDocumentRequest {
    tags: string[];
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = `${environment.apiUrl.replace(/\/$/, '')}/document`;

    getDocuments(query = ''): Observable<Document[]> {
        const params = query ? new HttpParams().set('query', query) : undefined;
        return this.http.get<DocumentResponse[]>(this.apiUrl, { params })
            .pipe(map(documents => documents.map(document => this.toDocument(document))));
    }

    getDocumentById(id: string): Observable<Document> {
        return this.http.get<DocumentResponse>(`${this.apiUrl}/${encodeURIComponent(id)}`)
            .pipe(map(document => this.toDocument(document)));
    }

    createDocument(documentData: CreateDocumentRequest): Observable<Document> {
        return this.http.post<DocumentResponse>(this.apiUrl, documentData)
            .pipe(map(document => this.toDocument(document)));
    }

    uploadDocument(file: File, documentData: UploadDocumentRequest): Observable<Document> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        formData.append('title', documentData.title);
        if (documentData.type) formData.append('type', documentData.type);
        formData.append('tags', JSON.stringify(documentData.tags));

        return this.http.post<DocumentResponse>(this.apiUrl, formData)
            .pipe(map(document => this.toDocument(document)));
    }

    updateDocument(id: string, documentData: Partial<Document>): Observable<Document> {
        return this.http.put<DocumentResponse>(`${this.apiUrl}/${encodeURIComponent(id)}`, documentData)
            .pipe(map(document => this.toDocument(document)));
    }

    deleteDocument(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}`);
    }

    getDocumentData(id: string): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/${encodeURIComponent(id)}/data`, {
            responseType: 'blob'
        });
    }

    deleteDocumentData(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}/data`);
    }

    private toDocument(response: DocumentResponse): Document {
        if ('createdAt' in response) return { ...response };

        return {
            id: response.id,
            title: response.title,
            createdAt: response.created ?? '',
            category: response.documentType ?? undefined,
            tags: response.tags ?? undefined,
            summary: response.summary ?? undefined,
            ocrText: response.content ?? undefined,
            fileUrl: response.fileUrl ?? undefined
        };
    }
}