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
    readonly search = output<string>();
    query = '';

    submitSearch(): void {
        this.search.emit(this.query.trim());
    }
}