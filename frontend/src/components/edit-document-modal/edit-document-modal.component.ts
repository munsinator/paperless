import { Component, input, output, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Document } from '../../models/document.model';

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
    readonly categories = input<string[]>(['Rechnung', 'Vertrag', 'Wohnen', 'Finanzen', 'Arbeit', 'Gesundheit', 'Versicherung']);

    readonly saved = output<Partial<Document>>();
    readonly closed = output<void>();

    // Formular-Definition
    readonly editForm = this.fb.nonNullable.group({
        title: ['', [Validators.required]],
        category: [''],
        tags: ['']
    });

    ngOnInit(): void {
        const doc = this.document();
        this.editForm.patchValue({
            title: doc.title,
            category: doc.category || '',
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

        const updatedData: Partial<Document> = {
            title: formValues.title,
            category: formValues.category || undefined,
            tags: tagsArray
        };

        this.saved.emit(updatedData);
    }

    onClose(): void {
        this.closed.emit();
    }
}