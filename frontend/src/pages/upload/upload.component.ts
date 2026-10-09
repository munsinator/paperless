import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DocumentService, UploadDocumentRequest } from '../../services/document.service';
import { DocumentType } from '../../models/document.model';
import { hasDuplicateTags } from '../../validators/tag-list.validator';

@Component({
    selector: 'app-upload',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './upload.component.html',
    styleUrl: './upload.component.css'
})
export class UploadComponent {
    private static readonly maxFileSize = 15 * 1024 * 1024;
    private readonly documentService = inject(DocumentService);
    private readonly router = inject(Router);

    readonly selectedFile = signal<File | null>(null);
    readonly isDragging = signal<boolean>(false);
    readonly isSubmitting = signal<boolean>(false);
    readonly isValidatingFile = signal<boolean>(false);
    readonly errorMessage = signal<string>('');
    readonly successMessage = signal<string>('');

    readonly documentTypes: { label: string; value: DocumentType }[] = [
        { label: 'Rechnung', value: 'RECEIPT' },
        { label: 'Vertrag', value: 'CONTRACT' },
        { label: 'Bank / Finanzen', value: 'BANK_STATEMENT' },
        { label: 'Zertifikat', value: 'CERTIFICATE' },
        { label: 'Notiz', value: 'NOTE' },
        { label: 'Reisepass', value: 'PASSPORT' },
        { label: 'Sonstiges', value: 'OTHER' }
    ];

    private readonly fb = new FormBuilder();
    private readonly tagsValidator: ValidatorFn = control => this.validateTags(String(control.value));
    readonly uploadForm = this.fb.nonNullable.group({
        title: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
        documentType: [''],
        tags: ['', [this.tagsValidator]]
    });
    private readonly formStatus = toSignal(this.uploadForm.statusChanges, {
        initialValue: this.uploadForm.status
    });

    readonly isFormValid = computed(() => {
        return this.selectedFile() !== null
            && this.formStatus() === 'VALID'
            && !this.isValidatingFile()
            && !this.isSubmitting();
    });

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

        if (!file.name.toLocaleLowerCase().endsWith('.pdf')
            || (file.type && file.type !== 'application/pdf')) {
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
            console.error('PDF-Datei konnte nicht gelesen werden:', error);
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
        if (!this.isFormValid()) return;
        const file = this.selectedFile();
        if (!file) return;

        const formValues = this.uploadForm.getRawValue();
        const title = formValues.title.trim();
        const documentType = this.documentTypes.find(item => item.value === formValues.documentType)?.value;
        const request: UploadDocumentRequest = {
            title,
            type: documentType,
            tags: this.normalizeTags(formValues.tags)
        };

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
                console.error('Fehler beim Hochladen des Dokuments:', error);
                this.isSubmitting.set(false);
                this.errorMessage.set(this.getUploadErrorMessage(error));
            }
        });
    }

    private normalizeTags(value: string): string[] {
        const seen = new Set<string>();
        return value.split(',')
            .map(tag => tag.trim())
            .filter(tag => {
                const normalized = tag.toLocaleLowerCase();
                if (!tag || seen.has(normalized)) return false;
                seen.add(normalized);
                return true;
            });
    }

    private validateTags(value: string): { [key: string]: boolean } | null {
        if (!value.trim()) return null;
        const tags = value.split(',').map(tag => tag.trim());
        if (tags.some(tag => !tag)) return { emptyTag: true };
        if (tags.length > 20) return { tooManyTags: true };
        if (tags.some(tag => tag.length > 50)) return { tagTooLong: true };
        if (hasDuplicateTags(value)) return { duplicateTags: true };
        return null;
    }

    private getUploadErrorMessage(error: unknown): string {
        if (!(error instanceof HttpErrorResponse)) {
            return 'Der Upload ist fehlgeschlagen. Bitte versuchen Sie es erneut.';
        }
        if (error.status === 0) return 'Der Server ist nicht erreichbar. Bitte versuchen Sie es später erneut.';
        if (error.status === 401 || error.status === 403) {
            return 'Sie sind nicht berechtigt, dieses Dokument hochzuladen. Bitte melden Sie sich erneut an.';
        }
        if (error.status === 400 || error.status === 422) {
            return 'Der Server hat die Dokumentdaten abgelehnt. Bitte prüfen Sie Titel, Kategorie und Tags.';
        }
        if (error.status === 404 || error.status === 405 || error.status === 415) {
            return 'Der Multipart-Upload-Endpunkt ist noch nicht im Backend implementiert.';
        }
        if (error.status === 413) return 'Die Datei ist für den Server zu groß.';
        if (error.status >= 500) return 'Beim Speichern des Dokuments ist ein Serverfehler aufgetreten.';

        const serverMessage = typeof error.error?.message === 'string' ? error.error.message : null;
        return serverMessage || `Upload fehlgeschlagen (HTTP ${error.status}).`;
    }
}