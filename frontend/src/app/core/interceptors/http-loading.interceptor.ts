import { HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, finalize, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LoadingService } from '../services/loading.service';

/**
 * Functional HTTP interceptor (Angular 15+ style).
 * - Increments/decrements the global loading counter around every request.
 * - Shows a MatSnackBar error banner on network failure (status 0) or 5xx responses.
 */
export const httpLoadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loading = inject(LoadingService);
  const snackBar = inject(MatSnackBar);

  loading.increment();

  return next(req).pipe(
    catchError(err => {
      const status: number = err.status ?? 0;
      if (status === 0 || status >= HttpStatusCode.InternalServerError) {
        snackBar.open(
          status === 0
            ? 'Cannot reach the server. Please check your connection.'
            : `Server error (${status}). Please try again later.`,
          'Dismiss',
          { duration: 6000, panelClass: 'snack-error' }
        );
      }
      return throwError(() => err);
    }),
    finalize(() => loading.decrement()),
  );
};
