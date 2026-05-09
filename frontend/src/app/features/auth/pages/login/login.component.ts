import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="flex flex-col">
      <div class="mb-7">
        <h2 class="text-2xl font-bold" style="color:#F8FAFC">Đăng nhập</h2>
        <p class="text-sm mt-1" style="color:#94A3B8">Chào mừng trở lại! Vui lòng nhập thông tin.</p>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Tên đăng nhập / Email / SĐT</mat-label>
          <input matInput formControlName="identifier" autocomplete="username" />
          @if (form.controls.identifier.touched && form.controls.identifier.invalid) {
            <mat-error>Vui lòng nhập tên đăng nhập</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Mật khẩu</mat-label>
          <input matInput [type]="hidePassword() ? 'password' : 'text'"
                 formControlName="password" autocomplete="current-password" />
          <button mat-icon-button matSuffix type="button"
                  (click)="hidePassword.set(!hidePassword())"
                  [attr.aria-label]="hidePassword() ? 'Hiện mật khẩu' : 'Ẩn mật khẩu'">
            <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <mat-error>Mật khẩu tối thiểu 8 ký tự</mat-error>
          }
        </mat-form-field>

        <div class="text-right -mt-2 mb-1">
          <a routerLink="/auth/forgot-password"
             class="text-sm font-medium" style="color:#F59E0B">
            Quên mật khẩu?
          </a>
        </div>

        <button mat-flat-button color="primary" type="submit"
                class="w-full h-12 text-base font-semibold rounded-lg"
                [disabled]="form.invalid || loading()">
          @if (loading()) {
            <mat-icon class="animate-spin mr-2">sync</mat-icon>
          }
          {{ loading() ? 'Đang xử lý...' : 'Đăng nhập' }}
        </button>
      </form>

      <p class="text-center text-sm mt-6" style="color:#94A3B8">
        Chưa có tài khoản?
        <a routerLink="/auth/register" class="font-semibold hover:underline" style="color:#F59E0B">
          Đăng ký ngay
        </a>
      </p>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly hidePassword = signal(true);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Đăng nhập thất bại';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }
}
