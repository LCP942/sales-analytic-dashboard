import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { httpLoadingInterceptor } from './core/interceptors/http-loading.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    // provideHttpClient with Fetch API (Angular 19 / modern approach) and loading interceptor
    provideHttpClient(
      withFetch(),
      withInterceptors([httpLoadingInterceptor]),
    ),
    provideAnimationsAsync(),
  ],
};
