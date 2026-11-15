import { Component, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-searchbar',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './searchbar.component.html'
})

export class SearchbarComponent {
    readonly search = output<string>();
    // aktuell aktiver Suchbegriff vom Parent (aus der URL)
    readonly value = input('');
    // Eingabe im Feld: startet mit value() und wird bei neuem value() zurückgesetzt, bleibt sonst frei beschreibbar
    protected readonly query = linkedSignal(() => this.value());

    submitSearch(): void {
        this.search.emit(this.query().trim());
    }
}