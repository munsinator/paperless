import { Routes } from '@angular/router';
import { DashboardComponent } from '../pages/dashboard/dashboard.component';
import { DocumentDetailComponent } from '../pages/document-detail/document-detail.component';
import { AuthComponent } from '../pages/auth/auth.component';
import { UploadComponent } from '../pages/upload/upload.component';
import { authGuard } from '../guards/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full'},

    { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard]},
    { path: 'upload', component: UploadComponent, canActivate: [authGuard]},
    { path: 'document/:id', component: DocumentDetailComponent, canActivate: [authGuard]},
    { path: 'auth', component: AuthComponent},

    { path: '**', redirectTo: 'dashboard'}
];