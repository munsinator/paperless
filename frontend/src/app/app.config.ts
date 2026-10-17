import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { fakeBackendInterceptor } from '../interceptors/fake-backend.interceptor';
import { authInterceptor } from '../interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    //provideHttpClient(),
    provideHttpClient(withInterceptors([authInterceptor, fakeBackendInterceptor])),
    provideRouter(routes)
  ]
};
