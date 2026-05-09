import { Component, Inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export interface OtpDialogData {
  title?: string;
  message?: string;
  contact?: string;
}

@Component({
  selector: 'app-otp-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title ?? 'Xác thực OTP' }}</h2>
    <mat-dialog-content>
      <p class="text-sm text-gray-500 mb-4">
        {{ data.message ?? 'Nhập mã OTP 6 số được gửi đến' }}
        @if (data.contact) { <strong>{{ data.contact }}</strong> }
      </p>
      <mat-form-field class="w-full">
        <mat-label>Mã OTP</mat-label>
        <input matInput [formControl]="otpControl" maxlength="6"
               placeholder="______" inputmode="numeric" autocomplete="one-time-code">
        @if (otpControl.hasError('required')) {
          <mat-error>Vui lòng nhập mã OTP</mat-error>
        }
        @if (otpControl.hasError('pattern')) {
          <mat-error>Mã OTP gồm 6 chữ số</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Huỷ</button>
      <button mat-flat-button color="primary"
              [disabled]="otpControl.invalid || loading()"
              (click)="confirm()">
        @if (loading()) { <mat-spinner diameter="18" /> } @else { Xác nhận }
      </button>
    </mat-dialog-actions>
  `,
})
export class OtpDialogComponent {
  otpControl = new FormControl('', [
    Validators.required,
    Validators.pattern(/^\d{6}$/),
  ]);
  loading = signal(false);

  constructor(
    public dialogRef: MatDialogRef<OtpDialogComponent, string | null>,
    @Inject(MAT_DIALOG_DATA) public data: OtpDialogData,
  ) {}

  confirm(): void {
    if (this.otpControl.valid) {
      this.dialogRef.close(this.otpControl.value!);
    }
  }
}
