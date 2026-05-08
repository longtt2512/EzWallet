import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

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
    <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
      <h2 class="text-xl font-semibold">Đăng nhập</h2>

      <mat-form-field appearance="outline">
        <mat-label>Tên đăng nhập / Email / SĐT</mat-label>
        <input matInput formControlName="username" autocomplete="username" />
        @if (form.controls.username.touched && form.controls.username.invalid) {
          <mat-error>Vui lòng nhập tên đăng nhập</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Mật khẩu</mat-label>
        <input
          matInput
          [type]="hidePassword() ? 'password' : 'text'"
          formControlName="password"
          autocomplete="current-password" />
        <button
          mat-icon-button
          matSuffix
          type="button"
          (click)="hidePassword.set(!hidePassword())">
          <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
        </button>
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <mat-error>Mật khẩu tối thiểu 8 ký tự</mat-error>
        }
      </mat-form-field>

      <button
        mat-raised-button
        color="primary"
        type="submit"
        [disabled]="form.invalid || loading()">
        {{ loading() ? 'Đang xử lý...' : 'Đăng nhập' }}
      </button>

      <div class="text-center text-sm">
        Chưa có tài khoản?
        <a routerLink="/auth/register" class="text-primary font-medium">
          Đăng ký ngay
        </a>
      </div>
    </form>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly hidePassword = signal(true);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
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
      error: () => this.loading.set(false),
    });
  }
}
