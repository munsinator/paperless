import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { DocumentType } from '../../models/document.model';
import { DocumentService } from '../../services/document.service';
import { DomSanitizer } from '@angular/platform-browser';
import { DocumentFormService } from '../../services/document-form.service';
import { NotificationService } from '../../services/notification.service';
import { toErrorMessage } from '../../utils/http-error';

@Component({
    selector: 'app-document-detail',
    standalone: true,
    imports: [RouterLink, DatePipe, ReactiveFormsModule],
    templateUrl: './document-detail.component.html'
})
export class DocumentDetailComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly documentService = inject(DocumentService);
    private readonly sanitizer = inject(DomSanitizer);
    private readonly documentForm = inject(DocumentFormService);
    private readonly notifications = inject(NotificationService);

    readonly documentId = toSignal(
        this.route.paramMap.pipe(map(params => params.get('id') ?? '')),
        { initialValue: '' }
    );

    readonly documentResource = rxResource({
        params: () => this.documentId() || undefined,
        stream: ({ params: id }) => this.documentService.getDocumentById(id)
    });

    readonly document = this.documentResource.value;
    readonly loadErrorMessage = computed(() => {
        const error = this.documentResource.error();
        return error ? toErrorMessage(this.unwrap(error), 'load') : null;
    });

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

    readonly editForm = this.documentForm.createForm();

    constructor() {
        effect(() => {
            const doc = this.document();
            if (doc) {
                this.editForm.patchValue(this.documentForm.toFormValue(doc));
                this.editForm.markAsPristine();
            }
        });
    }

    onSave(): void {
        if (this.editForm.invalid) {
            this.editForm.markAllAsTouched();
            return;
        }
        if (this.editForm.pristine) return;

        const id = this.documentId();
        if (!id) return;

        this.isSaving.set(true);
        this.saveSuccess.set(false);

        const updatedData = this.documentForm.toDocumentData(this.editForm.getRawValue());

        this.documentService.updateDocument(id, updatedData).subscribe({
            next: () => {
                this.isSaving.set(false);
                this.saveSuccess.set(true);
                this.editForm.markAsPristine();
                this.documentResource.reload();
                setTimeout(() => this.saveSuccess.set(false), 3000);
            },
            error: (err) => {
                this.isSaving.set(false);
                this.notifications.httpError(err, 'save');
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
                this.isDeleting.set(false);
                this.notifications.httpError(err, 'delete');
            }
        });
    }

    // rxResource verpackt Fehler manchmal in einen Wrapper-Error mit dem Original in cause
    private unwrap(error: unknown): unknown {
        return error instanceof Error && error.cause ? error.cause : error;
    }
}
