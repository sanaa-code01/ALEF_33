import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { demoInterceptor } from './demo/demo.interceptor';
import { isDemoMode } from './demo/demo-mode';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // Every HTTP call goes through the interceptor, which adds the JWT token.
    // In demo mode (GitHub Pages), the demo interceptor answers first, in the browser.
    provideHttpClient(withInterceptors(isDemoMode() ? [demoInterceptor, authInterceptor] : [authInterceptor])),
  ],
};
