import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '@core/services/auth.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const pw = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
      <h2 class="text-xl font-semibold">Đặt lại mật khẩu</h2>

      <mat-form-field appearance="outline">
        <mat-label>Email / Số điện thoại</mat-label>
        <input matInput formControlName="identifier">
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Mã OTP</mat-label>
        <input matInput formControlName="otpCode" maxlength="6" inputmode="numeric">
        @if (form.controls.otpCode.touched && form.controls.otpCode.invalid) {
          <mat-error>Mã OTP gồm 6 chữ số</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Mật khẩu mới</mat-label>
        <input matInput [type]="hidePassword() ? 'password' : 'text'" formControlName="newPassword">
        <button mat-icon-button matSuffix type="button" (click)="hidePassword.set(!hidePassword())">
          <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
        </button>
        @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
          <mat-error>Mật khẩu 8–64 ký tự</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Xác nhận mật khẩu mới</mat-label>
        <input matInput type="password" formControlName="confirmPassword">
        @if (form.errors?.['passwordMismatch'] && form.controls.confirmPassword.touched) {
          <mat-error>Mật khẩu xác nhận không khớp</mat-error>
        }
      </mat-form-field>

      <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
        {{ loading() ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
      </button>
      <a routerLink="/auth/login" class="text-sm text-center">Quay lại đăng nhập</a>
    </form>
  `,
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly hidePassword = signal(true);

  readonly form = this.fb.nonNullable.group({
    identifier: ['', Validators.required],
    otpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const { identifier, otpCode, newPassword } = this.form.getRawValue();
    this.auth.resetPassword(identifier, otpCode, newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Đặt lại mật khẩu thành công!', 'Đóng', { duration: 3000 });
        this.router.navigate(['/auth/login']);
      },
      error: () => this.loading.set(false),
    });
  }
}
