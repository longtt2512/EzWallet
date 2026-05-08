import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule],
  template: `
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Số dư ví</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p class="text-3xl font-bold">0 đ</p>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>Giao dịch hôm nay</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p class="text-3xl font-bold">0</p>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>Hạn mức còn lại</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p class="text-3xl font-bold">10.000.000 đ</p>
        </mat-card-content>
      </mat-card>
    </div>

    <p class="mt-6 text-gray-500">
      Đây là trang tổng quan, các thành viên sẽ bổ sung biểu đồ + thông tin chi tiết.
    </p>
  `,
})
export class DashboardComponent {}
