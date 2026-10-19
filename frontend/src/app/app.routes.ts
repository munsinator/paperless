import { Routes } from '@angular/router';
import { DashboardComponent } from '../pages/dashboard/dashboard.component';
import { DocumentDetailComponent } from '../pages/document-detail/document-detail.component';
import { AuthComponent } from '../pages/auth/auth.component';
import { UploadComponent } from '../pages/upload/upload.component';
import { authGuard } from '../guards/auth.guard';

export const routes: Routes = [
    {
        path: '',
        canActivateChild: [authGuard],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: DashboardComponent },
            { path: 'upload', component: UploadComponent },
            { path: 'document/:id', component: DocumentDetailComponent }
        ]
    },
    { path: 'auth', component: AuthComponent },
    // lazy laden der 404 Page
    { path: '**', loadComponent: () => import('../pages/not-found/not-found.component').then(m => m.NotFoundComponent) }
];