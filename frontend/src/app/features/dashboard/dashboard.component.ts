import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRippleModule } from '@angular/material/core';

import { TopupService } from '../../core/services/topup.service';
import { TransactionHistoryService } from '../../core/services/transaction.service';
import { CurrencyVndPipe } from '../../shared/pipes/currency-vnd.pipe';
import { WalletInfo } from '../../core/models/wallet.model';
import { TransactionResponse } from '../../core/models/transaction.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatRippleModule,
    CurrencyVndPipe,
  ],
  styles: [`
    .stat-card {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 16px;
      padding: 20px 24px;
      transition: border-color 150ms ease;
    }
    .stat-card:hover { border-color: #475569; }

    .balance-card {
      background: linear-gradient(135deg, #1a1040 0%, #0f1f3d 100%);
      border: 1px solid rgba(245,158,11,0.25);
      border-radius: 20px;
      padding: 28px;
      position: relative;
      overflow: hidden;
    }
    .balance-card::before {
      content: '';
      position: absolute;
      top: -40px; right: -40px;
      width: 180px; height: 180px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(245,158,11,0.12), transparent 70%);
      pointer-events: none;
    }
    .wallet-icon-wrap {
      width: 56px; height: 56px;
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.2);
    }

    .quick-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 16px 12px;
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 14px;
      text-decoration: none;
      cursor: pointer;
      transition: all 150ms ease;
    }
    .quick-btn:hover {
      border-color: rgba(245,158,11,0.4);
      background: rgba(245,158,11,0.06);
    }
    .quick-btn .icon-wrap {
      width: 44px; height: 44px;
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
    }

    .tx-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 16px;
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 12px;
      transition: border-color 150ms ease;
    }
    .tx-row:hover { border-color: #475569; }
    .tx-icon-wrap {
      width: 40px; height: 40px; border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
  `],
  template: `
    <div class="max-w-4xl mx-auto">

      <!-- Balance card -->
      <div class="balance-card mb-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs font-medium uppercase tracking-widest mb-2" style="color:#94A3B8">Số dư ví</p>
            @if (wallet()) {
              <div class="flex items-center gap-1 mt-0.5">
                @if (balanceVisible()) {
                  <p class="text-4xl font-bold" style="color:#F59E0B">{{ wallet()!.balance | currencyVnd }}</p>
                } @else {
                  <p class="text-4xl font-bold" style="color:#F59E0B;letter-spacing:6px">••••••</p>
                }
                <button type="button" (click)="balanceVisible.set(!balanceVisible())"
                        aria-label="Ẩn/hiện số dư"
                        style="background:none;border:none;padding:2px;cursor:pointer;display:flex;align-items:center;flex-shrink:0;outline:none">
                  <mat-icon style="font-size:18px;width:18px;height:18px;color:#64748B;transition:color 150ms">
                    {{ balanceVisible() ? 'visibility' : 'visibility_off' }}
                  </mat-icon>
                </button>
              </div>
              <span class="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium mt-1"
                    [style]="wallet()!.status === 'ACTIVE'
                      ? 'background:rgba(34,197,94,0.12);color:#22C55E'
                      : 'background:rgba(239,68,68,0.12);color:#EF4444'">
                <span class="w-1.5 h-1.5 rounded-full inline-block"
                      [style]="wallet()!.status === 'ACTIVE' ? 'background:#22C55E' : 'background:#EF4444'"></span>
                {{ wallet()!.status === 'ACTIVE' ? 'Đang hoạt động' : wallet()!.status }}
              </span>
            } @else {
              <div class="h-10 w-48 rounded-lg animate-pulse" style="background:#334155"></div>
            }
          </div>
          <div class="wallet-icon-wrap">
            <mat-icon style="color:#F59E0B;font-size:28px;width:28px;height:28px;line-height:28px">
              account_balance_wallet
            </mat-icon>
          </div>
        </div>
      </div>

      <!-- Stats row -->
      <div class="grid grid-cols-2 md:grid-cols-3 gap-3 mb-6">
        <div class="stat-card">
          <p class="text-xs mb-1" style="color:#94A3B8">Tổng giao dịch</p>
          <p class="text-2xl font-bold" style="color:#F8FAFC">{{ recentTxCount() }}</p>
        </div>
        <div class="stat-card">
          <p class="text-xs mb-1" style="color:#94A3B8">Loại tài khoản</p>
          <p class="text-2xl font-bold" style="color:#F59E0B">BRONZE</p>
        </div>
        <div class="stat-card col-span-2 md:col-span-1">
          <p class="text-xs mb-1" style="color:#94A3B8">Tiền tệ</p>
          <p class="text-2xl font-bold" style="color:#F8FAFC">VND</p>
        </div>
      </div>

      <!-- Quick actions -->
      <p class="text-xs font-semibold uppercase tracking-widest mb-3" style="color:#64748B">Thao tác nhanh</p>
      <div class="grid grid-cols-4 gap-3 mb-8">
        <a routerLink="/topup-withdraw" class="quick-btn">
          <div class="icon-wrap" style="background:rgba(34,197,94,0.12)">
            <mat-icon class="!text-xl" style="color:#22C55E">add_circle_outline</mat-icon>
          </div>
          <span class="text-xs font-medium" style="color:#CBD5E1">Nạp tiền</span>
        </a>
        <a routerLink="/topup-withdraw" [queryParams]="{tab: 1}" class="quick-btn">
          <div class="icon-wrap" style="background:rgba(239,68,68,0.12)">
            <mat-icon class="!text-xl" style="color:#EF4444">remove_circle_outline</mat-icon>
          </div>
          <span class="text-xs font-medium" style="color:#CBD5E1">Rút tiền</span>
        </a>
        <a routerLink="/transfer" class="quick-btn">
          <div class="icon-wrap" style="background:rgba(139,92,246,0.12)">
            <mat-icon class="!text-xl" style="color:#8B5CF6">send</mat-icon>
          </div>
          <span class="text-xs font-medium" style="color:#CBD5E1">Chuyển tiền</span>
        </a>
        <a routerLink="/bills" class="quick-btn">
          <div class="icon-wrap" style="background:rgba(245,158,11,0.12)">
            <mat-icon class="!text-xl" style="color:#F59E0B">receipt_long</mat-icon>
          </div>
          <span class="text-xs font-medium" style="color:#CBD5E1">Hoá đơn</span>
        </a>
      </div>

      <!-- Recent transactions -->
      <div class="flex items-center justify-between mb-3">
        <p class="text-xs font-semibold uppercase tracking-widest" style="color:#64748B">Giao dịch gần đây</p>
        <a routerLink="/history" class="text-xs font-medium" style="color:#F59E0B">Xem tất cả →</a>
      </div>

      @if (loadingTxs()) {
        @for (i of [1,2,3]; track i) {
          <div class="tx-row mb-2">
            <div class="tx-icon-wrap animate-pulse" style="background:#334155; width:40px; height:40px"></div>
            <div class="flex-1">
              <div class="h-3 w-24 rounded animate-pulse mb-2" style="background:#334155"></div>
              <div class="h-2 w-16 rounded animate-pulse" style="background:#293548"></div>
            </div>
            <div class="h-3 w-20 rounded animate-pulse" style="background:#334155"></div>
          </div>
        }
      } @else if (recentTxs().length === 0) {
        <div class="flex flex-col items-center py-12 gap-3">
          <mat-icon class="!text-5xl opacity-20" style="color:#F59E0B">receipt_long</mat-icon>
          <p class="text-sm" style="color:#64748B">Chưa có giao dịch nào</p>
        </div>
      } @else {
        @for (tx of recentTxs(); track tx.id) {
          <div class="tx-row mb-2">
            <div class="tx-icon-wrap" [style]="txIconBg(tx.type)">
              <mat-icon class="!text-lg" [style]="txIconColor(tx.type)">{{ txIcon(tx.type) }}</mat-icon>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium truncate" style="color:#F8FAFC">{{ txLabel(tx.type) }}</p>
              <p class="text-xs" style="color:#64748B">{{ tx.createdAt | date:'HH:mm · dd/MM/yyyy' }}</p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-sm font-semibold"
                 [style]="isCredit(tx) ? 'color:#22C55E' : 'color:#EF4444'">
                {{ isCredit(tx) ? '+' : '-' }}{{ tx.amount | currencyVnd }}
              </p>
              <span class="text-xs px-1.5 py-0.5 rounded font-medium"
                    [style]="tx.status === 'COMPLETED'
                      ? 'background:rgba(34,197,94,0.1);color:#22C55E'
                      : 'background:rgba(100,116,139,0.15);color:#94A3B8'">
                {{ tx.status === 'COMPLETED' ? 'Thành công' : tx.status }}
              </span>
            </div>
          </div>
        }
      }
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private topupService = inject(TopupService);
  private txService = inject(TransactionHistoryService);

  wallet = signal<WalletInfo | null>(null);
  balanceVisible = signal(true);
  recentTxs = signal<TransactionResponse[]>([]);
  recentTxCount = signal(0);
  loadingTxs = signal(true);

  ngOnInit(): void {
    this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
    this.txService.list({ page: 0, size: 5 }).subscribe({
      next: p => {
        this.recentTxs.set(p.content);
        this.recentTxCount.set(p.totalElements);
        this.loadingTxs.set(false);
      },
      error: () => this.loadingTxs.set(false),
    });
  }

  txIcon(type: string): string {
    return ({ TOPUP: 'add_circle_outline', WITHDRAW: 'remove_circle_outline',
              TRANSFER: 'send', BILL_PAYMENT: 'receipt_long' } as any)[type] ?? 'swap_horiz';
  }

  txIconBg(type: string): string {
    return ({
      TOPUP:        'background:rgba(34,197,94,0.12)',
      WITHDRAW:     'background:rgba(239,68,68,0.12)',
      TRANSFER:     'background:rgba(139,92,246,0.12)',
      BILL_PAYMENT: 'background:rgba(245,158,11,0.12)',
    } as any)[type] ?? 'background:rgba(100,116,139,0.12)';
  }

  txIconColor(type: string): string {
    return ({
      TOPUP:        'color:#22C55E',
      WITHDRAW:     'color:#EF4444',
      TRANSFER:     'color:#8B5CF6',
      BILL_PAYMENT: 'color:#F59E0B',
    } as any)[type] ?? 'color:#94A3B8';
  }

  txLabel(type: string): string {
    return ({ TOPUP: 'Nạp tiền', WITHDRAW: 'Rút tiền',
              TRANSFER: 'Chuyển tiền', BILL_PAYMENT: 'Hoá đơn' } as any)[type] ?? type;
  }

  isCredit(tx: TransactionResponse): boolean {
    if (tx.direction) return tx.direction === 'CREDIT';
    return tx.type === 'TOPUP';
  }
}
