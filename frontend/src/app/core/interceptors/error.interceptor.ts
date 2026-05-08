import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Chuyển HttpErrorResponse thành thông báo người dùng + xử lý 401 (logout).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const apiMessage = err.error?.message ?? 'Đã có lỗi xảy ra';

      if (err.status === 401) {
        auth.logout();
        router.navigate(['/auth/login']);
      }

      // Bỏ qua thông báo cho lỗi validate, để form tự xử lý theo errors[]
      if (err.status !== 0 && err.error?.code !== 'VALIDATION_FAILED') {
        snackBar.open(apiMessage, 'Đóng', {
          duration: 4000,
          panelClass: ['snack-error'],
        });
      }

      return throwError(() => err);
    })
  );
};
