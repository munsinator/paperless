import { Component, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Document, documentTypeLabel } from '../../models/document.model';
import { EditDocumentModalComponent } from '../edit-document-modal/edit-document-modal.component';

@Component({
    selector: 'app-document-card',
    standalone: true,
    imports: [RouterLink, DatePipe, EditDocumentModalComponent],
    templateUrl: './document-card.component.html'
})
export class DocumentCardComponent {
    readonly document = input.required<Document>();
    readonly documentTypeLabel = documentTypeLabel;

    // State für das Edit-Modal
    readonly isEditModalOpen = signal(false);

    // Output Events
    readonly delete = output<string>();
    readonly update = output<{ id: string; data: Partial<Document> }>();

    openEditModal(): void {
        this.isEditModalOpen.set(true);
    }

    closeEditModal(): void {
        this.isEditModalOpen.set(false);
    }

    onSaveEdit(updatedData: Partial<Document>): void {
        this.update.emit({
            id: this.document().id,
            data: updatedData
        });
        this.closeEditModal();
    }

    onDelete(): void {
        this.delete.emit(this.document().id);
    }
}