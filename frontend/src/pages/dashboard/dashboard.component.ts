import { Component, inject, computed, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { SearchbarComponent } from '../../components/searchbar/searchbar.component';
import { DocumentCardComponent } from '../../components/document-card/document-card.component';
import { DocumentService } from '../../services/document.service';
import { NotificationService } from '../../services/notification.service';
import { DocumentUpdateEvent } from '../../components/document-card/document-card.component';
import { toErrorMessage } from '../../utils/http-error';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [DocumentCardComponent, SearchbarComponent],
    templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
    private readonly documentService = inject(DocumentService);
    private readonly notifications = inject(NotificationService);
    private readonly router = inject(Router);

    // Suchbegriff steht in der URL (?q=...) und wird vom Router in den Input geschrieben; ohne Parameter kommt undefined
    readonly q = input('', { transform: (value: string | undefined) => value ?? '' });

    readonly documentsResource = rxResource({
        params: () => this.q(),
        stream: ({ params: query }) => this.documentService.getDocuments(query)
    });

    readonly allDocuments = computed(() => this.documentsResource.value() ?? []);

    // passende Meldung ("Die Suche ist derzeit nicht verfügbar")
    readonly errorMessage = computed(() =>
        toErrorMessage(this.documentsResource.error(), this.q() ? 'search' : 'load'));

    onSearch(query: string): void {
        // leerer Begriff entfernt ?q= aus der URL; merge: andere Query-Parameter bleiben erhalten
        void this.router.navigate([], { queryParams: { q: query || null }, queryParamsHandling: 'merge' });
    }


    onDeleteDocument(id: string): void {
        if (!confirm('Möchtest du dieses Dokument wirklich löschen?')) return;

        this.documentService.deleteDocument(id).subscribe({
            next: () => {
                this.documentsResource.update(docs => docs?.filter(doc => doc.id !== id));
                this.notifications.success('Dokument wurde gelöscht.');
            },
            error: (err) => this.notifications.httpError(err, 'delete')
        });
    }

    onUpdateDocument(event: DocumentUpdateEvent): void {
        this.documentService.updateDocument(event.id, event.data).subscribe({
            next: (updatedDoc) => {
                this.documentsResource.update(docs =>
                    docs?.map(doc => doc.id === event.id ? { ...doc, ...updatedDoc } : doc)
                );
                this.notifications.success('Änderungen wurden gespeichert.');
                event.done(true);
            },
            error: (err) => {
                this.notifications.httpError(err, 'save');
                event.done(false);
            }
        });
    }
}