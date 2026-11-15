import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { DOCUMENT_TYPE_OPTIONS } from '../../models/document.model';
import { DocumentService } from '../../services/document.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
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
    readonly isDownloading = signal<boolean>(false);
    readonly saveSuccess = signal<boolean>(false);

    readonly documentTypes = DOCUMENT_TYPE_OPTIONS;

    // lädt die PDF für die Vorschau (über HttpClient, damit der JWT mitgeschickt wird)
    readonly previewResource = rxResource({
        params: () => this.documentId() || undefined,
        stream: ({ params: id }) => this.documentService.downloadDocument(id)
    });

    readonly safeFileUrl = signal<SafeResourceUrl | null>(null);

    readonly editForm = this.documentForm.createForm();

    constructor() {
        effect((onCleanup) => { // onCleanup: ausgeführt, bevor der Effect erneut ausgeführt wird oder wenn der Effect zerstört wird.
            const blob = this.previewResource.hasValue() ? this.previewResource.value() : undefined;
            if (!blob) {
                this.safeFileUrl.set(null);
                return;
            }
            const url = URL.createObjectURL(blob);
            this.safeFileUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url)); // Sanitizer: überprüft URLs, bevor sie in HTML-Elemente eingefügt werden
            // neuen Blob und Effect wird erneut ausgeführt; Komponente wird zerstört, z.B. Detailseite verlassen
            onCleanup(() => URL.revokeObjectURL(url));
        });

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

    onDownload(): void {
        const id = this.documentId();
        const title = this.document()?.title;
        if (!id || !title) return;

        this.isDownloading.set(true);
        this.documentService.downloadDocument(id).subscribe({
            next: blob => {
                this.isDownloading.set(false);
                this.saveFile(blob, `${title}.pdf`);
            },
            error: (err) => {
                this.isDownloading.set(false);
                this.notifications.httpError(err, 'download');
            }
        });
    }

    // löst den Browser-Download aus: unsichtbarer Link auf eine temporäre URL des Blobs
    private saveFile(blob: Blob, filename: string): void {
        const url = URL.createObjectURL(blob);
        const link = window.document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
    }

    // rxResource verpackt Fehler manchmal in einen Wrapper-Error mit dem Original in cause
    private unwrap(error: unknown): unknown {
        return error instanceof Error && error.cause ? error.cause : error;
    }
}
