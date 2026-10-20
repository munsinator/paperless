import { Component, computed, inject, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { AuthService } from "../../services/auth.service";

interface NavbarItem {
    label: string;
    route?: string;
    requiresAuthentication?: boolean;
    action?: 'logout';
}

@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [RouterLink],
    templateUrl: './navbar.component.html',
    styleUrl: './navbar.component.css'
})

export class NavbarComponent {
    private readonly authService = inject(AuthService);
    readonly isLoggedIn = this.authService.isLoggedIn;
    readonly isMenuOpen = signal(false);
    readonly menuItems = computed<NavbarItem[]>(() => {
        const items: NavbarItem[] = this.isLoggedIn()
            ? [
                { label: 'Dashboard', route: '/dashboard', requiresAuthentication: true },
                { label: 'Upload', route: '/upload', requiresAuthentication: true },
                { label: 'Logout', action: 'logout' }
            ]
            : [{ label: 'Login', route: '/auth' }];

        return items.filter(item => !item.requiresAuthentication || this.isLoggedIn());
    });

    toggleMenu(): void {
        this.isMenuOpen.update(open => !open);
    }

    closeMenu(): void {
        this.isMenuOpen.set(false);
    }

    logout(): void {
        this.closeMenu();
        this.authService.logout();
    }
}
