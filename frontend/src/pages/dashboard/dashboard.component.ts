import { Component, inject, computed, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { SearchbarComponent } from '../../components/searchbar/searchbar.component';
import { DocumentCardComponent } from '../../components/document-card/document-card.component';
import { Document } from '../../models/document.model';
import { DocumentService } from '../../services/document.service';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [DocumentCardComponent, SearchbarComponent],
    templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
    private readonly documentService = inject(DocumentService);
    private readonly searchQuery = signal('');

    readonly documentsResource = rxResource({
        params: () => this.searchQuery(),
        stream: ({ params: query }) => this.documentService.getDocuments(query)
    });

    readonly allDocuments = computed(() => this.documentsResource.value() ?? []);

    onSearch(query: string): void {
        this.searchQuery.set(query);
    }


    onDeleteDocument(id: string): void {
        if (!confirm('Möchtest du dieses Dokument wirklich löschen?')) return;

        this.documentService.deleteDocument(id).subscribe({
            next: () => this.documentsResource.update(docs => docs?.filter(doc => doc.id !== id)),
            error: (err) => console.error('Fehler beim Löschen:', err)
        });
    }

    onUpdateDocument(event: { id: string; data: Partial<Document> }): void {
        this.documentService.updateDocument(event.id, event.data).subscribe({
            next: (updatedDoc) => this.documentsResource.update(docs =>
                docs?.map(doc => doc.id === event.id ? { ...doc, ...updatedDoc } : doc)
            ),
            error: (err) => console.error('Fehler beim Aktualisieren:', err)
        });
    }
}