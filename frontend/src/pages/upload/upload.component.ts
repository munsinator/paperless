import { Component, signal, computed } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

@Component({
    selector: 'app-upload',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './upload.component.html',
    styleUrl: './upload.component.css'
})
export class UploadComponent {
    readonly selectedFile = signal<File | null>(null);
    readonly isDragging = signal<boolean>(false);
    readonly isUploading = signal<boolean>(false);
    readonly uploadProgress = signal<number>(0);
    readonly errorMessage = signal<string>('');
    readonly successMessage = signal<string>('');

    readonly categories = signal<string[]>([
        'Rechnung',
        'Quittung',
        'Mietvertrag',
        'Arbeitsvertrag',
        'Versicherung',
        'Steuerunterlagen',
        'Bank / Finanzen',
        'Sonstiges'
    ]);

    private readonly fb = new FormBuilder();
    readonly uploadForm = this.fb.nonNullable.group({
        title: ['', [Validators.required]],
        category: [''],
        tags: ['']
    });

    readonly isFormValid = computed(() => {
        return this.selectedFile() !== null && this.uploadForm.valid;
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
            this.handleFile(input.files[0]);
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
            this.handleFile(event.dataTransfer.files[0]);
        }
    }

    private handleFile(file: File): void {
        if (file.type !== 'application/pdf') {
            this.errorMessage.set('Bitte wählen Sie eine gültige PDF-Datei aus.');
            return;
        }

        this.errorMessage.set('');
        this.selectedFile.set(file);

        // Titel automatisch aus Dateinamen ausfüllen
        if (!this.uploadForm.controls.title.value) {
            this.uploadForm.patchValue({ title: file.name });
        }
    }

    removeFile(fileInput: HTMLInputElement): void {
        this.selectedFile.set(null);
        fileInput.value = '';
        this.uploadForm.patchValue({ title: '' });
    }

    onUpload(): void {
        if (!this.isFormValid()) return;

        this.isUploading.set(true);
        // Spätere Anbindung an den DocumentService
    }
}