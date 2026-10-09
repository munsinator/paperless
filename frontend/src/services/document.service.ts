import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Document } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/documents';

    getDocuments(query = ''): Observable<Document[]> {
        const params = query ? new HttpParams().set('query', query) : undefined;
        return this.http.get<Document[]>(this.apiUrl, { params });
    }

    getDocumentById(id: string): Observable<Document> {
        return this.http.get<Document>(`${this.apiUrl}/${id}`);
    }

    updateDocument(id: string, documentData: Partial<Document>): Observable<Document> {
        return this.http.put<Document>(`${this.apiUrl}/${id}`, documentData);
    }

    deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

}