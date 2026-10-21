import { Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DocumentService, UploadDocumentRequest } from '../../services/document.service';
import { DocumentFormService } from '../../services/document-form.service';
import { toErrorMessage } from '../../utils/http-error';
import { LoggerService } from '../../services/logger.service';
import { DOCUMENT_TYPE_OPTIONS, DocumentType, getDocumentTypeLabel } from '../../models/document.model';

const SOURCE = 'UploadComponent';

@Component({
    selector: 'app-upload',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './upload.component.html'
})
export class UploadComponent {
    private static readonly maxFileSize = 15 * 1024 * 1024;
    private readonly documentService = inject(DocumentService);
    private readonly router = inject(Router);
    private readonly documentForm = inject(DocumentFormService);
    private readonly logger = inject(LoggerService);

    readonly selectedFile = signal<File | null>(null);
    readonly isDragging = signal<boolean>(false);
    readonly isSubmitting = signal<boolean>(false);
    readonly isValidatingFile = signal<boolean>(false);
    readonly errorMessage = signal<string>('');
    readonly successMessage = signal<string>('');

    readonly documentTypes = DOCUMENT_TYPE_OPTIONS;

    readonly uploadForm = this.documentForm.createForm();
    readonly formattedFileSize = computed(() => {
        const file = this.selectedFile();
        if (!file) return '0 KB';
        const sizeInKb = file.size / 1024;
        return sizeInKb > 1024
            ? `${(sizeInKb / 1024).toFixed(2)} MB`
            : `${Math.round(sizeInKb)} KB`;
    });

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files?.length) {
            void this.handleFile(input.files[0]);
            input.value = '';
        }
    }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
        this.isDragging.set(true);
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault();
        this.isDragging.set(false);
    }

    onDrop(event: DragEvent): void {
        event.preventDefault();
        this.isDragging.set(false);

        if (event.dataTransfer?.files.length) {
            void this.handleFile(event.dataTransfer.files[0]);
        }
    }

    private async handleFile(file: File): Promise<void> {
        this.errorMessage.set('');
        this.successMessage.set('');
        this.selectedFile.set(null);
        this.logger.debug(SOURCE,`File selected: '${file.name}' (${file.size} bytes, ${file.type || 'unknown type'})`);

        if (!file.name.toLocaleLowerCase().endsWith('.pdf')
            || (file.type && file.type !== 'application/pdf')) {
            this.logger.warn(SOURCE,`Rejected '${file.name}': not a PDF`);
            this.errorMessage.set('Bitte wählen Sie eine PDF-Datei aus.');
            return;
        }

        if (file.size === 0) {
            this.errorMessage.set('Die ausgewählte PDF-Datei ist leer.');
            return;
        }

        if (file.size > UploadComponent.maxFileSize) {
            this.errorMessage.set('Die PDF-Datei darf höchstens 15 MB groß sein.');
            return;
        }

        this.isValidatingFile.set(true);
        try {
            const signature = new TextDecoder().decode(await file.slice(0, 5).arrayBuffer());
            if (signature !== '%PDF-') {
                this.errorMessage.set('Die Datei enthält keinen gültigen PDF-Dateikopf.');
                return;
            }
            this.selectedFile.set(file);

            if (!this.uploadForm.controls.title.value.trim()) {
                this.uploadForm.patchValue({ title: file.name.replace(/\.pdf$/i, '') });
            }
        } catch (error) {
            this.logger.error(SOURCE,`Could not read file '${file.name}'`, error);
            this.errorMessage.set('Die ausgewählte Datei konnte nicht geprüft werden.');
        } finally {
            this.isValidatingFile.set(false);
        }
    }

    removeFile(fileInput: HTMLInputElement): void {
        this.selectedFile.set(null);
        fileInput.value = '';
    }

    onUpload(): void {
        this.uploadForm.markAllAsTouched();
        if (this.uploadForm.invalid || this.isValidatingFile() || this.isSubmitting()) return;

        const file = this.selectedFile();
        if (!file) {
            this.errorMessage.set('Bitte wählen Sie eine PDF-Datei aus.');
            return;
        }

        const { title, documentType, tags } = this.documentForm.toDocumentData(this.uploadForm.getRawValue());
        const request: UploadDocumentRequest = { title: title ?? '', type: documentType, tags: tags ?? [] };

        this.errorMessage.set('');
        this.successMessage.set('');
        this.isSubmitting.set(true);

        this.documentService.uploadDocument(file, request).subscribe({
            next: document => {
                this.isSubmitting.set(false);
                this.successMessage.set('Das Dokument wurde erfolgreich hochgeladen.');
                window.setTimeout(() => {
                    void this.router.navigate(['/document', document.id]);
                }, 1000);
            },
            error: (error: unknown) => {
                this.logger.error(SOURCE,'Upload failed', error);
                this.isSubmitting.set(false);
                this.errorMessage.set(toErrorMessage(error, 'upload'));
            }
        });
    }
}
