import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Skip SSE subscribe endpoint — it's a long-lived connection
  if (req.url.includes('/notifications/subscribe')) {
    return next(req);
  }

  // Skip AI chat endpoint — has its own loading state inside the chatbot
  if (req.url.includes('/chat')) {
    return next(req);
  }

  loadingService.show();

  return next(req).pipe(
    finalize(() => loadingService.hide())
  );
};
