import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
      <h2 class="text-xl font-semibold">Quên mật khẩu</h2>
      <p class="text-sm text-gray-500">Nhập email hoặc số điện thoại để nhận mã OTP đặt lại mật khẩu.</p>

      <mat-form-field appearance="outline">
        <mat-label>Email / Số điện thoại</mat-label>
        <input matInput formControlName="identifier">
        @if (form.controls.identifier.touched && form.controls.identifier.invalid) {
          <mat-error>Vui lòng nhập thông tin</mat-error>
        }
      </mat-form-field>

      <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
        {{ loading() ? 'Đang gửi...' : 'Gửi mã OTP' }}
      </button>

      <a routerLink="/auth/reset-password" class="text-sm text-center text-primary">Đã có mã OTP? Đặt lại ngay</a>
      <a routerLink="/auth/login" class="text-sm text-center">Quay lại đăng nhập</a>
    </form>
  `,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly form = this.fb.nonNullable.group({
    identifier: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.auth.forgotPassword(this.form.getRawValue().identifier).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Mã OTP đã được gửi đến email/số điện thoại của bạn.', 'Đóng', { duration: 4000 });
        this.router.navigate(['/auth/reset-password']);
      },
      error: () => this.loading.set(false),
    });
  }
}
