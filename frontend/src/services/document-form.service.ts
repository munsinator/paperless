import { inject, Injectable } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Document, DocumentType } from '../models/document.model';
import { TagService } from './tag.service';

export interface DocumentFormValue {
    title: string;
    documentType: DocumentType | '';
    tags: string;
}

// gemeinsames Formular für Upload, Detailseite und Edit-Modal
@Injectable({ providedIn: 'root' })
export class DocumentFormService {
    private readonly fb = inject(FormBuilder);
    private readonly tagService = inject(TagService);

    createForm() {
        return this.fb.nonNullable.group({
            title: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
            documentType: this.fb.nonNullable.control<DocumentType | ''>(''),
            tags: ['', [this.tagService.validator]]
        });
    }

    // Bereitet vorhandene Dokumentdaten für das Formular auf
    toFormValue(document: Document): DocumentFormValue {
        return {
            title: document.title,
            documentType: document.documentType ?? '',
            tags: this.tagService.formatTags(document.tags)
        };
    }

    // wandelt Formulardaten in die Datenstruktur um, die zum Speichern des Dokuments benötigt wird
    toDocumentData(value: DocumentFormValue): Partial<Document> {
        return {
            title: value.title.trim(),
            documentType: value.documentType || undefined,
            tags: this.tagService.parseTags(value.tags)
        };
    }
}
