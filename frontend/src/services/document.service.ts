import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../environment';
import { Document, DocumentType } from '../models/document.model';

type ApiDocument = {
    id: string;
    title: string;
    documentType?: DocumentType | null;
    createdAt?: string | null;
    ocrText?: string | null;
    tags?: string[] | null;
    summary?: string | null;
    fileUrl?: string | null;
};

// ApiDocument: Rohform aus demm Backend; Document: Aufbereitete Form für die Frontend-Nutzung
// falls sich bei DocumentType etwas ändert, kann man es leicht anpassen.
type DocumentResponse = ApiDocument;

export interface CreateDocumentRequest {
    title: string;
    type?: DocumentType;
    tags?: string[];
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

    // nur Metadaten werden erstellt, das Hochladen von Dateien erfolgt separat
    createDocument(documentData: CreateDocumentRequest): Observable<Document> {
        return this.http.post<DocumentResponse>(this.apiUrl, documentData)
            .pipe(map(document => this.toDocument(document)));
    }

    // schickt die PDF zusammen mit den Metadaten als multipart/form-data ans Backend
    uploadDocument(file: File, documentData: UploadDocumentRequest): Observable<Document> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        formData.append('title', documentData.title);
        if (documentData.type) formData.append('type', documentData.type);
        documentData.tags.forEach(tag => formData.append('tags', tag));

        return this.http.post<DocumentResponse>(this.apiUrl, formData)
            .pipe(map(document => this.toDocument(document)));
    }

    updateDocument(id: string, documentData: Partial<Document>): Observable<Document> {
        const request: CreateDocumentRequest = { title: documentData.title ?? '' };
        if (documentData.title !== undefined) request.title = documentData.title;
        if (documentData.documentType !== undefined) request.type = documentData.documentType;
        if (documentData.tags !== undefined) request.tags = documentData.tags;

        return this.http.put<DocumentResponse>(`${this.apiUrl}/${encodeURIComponent(id)}`, request)
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
        return {
            id: response.id,
            title: response.title,
            documentType: response.documentType ?? undefined,
            createdAt: response.createdAt ?? '',
            tags: response.tags ?? [],
            summary: response.summary ?? undefined,
            ocrText: response.ocrText ?? undefined,
            fileUrl: response.fileUrl ?? undefined
        };
    }
}