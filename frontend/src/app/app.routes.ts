import { Routes } from '@angular/router';
import { DashboardComponent } from '../pages/dashboard/dashboard.component';
import { DocumentDetailComponent } from '../pages/document-detail/document-detail.component';
import { AuthComponent } from '../pages/auth/auth.component';
import { UploadComponent } from '../pages/upload/upload.component';

export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full'},

    { path: 'dashboard', component: DashboardComponent},
    { path: 'upload', component: UploadComponent},
    { path: 'document/:id', component: DocumentDetailComponent},
    { path: 'auth', component: AuthComponent},

    { path: '**', redirectTo: 'dashboard'}
];