import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';

function passwordMatchValidator(g: AbstractControl): ValidationErrors | null {
  const pw = g.get('newPassword')?.value;
  const confirm = g.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-change-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="max-w-md mx-auto p-4">
      <h1 class="text-2xl font-bold mb-6">Đổi mật khẩu</h1>

      <mat-card>
        <mat-card-content class="pt-4">
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field class="w-full mb-4">
              <mat-label>Mật khẩu hiện tại</mat-label>
              <input matInput [type]="hideOld() ? 'password' : 'text'"
                     formControlName="currentPassword" autocomplete="current-password">
              <button mat-icon-button matSuffix type="button"
                      (click)="hideOld.set(!hideOld())">
                <mat-icon>{{ hideOld() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.get('currentPassword')?.hasError('required') &&
                   form.get('currentPassword')?.touched) {
                <mat-error>Vui lòng nhập mật khẩu hiện tại</mat-error>
              }
            </mat-form-field>

            <mat-form-field class="w-full mb-4">
              <mat-label>Mật khẩu mới</mat-label>
              <input matInput [type]="hideNew() ? 'password' : 'text'"
                     formControlName="newPassword" autocomplete="new-password">
              <button mat-icon-button matSuffix type="button"
                      (click)="hideNew.set(!hideNew())">
                <mat-icon>{{ hideNew() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.get('newPassword')?.hasError('required') &&
                   form.get('newPassword')?.touched) {
                <mat-error>Vui lòng nhập mật khẩu mới</mat-error>
              }
              @if (form.get('newPassword')?.hasError('minlength') &&
                   form.get('newPassword')?.touched) {
                <mat-error>Mật khẩu mới tối thiểu 8 ký tự</mat-error>
              }
            </mat-form-field>

            <mat-form-field class="w-full mb-4">
              <mat-label>Xác nhận mật khẩu mới</mat-label>
              <input matInput [type]="hideConfirm() ? 'password' : 'text'"
                     formControlName="confirmPassword" autocomplete="new-password">
              <button mat-icon-button matSuffix type="button"
                      (click)="hideConfirm.set(!hideConfirm())">
                <mat-icon>{{ hideConfirm() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.hasError('passwordMismatch') &&
                   form.get('confirmPassword')?.touched) {
                <mat-error>Mật khẩu xác nhận không khớp</mat-error>
              }
            </mat-form-field>

            <button mat-flat-button color="primary" type="submit"
                    class="w-full" [disabled]="form.invalid || loading()">
              @if (loading()) {
                <mat-spinner diameter="20" class="inline-block mr-2" />
              }
              Đổi mật khẩu
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class ChangePasswordComponent {
  private auth = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  loading = signal(false);
  hideOld = signal(true);
  hideNew = signal(true);
  hideConfirm = signal(true);

  form: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  submit(): void {
    if (this.form.invalid) return;
    const { currentPassword, newPassword, confirmPassword } = this.form.value;
    this.loading.set(true);
    this.auth.changePassword(currentPassword, newPassword, confirmPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Đổi mật khẩu thành công!', 'Đóng', { duration: 3000 });
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(err?.error?.message ?? 'Đổi mật khẩu thất bại', 'Đóng', { duration: 4000 });
      },
    });
  }
}
