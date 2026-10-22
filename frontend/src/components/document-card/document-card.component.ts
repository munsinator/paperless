import { Component, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Document, getDocumentTypeLabel } from '../../models/document.model';
import { EditDocumentModalComponent } from '../edit-document-modal/edit-document-modal.component';

// done meldet der Karte, ob das Speichern geklappt hat (Modal bleibt sonst offen)
export interface DocumentUpdateEvent {
    id: string;
    data: Partial<Document>;
    done: (success: boolean) => void;
}

@Component({
    selector: 'app-document-card',
    standalone: true,
    imports: [RouterLink, DatePipe, EditDocumentModalComponent],
    templateUrl: './document-card.component.html'
})
export class DocumentCardComponent {
    readonly document = input.required<Document>();
    readonly documentTypeLabel = getDocumentTypeLabel;

    // State für das Edit-Modal
    readonly isEditModalOpen = signal(false);
    readonly isSavingEdit = signal(false);

    // Output Events
    readonly delete = output<string>();
    readonly update = output<DocumentUpdateEvent>();

    openEditModal(): void {
        this.isEditModalOpen.set(true);
    }

    closeEditModal(): void {
        this.isEditModalOpen.set(false);
    }

    onSaveEdit(updatedData: Partial<Document>): void {
        if (this.isSavingEdit()) return;
        this.isSavingEdit.set(true);
        this.update.emit({
            id: this.document().id,
            data: updatedData,
            done: success => {
                this.isSavingEdit.set(false);
                if (success) this.closeEditModal();
            }
        });
    }

    onDelete(): void {
        this.delete.emit(this.document().id);
    }
}