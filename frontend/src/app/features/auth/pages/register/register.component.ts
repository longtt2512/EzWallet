import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';

import { AuthService } from '@core/services/auth.service';

function passwordMatchValidator(g: AbstractControl): ValidationErrors | null {
  const pw = g.get('password')?.value;
  const confirm = g.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-register',
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
    MatStepperModule,
  ],
  template: `
    <div class="flex flex-col">
      <div class="mb-6">
        <h2 class="text-2xl font-bold" style="color:#F8FAFC">Tạo tài khoản</h2>
        <p class="text-sm mt-1" style="color:#94A3B8">Điền thông tin bên dưới để bắt đầu.</p>
      </div>

      <mat-stepper [linear]="true" #stepper class="bg-transparent shadow-none">
        <!-- Step 1: Account info -->
        <mat-step [stepControl]="accountForm" label="Thông tin">
          <form [formGroup]="accountForm" class="flex flex-col gap-3 pt-2">

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Tên đăng nhập</mat-label>
              <input matInput formControlName="username" autocomplete="username" />
              @if (accountForm.controls.username.touched && accountForm.controls.username.invalid) {
                <mat-error>Tên đăng nhập 3–50 ký tự</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Họ và tên</mat-label>
              <input matInput formControlName="fullName" autocomplete="name" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email" />
              @if (accountForm.controls.email.touched && accountForm.controls.email.invalid) {
                <mat-error>Email không hợp lệ</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Số điện thoại</mat-label>
              <input matInput formControlName="phone" type="tel" autocomplete="tel" />
              @if (accountForm.controls.phone.touched && accountForm.controls.phone.invalid) {
                <mat-error>Số điện thoại không hợp lệ (VD: 0912345678)</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Mật khẩu</mat-label>
              <input matInput [type]="hidePassword() ? 'password' : 'text'"
                     formControlName="password" autocomplete="new-password" />
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (accountForm.controls.password.touched && accountForm.controls.password.invalid) {
                <mat-error>Mật khẩu 8–64 ký tự</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Nhập lại mật khẩu</mat-label>
              <input matInput [type]="hideConfirm() ? 'password' : 'text'"
                     formControlName="confirmPassword" autocomplete="new-password" />
              <button mat-icon-button matSuffix type="button"
                      (click)="hideConfirm.set(!hideConfirm())">
                <mat-icon>{{ hideConfirm() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (accountForm.controls.confirmPassword.touched &&
                   accountForm.controls.confirmPassword.hasError('required')) {
                <mat-error>Vui lòng nhập lại mật khẩu</mat-error>
              }
              @if (accountForm.hasError('passwordMismatch') &&
                   accountForm.controls.confirmPassword.touched) {
                <mat-error>Mật khẩu không khớp</mat-error>
              }
            </mat-form-field>

            <button mat-flat-button color="primary" type="button"
                    class="w-full h-12 text-base font-semibold mt-1"
                    [disabled]="accountForm.invalid || loading()"
                    (click)="submitRegister(stepper)">
              @if (loading()) {
                <mat-icon class="animate-spin mr-2">sync</mat-icon>
              }
              {{ loading() ? 'Đang xử lý...' : 'Tiếp theo' }}
            </button>

            <p class="text-center text-sm mt-2" style="color:#94A3B8">
              Đã có tài khoản?
              <a routerLink="/auth/login" class="font-semibold hover:underline" style="color:#F59E0B">
                Đăng nhập
              </a>
            </p>
          </form>
        </mat-step>

        <!-- Step 2: OTP verify -->
        <mat-step label="Xác thực OTP">
          <div class="flex flex-col gap-4 pt-2">
            <div class="rounded-xl p-4 text-sm flex gap-3"
                 style="background:rgba(245,158,11,0.08);border:1px solid rgba(245,158,11,0.2);color:#FDE68A">
              <mat-icon class="shrink-0 mt-0.5">mark_email_read</mat-icon>
              <span>
                Mã OTP 6 số đã được gửi đến
                <strong>{{ accountForm.controls.email.value }}</strong>.
                Kiểm tra hộp thư (và thư mục spam).
              </span>
            </div>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Mã OTP</mat-label>
              <input matInput [formControl]="otpControl" maxlength="6"
                     inputmode="numeric" autocomplete="one-time-code"
                     placeholder="______" />
              @if (otpControl.touched && otpControl.invalid) {
                <mat-error>Mã OTP gồm 6 chữ số</mat-error>
              }
            </mat-form-field>

            <button mat-flat-button color="primary" type="button"
                    class="w-full h-12 text-base font-semibold"
                    [disabled]="otpControl.invalid || loading()"
                    (click)="verifyOtp()">
              @if (loading()) {
                <mat-icon class="animate-spin mr-2">sync</mat-icon>
              }
              {{ loading() ? 'Đang xác thực...' : 'Xác nhận OTP' }}
            </button>

            <button mat-stroked-button type="button" class="w-full"
                    (click)="resendOtp()">
              <mat-icon>refresh</mat-icon>
              Gửi lại mã OTP
            </button>
          </div>
        </mat-step>
      </mat-stepper>
    </div>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly hidePassword = signal(true);
  readonly hideConfirm = signal(true);
  readonly loading = signal(false);

  readonly accountForm = this.fb.nonNullable.group(
    {
      username:        ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      fullName:        ['', Validators.maxLength(150)],
      email:           ['', [Validators.required, Validators.email]],
      phone:           ['', [Validators.required, Validators.pattern(/^(\+84|0)\d{9,10}$/)]],
      password:        ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordMatchValidator }
  );

  readonly otpControl = new FormControl('', [
    Validators.required,
    Validators.pattern(/^\d{6}$/),
  ]);

  submitRegister(stepper: any): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const { confirmPassword, ...payload } = this.accountForm.getRawValue();
    this.auth.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Đăng ký thành công! Kiểm tra email để lấy mã OTP.', 'Đóng', { duration: 4000 });
        stepper.next();
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Đăng ký thất bại';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }

  verifyOtp(): void {
    if (this.otpControl.invalid) return;
    this.loading.set(true);
    this.auth.verifyOtp({
      identifier: this.accountForm.controls.email.value,
      purpose: 'REGISTER',
      code: this.otpControl.value!,
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Xác thực thành công! Hãy đăng nhập.', 'Đóng', { duration: 3000 });
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Mã OTP không hợp lệ';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }

  resendOtp(): void {
    this.auth.resendOtp({
      identifier: this.accountForm.controls.email.value,
      purpose: 'REGISTER',
    }).subscribe({
      next: () => this.snackBar.open('OTP mới đã được gửi.', 'Đóng', { duration: 2000 }),
    });
  }
}
