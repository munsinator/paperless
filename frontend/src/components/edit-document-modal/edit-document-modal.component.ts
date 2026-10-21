import { Component, input, output, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Document, DOCUMENT_TYPE_OPTIONS } from '../../models/document.model';
import { duplicateTagsValidator } from '../../validators/tag-list.validator';

@Component({
    selector: 'app-edit-document-modal',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './edit-document-modal.component.html'
})
export class EditDocumentModalComponent implements OnInit {
    private readonly fb = inject(FormBuilder);

    // Inputs & Outputs
    readonly document = input.required<Document>();
    readonly documentTypes = DOCUMENT_TYPE_OPTIONS;

    readonly saved = output<Partial<Document>>();
    readonly closed = output<void>();

    // Formular-Definition
    readonly editForm = this.fb.nonNullable.group({
        title: ['', [Validators.required]],
        documentType: [''],
        tags: ['', [duplicateTagsValidator]]
    });

    ngOnInit(): void {
        const doc = this.document();
        this.editForm.patchValue({
            title: doc.title,
            documentType: doc.documentType || '',
            tags: doc.tags ? doc.tags.join(', ') : ''
        });
    }

    onSave(): void {
        if (this.editForm.invalid) return;

        const formValues = this.editForm.getRawValue();

        // Tags-String in Array umwandeln & Trimmen
        const tagsArray = formValues.tags
            .split(',')
            .map(tag => tag.trim())
            .filter(tag => tag.length > 0);

        const documentType = this.documentTypes.find(item => item.value === formValues.documentType)?.value;
        const updatedData: Partial<Document> = {
            title: formValues.title,
            documentType,
            tags: tagsArray
        };

        this.saved.emit(updatedData);
    }

    onClose(): void {
        this.closed.emit();
    }
}