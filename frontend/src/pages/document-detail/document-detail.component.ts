import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { Document, DocumentType } from '../../models/document.model';
import { DocumentService } from '../../services/document.service';
import { DomSanitizer } from '@angular/platform-browser';
import { duplicateTagsValidator } from '../../validators/tag-list.validator';

@Component({
    selector: 'app-document-detail',
    standalone: true,
    imports: [RouterLink, DatePipe, ReactiveFormsModule],
    templateUrl: './document-detail.component.html',
    styleUrl: './document-detail.component.css'
})
export class DocumentDetailComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly fb = inject(FormBuilder);
    private readonly documentService = inject(DocumentService);
    private readonly sanitizer = inject(DomSanitizer);

    readonly documentId = toSignal(
        this.route.paramMap.pipe(map(params => params.get('id') ?? '')),
        { initialValue: '' }
    );

    readonly documentResource = rxResource({
        params: () => this.documentId() || undefined,
        stream: ({ params: id }) => this.documentService.getDocumentById(id)
    });

    readonly document = this.documentResource.value;

    readonly activeTab = signal<'metadata' | 'ai'>('metadata');
    readonly isSaving = signal<boolean>(false);
    readonly isDeleting = signal<boolean>(false);
    readonly saveSuccess = signal<boolean>(false);

    readonly documentTypes: { label: string; value: DocumentType }[] = [
        { label: 'Rechnung', value: 'RECEIPT' },
        { label: 'Vertrag', value: 'CONTRACT' },
        { label: 'Bank / Finanzen', value: 'BANK_STATEMENT' },
        { label: 'Zertifikat', value: 'CERTIFICATE' },
        { label: 'Notiz', value: 'NOTE' },
        { label: 'Reisepass', value: 'PASSPORT' },
        { label: 'Sonstiges', value: 'OTHER' }
    ];

    readonly safeFileUrl = computed(() => {
        const url = this.document()?.fileUrl;
        return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
    });

    readonly editForm = this.fb.nonNullable.group({
        title: ['', [Validators.required]],
        documentType: [''],
        tags: ['', [duplicateTagsValidator]]
    });

    constructor() {
        effect(() => {
            const doc = this.document();
            if (doc) {
                this.editForm.patchValue({
                    title: doc.title,
                    documentType: doc.documentType ?? '',
                    tags: doc.tags ? doc.tags.join(', ') : ''
                });
            }
        });
    }

    onSave(): void {
        if (this.editForm.invalid) return;

        const id = this.documentId();
        if (!id) return;

        this.isSaving.set(true);
        this.saveSuccess.set(false);

        const formValues = this.editForm.getRawValue();
        const tagsArray = formValues.tags
            .split(',')
            .map(t => t.trim())
            .filter(t => t.length > 0);

        const updatedData: Partial<Document> = {
            title: formValues.title,
            documentType: this.documentTypes.find(item => item.value === formValues.documentType)?.value,
            tags: tagsArray
        };

        this.documentService.updateDocument(id, updatedData).subscribe({
            next: () => {
                this.isSaving.set(false);
                this.saveSuccess.set(true);
                this.documentResource.reload();
                setTimeout(() => this.saveSuccess.set(false), 3000);
            },
            error: (err) => {
                console.error('Fehler beim Speichern:', err);
                this.isSaving.set(false);
            }
        });
    }

    onDelete(): void {
        const id = this.documentId();
        if (!id || !confirm('Möchtest du dieses Dokument wirklich unwiderruflich löschen?')) return;

        this.isDeleting.set(true);
        this.documentService.deleteDocument(id).subscribe({
            next: () => {
                this.isDeleting.set(false);
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                console.error('Fehler beim Löschen:', err);
                this.isDeleting.set(false);
            }
        });
    }
}