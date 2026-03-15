import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

// Module-level state — survives across requests
let isRefreshing = false;
const refreshSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<any>,
  next: HttpHandlerFn,
): Observable<HttpEvent<any>> => {
  const token = localStorage.getItem('hs_token');

  // Attach token exactly as your original did
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((err) => {
      // Only intercept 401s that are NOT from auth endpoints
      // (avoids infinite loop on login/refresh failures)
      if (err.status === 401 && !req.url.includes('/api/auth/')) {
        return handle401(req, next);
      }
      return throwError(() => err);
    }),
  );
};

function handle401(req: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!isRefreshing) {
    isRefreshing = true;
    refreshSubject.next(null);

    return auth.refreshToken().pipe(
      switchMap((res) => {
        isRefreshing = false;
        const newToken = res.accessToken;
        refreshSubject.next(newToken);

        // Retry the original request with new token
        return next(
          req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` },
          }),
        );
      }),
      catchError((err) => {
        // Refresh failed — session is truly expired, force logout
        isRefreshing = false;
        auth.logout();
        router.navigate(['/login']);
        return throwError(() => err);
      }),
    );
  }

  // Another request already triggered a refresh — wait for it to finish
  return refreshSubject.pipe(
    filter((token) => token !== null),
    take(1),
    switchMap((token) =>
      next(
        req.clone({
          setHeaders: { Authorization: `Bearer ${token!}` },
        }),
      ),
    ),
  );
}
