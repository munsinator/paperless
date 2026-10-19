import { Component, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-searchbar',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './searchbar.component.html',
    styleUrl: './searchbar.component.css'
})

export class SearchbarComponent {
    // kein signal sonst müsste parent (dashboard) schauen, ob sich der Wert geändert hat
    readonly search = output<string>();
    query = '';

    submitSearch(): void {
        this.search.emit(this.query.trim());
    }
}