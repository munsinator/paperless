import { Component, effect, input, output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Document, DocumentType } from '../../models/document.model';
import { TagService } from '../../services/tag.service';

@Component({
    selector: 'app-edit-document-modal',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './edit-document-modal.component.html'
})
export class EditDocumentModalComponent {
    private readonly fb = inject(FormBuilder);
    private readonly tagService = inject(TagService);

    readonly document = input.required<Document>();
    readonly documentTypes: { label: string; value: DocumentType }[] = [
        { label: 'Rechnung', value: 'RECEIPT' },
        { label: 'Vertrag', value: 'CONTRACT' },
        { label: 'Bank / Finanzen', value: 'BANK_STATEMENT' },
        { label: 'Zertifikat', value: 'CERTIFICATE' },
        { label: 'Notiz', value: 'NOTE' },
        { label: 'Reisepass', value: 'PASSPORT' },
        { label: 'Sonstiges', value: 'OTHER' }
    ];

    readonly isSaving = input(false);
    readonly saved = output<Partial<Document>>();
    readonly closed = output<void>();

    // 
    readonly editForm = this.fb.nonNullable.group({
        title: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
        documentType: this.fb.nonNullable.control<DocumentType | ''>(''),
        tags: ['', [this.tagService.validator]]
    });

    constructor() {
        effect(() => {
            const doc = this.document();
            this.editForm.patchValue({
                title: doc.title,
                documentType: doc.documentType || '',
                tags: this.tagService.formatTags(doc.tags)
            });
            this.editForm.markAsPristine();
        });
    }

    onSave(): void {
        if (this.editForm.invalid) {
            this.editForm.markAllAsTouched();
            return;
        }
        if (this.editForm.pristine) {
            this.closed.emit();
            return;
        }

        const { title, documentType, tags } = this.editForm.getRawValue();

        const updatedData: Partial<Document> = {
            title: title.trim(),
            documentType: documentType || undefined,
            tags: this.tagService.parseTags(tags)
        };

        this.saved.emit(updatedData);
    }

    onClose(): void {
        this.closed.emit();
    }
}