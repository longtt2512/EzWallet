import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CurrencyVndPipe } from '../../pipes/currency-vnd.pipe';

export interface ConfirmDialogData {
  title: string;
  message: string;
  amount?: number;
  fee?: number;
  confirmLabel?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, CurrencyVndPipe],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="mb-3">{{ data.message }}</p>
      @if (data.amount != null) {
        <div class="rounded bg-gray-50 p-3 space-y-1 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-500">Số tiền</span>
            <span class="font-medium">{{ data.amount | currencyVnd }}</span>
          </div>
          @if (data.fee != null) {
            <div class="flex justify-between">
              <span class="text-gray-500">Phí</span>
              <span>{{ data.fee | currencyVnd }}</span>
            </div>
            <div class="flex justify-between border-t pt-1 font-semibold">
              <span>Tổng</span>
              <span>{{ (data.amount + (data.fee ?? 0)) | currencyVnd }}</span>
            </div>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Huỷ</button>
      <button mat-flat-button color="primary" [mat-dialog-close]="true">
        {{ data.confirmLabel ?? 'Xác nhận' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData) {}
}
