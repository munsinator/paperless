import { Component, effect, input, output, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Document, DOCUMENT_TYPE_OPTIONS } from '../../models/document.model';
import { DocumentFormService } from '../../services/document-form.service';

@Component({
    selector: 'app-edit-document-modal',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './edit-document-modal.component.html'
})
export class EditDocumentModalComponent {
    private readonly documentForm = inject(DocumentFormService);

    readonly document = input.required<Document>();
    readonly documentTypes = DOCUMENT_TYPE_OPTIONS;

    readonly isSaving = input(false);
    readonly saved = output<Partial<Document>>();
    readonly closed = output<void>();

    readonly editForm = this.documentForm.createForm();

    constructor() {
        effect(() => {
            this.editForm.patchValue(this.documentForm.toFormValue(this.document()));
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

        this.saved.emit(this.documentForm.toDocumentData(this.editForm.getRawValue()));
    }

    onClose(): void {
        this.closed.emit();
    }
}