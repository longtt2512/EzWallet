import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import {
  TransactionHistoryService,
  PageResponse,
} from '../../core/services/transaction.service';
import { CurrencyVndPipe } from '../../shared/pipes/currency-vnd.pipe';
import { TransactionResponse, TransactionType, TransactionStatus } from '../../core/models/transaction.model';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatPaginatorModule,
    CurrencyVndPipe,
  ],
  styles: [`
    .filter-panel {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 16px;
      padding: 20px;
    }

    .tx-row {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 12px;
      padding: 14px 16px;
      cursor: pointer;
      transition: border-color 150ms ease;
    }
    .tx-row:hover { border-color: #475569; }

    .tx-icon-wrap {
      width: 40px; height: 40px;
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }

    .status-badge {
      font-size: 11px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 9999px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      padding: 4px 0;
    }

    :host ::ng-deep .mat-mdc-form-field .mdc-text-field { background: rgba(30,41,59,0.6) !important; }
    :host ::ng-deep .mat-mdc-form-field .mdc-text-field--outlined .mdc-notched-outline__leading,
    :host ::ng-deep .mat-mdc-form-field .mdc-text-field--outlined .mdc-notched-outline__notch,
    :host ::ng-deep .mat-mdc-form-field .mdc-text-field--outlined .mdc-notched-outline__trailing {
      border-color: #334155 !important;
    }
    :host ::ng-deep .mat-mdc-form-field.mat-focused .mdc-notched-outline__leading,
    :host ::ng-deep .mat-mdc-form-field.mat-focused .mdc-notched-outline__notch,
    :host ::ng-deep .mat-mdc-form-field.mat-focused .mdc-notched-outline__trailing {
      border-color: #F59E0B !important;
    }
    :host ::ng-deep .mat-mdc-select-value { color: #F8FAFC !important; }
    :host ::ng-deep .mat-mdc-paginator { background: transparent; color: #94A3B8; }
    :host ::ng-deep .mat-mdc-paginator .mat-mdc-icon-button { color: #94A3B8; }
    :host ::ng-deep .mat-mdc-paginator .mat-mdc-select-value { color: #94A3B8 !important; font-size: 13px; }
  `],
  template: `
    <div class="max-w-3xl mx-auto">
      <h1 class="text-2xl font-bold mb-6" style="color:#F8FAFC">Lịch sử giao dịch</h1>

      <!-- Filter panel -->
      <div class="filter-panel mb-5">
        <p class="text-xs font-semibold uppercase tracking-widest mb-4" style="color:#64748B">Bộ lọc</p>
        <form [formGroup]="filterForm" (ngSubmit)="applyFilter()" class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label style="color:#94A3B8">Loại giao dịch</mat-label>
            <mat-select formControlName="type">
              <mat-option value="">Tất cả</mat-option>
              <mat-option value="TOPUP">Nạp tiền</mat-option>
              <mat-option value="WITHDRAW">Rút tiền</mat-option>
              <mat-option value="TRANSFER">Chuyển tiền</mat-option>
              <mat-option value="BILL_PAYMENT">Hoá đơn</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label style="color:#94A3B8">Trạng thái</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Tất cả</mat-option>
              <mat-option value="COMPLETED">Thành công</mat-option>
              <mat-option value="PENDING">Đang xử lý</mat-option>
              <mat-option value="FAILED">Thất bại</mat-option>
              <mat-option value="CANCELLED">Đã huỷ</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label style="color:#94A3B8">Từ ngày</mat-label>
            <input matInput type="date" formControlName="dateFrom">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label style="color:#94A3B8">Đến ngày</mat-label>
            <input matInput type="date" formControlName="dateTo">
          </mat-form-field>

          <div class="col-span-2 flex gap-2">
            <button mat-flat-button type="submit" class="h-10"
                    style="background:#F59E0B;color:#0F172A;font-weight:600;border-radius:8px;flex:1">
              <mat-icon class="!text-base mr-1">filter_list</mat-icon>
              Lọc
            </button>
            <button mat-stroked-button type="button" class="h-10"
                    style="border-color:#334155;color:#94A3B8;border-radius:8px"
                    (click)="resetFilter()">
              Xoá
            </button>
          </div>
        </form>
      </div>

      <!-- Loading skeleton -->
      @if (loading()) {
        @for (i of [1,2,3,4,5]; track i) {
          <div class="tx-row mb-2">
            <div class="tx-icon-wrap animate-pulse" style="background:#334155"></div>
            <div class="flex-1 ml-3">
              <div class="h-3 w-28 rounded animate-pulse mb-2" style="background:#334155"></div>
              <div class="h-2 w-20 rounded animate-pulse" style="background:#293548"></div>
            </div>
            <div class="h-3 w-24 rounded animate-pulse" style="background:#334155"></div>
          </div>
        }
      }

      <!-- Empty state -->
      @if (!loading() && (page()?.totalElements ?? 0) === 0) {
        <div class="flex flex-col items-center py-16 gap-3">
          <mat-icon class="!text-5xl opacity-20" style="color:#F59E0B">receipt_long</mat-icon>
          <p class="text-sm" style="color:#64748B">Không có giao dịch nào phù hợp.</p>
        </div>
      }

      <!-- Transaction list -->
      @if (!loading()) {
        @for (tx of page()?.content ?? []; track tx.id) {
          <div class="tx-row mb-2" (click)="toggleDetail(tx)">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="tx-icon-wrap" [style]="txIconBg(tx.type)">
                  <mat-icon class="!text-lg" [style]="txIconColor(tx.type)">{{ txIcon(tx.type) }}</mat-icon>
                </div>
                <div>
                  <p class="text-sm font-medium" style="color:#F8FAFC">{{ typeLabel(tx.type) }}</p>
                  <p class="text-xs" style="color:#64748B">
                    {{ tx.createdAt | date:'HH:mm · dd/MM/yyyy' }}
                  </p>
                </div>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold mb-1"
                   [style]="tx.status === 'FAILED'
                     ? 'color:#94A3B8;text-decoration:line-through'
                     : isCredit(tx) ? 'color:#22C55E' : 'color:#EF4444'">
                  {{ isCredit(tx) ? '+' : '-' }}{{ tx.amount | currencyVnd }}
                </p>
                <span class="status-badge" [style]="statusStyle(tx.status)">
                  {{ statusLabel(tx.status) }}
                </span>
              </div>
            </div>

            <!-- Expanded detail -->
            @if (expandedTxId() === tx.id) {
              <div style="height:1px;background:#334155;margin:12px 0"></div>
              <div class="space-y-1">
                <div class="detail-row">
                  <span style="color:#64748B">Mã giao dịch</span>
                  <span class="font-mono text-xs" style="color:#F8FAFC">{{ tx.transactionRef }}</span>
                </div>
                @if (tx.fee) {
                  <div class="detail-row">
                    <span style="color:#64748B">Phí giao dịch</span>
                    <span style="color:#F59E0B">{{ tx.fee | currencyVnd }}</span>
                  </div>
                }
                @if (tx.description) {
                  <div class="detail-row">
                    <span style="color:#64748B">Nội dung</span>
                    <span style="color:#CBD5E1">{{ tx.description }}</span>
                  </div>
                }
                @if (tx.failureReason) {
                  <div class="detail-row">
                    <span style="color:#64748B">Lý do thất bại</span>
                    <span style="color:#FCA5A5">{{ tx.failureReason }}</span>
                  </div>
                }
                @if (balanceBefore(tx) != null) {
                  <div class="detail-row">
                    <span style="color:#64748B">Số dư thay đổi</span>
                    <span class="font-medium" style="color:#CBD5E1">
                      {{ balanceBefore(tx) | currencyVnd }}
                      <span style="color:#475569"> → </span>
                      <span [style]="isCredit(tx) ? 'color:#22C55E' : 'color:#EF4444'">
                        {{ balanceAfter(tx) | currencyVnd }}
                      </span>
                    </span>
                  </div>
                }
              </div>
            }
          </div>
        }
      }

      <!-- Paginator -->
      @if (!loading() && (page()?.totalElements ?? 0) > 0) {
        <mat-paginator
          [length]="page()!.totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 20, 50]"
          [pageIndex]="currentPage"
          (page)="onPageChange($event)"
          showFirstLastButtons />
      }
    </div>
  `,
})
export class TransactionHistoryComponent implements OnInit {
  private txService = inject(TransactionHistoryService);
  private fb = inject(FormBuilder);

  loading = signal(false);
  page = signal<PageResponse<TransactionResponse> | null>(null);
  expandedTxId = signal<number | null>(null);

  currentPage = 0;
  pageSize = 10;

  filterForm: FormGroup = this.fb.group({
    type: [''],
    status: [''],
    dateFrom: [''],
    dateTo: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  applyFilter(): void {
    this.currentPage = 0;
    this.load();
  }

  resetFilter(): void {
    this.filterForm.reset({ type: '', status: '', dateFrom: '', dateTo: '' });
    this.currentPage = 0;
    this.load();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  toggleDetail(tx: TransactionResponse): void {
    this.expandedTxId.set(this.expandedTxId() === tx.id ? null : tx.id);
  }

  private load(): void {
    this.loading.set(true);
    const { type, status, dateFrom, dateTo } = this.filterForm.value;
    this.txService.list({
      type: type || undefined,
      status: status || undefined,
      dateFrom: dateFrom ? new Date(dateFrom).toISOString() : undefined,
      dateTo: dateTo ? new Date(dateTo).toISOString() : undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: (p) => { this.loading.set(false); this.page.set(p); },
      error: () => this.loading.set(false),
    });
  }

  txIcon(type: TransactionType): string {
    return ({
      TOPUP: 'add_circle_outline',
      WITHDRAW: 'remove_circle_outline',
      TRANSFER: 'send',
      BILL_PAYMENT: 'receipt_long',
    } as any)[type] ?? 'swap_horiz';
  }

  txIconBg(type: TransactionType): string {
    return ({
      TOPUP:        'background:rgba(34,197,94,0.12)',
      WITHDRAW:     'background:rgba(239,68,68,0.12)',
      TRANSFER:     'background:rgba(139,92,246,0.12)',
      BILL_PAYMENT: 'background:rgba(245,158,11,0.12)',
    } as any)[type] ?? 'background:rgba(100,116,139,0.12)';
  }

  txIconColor(type: TransactionType): string {
    return ({
      TOPUP:        'color:#22C55E',
      WITHDRAW:     'color:#EF4444',
      TRANSFER:     'color:#8B5CF6',
      BILL_PAYMENT: 'color:#F59E0B',
    } as any)[type] ?? 'color:#94A3B8';
  }

  typeLabel(type: TransactionType): string {
    return ({
      TOPUP: 'Nạp tiền',
      WITHDRAW: 'Rút tiền',
      TRANSFER: 'Chuyển tiền',
      BILL_PAYMENT: 'Thanh toán hoá đơn',
    } as any)[type] ?? type;
  }

  statusLabel(status: TransactionStatus): string {
    return ({
      COMPLETED: 'Thành công',
      PENDING: 'Đang xử lý',
      FAILED: 'Thất bại',
      CANCELLED: 'Đã huỷ',
    } as any)[status] ?? status;
  }

  statusStyle(status: TransactionStatus): string {
    return ({
      COMPLETED: 'background:rgba(34,197,94,0.12);color:#22C55E',
      PENDING:   'background:rgba(245,158,11,0.12);color:#F59E0B',
      FAILED:    'background:rgba(239,68,68,0.12);color:#EF4444',
      CANCELLED: 'background:rgba(100,116,139,0.12);color:#94A3B8',
    } as any)[status] ?? '';
  }

  isCredit(tx: TransactionResponse): boolean {
    if (tx.direction) return tx.direction === 'CREDIT';
    return tx.type === 'TOPUP';
  }

  balanceBefore(tx: TransactionResponse): number | null {
    return this.isCredit(tx)
      ? (tx.targetBalanceBefore ?? null)
      : (tx.sourceBalanceBefore ?? null);
  }

  balanceAfter(tx: TransactionResponse): number | null {
    return this.isCredit(tx)
      ? (tx.targetBalanceAfter ?? null)
      : (tx.sourceBalanceAfter ?? null);
  }
}
