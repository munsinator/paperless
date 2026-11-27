import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../environment';
import { LoggerService } from './logger.service';

const SOURCE = 'DocumentService';
import { Document, DocumentType } from '../models/document.model';

type ApiDocument = {
    id: string;
    title: string;
    documentType?: DocumentType | null;
    createdAt?: string | null;
    tags?: string[] | null;
    fileUrl?: string | null;
};

// ApiDocument: Rohform aus dem Backend; Document: Aufbereitete Form für die Frontend-Nutzung
// falls sich bei DocumentType etwas ändert, kann man es leicht anpassen.
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
    private readonly logger = inject(LoggerService);
    private readonly apiUrl = `${environment.apiUrl.replace(/\/$/, '')}/document`;

    getDocuments(query = ''): Observable<Document[]> {
        const params = query ? new HttpParams().set('query', query) : undefined;
        this.logger.debug(SOURCE,`Loading documents (search: ${query !== ''})`);
        return this.http.get<ApiDocument[] | null>(this.apiUrl, { params })
            .pipe(map(documents => (documents ?? []).map(document => this.toDocument(document))));
    }

    getDocumentById(id: string): Observable<Document> {
        this.logger.debug(SOURCE,`Loading document ${id}`);
        return this.http.get<ApiDocument>(`${this.apiUrl}/${encodeURIComponent(id)}`)
            .pipe(map(document => this.toDocument(document)));
    }

    // 204 (Summary noch nicht erzeugt) kommt ohne Body zurück und wird zu null
    getSummary(id: string): Observable<string | null> {
        this.logger.debug(SOURCE,`Loading summary of document ${id}`);
        return this.http.get<{ summary: string } | null>(`${this.apiUrl}/${encodeURIComponent(id)}/summary`)
            .pipe(map(response => response?.summary ?? null));
    }

    // 204 (OCR noch nicht fertig) kommt ohne Body zurück und wird zu null
    getText(id: string): Observable<string | null> {
        this.logger.debug(SOURCE,`Loading OCR text of document ${id}`);
        return this.http.get<{ text: string } | null>(`${this.apiUrl}/${encodeURIComponent(id)}/text`)
            .pipe(map(response => response?.text ?? null));
    }

    // schickt die PDF zusammen mit den Metadaten als multipart/form-data ans Backend
    uploadDocument(file: File, documentData: UploadDocumentRequest): Observable<Document> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        formData.append('title', documentData.title);
        if (documentData.type) formData.append('type', documentData.type);
        documentData.tags.forEach(tag => formData.append('tags', tag));

        this.logger.info(SOURCE,`Uploading '${file.name}' (${file.size} bytes)`);
        return this.http.post<ApiDocument>(this.apiUrl, formData)
            .pipe(
                tap(document => this.logger.info(SOURCE,`Upload finished, document ${document.id} created`)),
                map(document => this.toDocument(document))
            );
    }

    updateDocument(id: string, documentData: Partial<Document>): Observable<Document> {
        const request: CreateDocumentRequest = { title: documentData.title ?? '' };
        if (documentData.title !== undefined) request.title = documentData.title;
        if (documentData.documentType !== undefined) request.type = documentData.documentType;
        if (documentData.tags !== undefined) request.tags = documentData.tags;

        this.logger.info(SOURCE,`Updating document ${id}`);
        return this.http.put<ApiDocument>(`${this.apiUrl}/${encodeURIComponent(id)}`, request)
            .pipe(map(document => this.toDocument(document)));
    }

    downloadDocument(id: string): Observable<Blob> {
        this.logger.info(SOURCE,`Downloading document ${id}`);
        return this.http.get(`${this.apiUrl}/${encodeURIComponent(id)}/data`, { responseType: 'blob' });
    }

    deleteDocument(id: string): Observable<void> {
        this.logger.info(SOURCE,`Deleting document ${id}`);
        return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}`);
    }

    private toDocument(response: ApiDocument): Document {
        return {
            id: response.id,
            title: response.title,
            documentType: response.documentType ?? undefined,
            createdAt: response.createdAt ?? '',
            tags: response.tags ?? [],
            fileUrl: response.fileUrl ?? undefined
        };
    }
}
