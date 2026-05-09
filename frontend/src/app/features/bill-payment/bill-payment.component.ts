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
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { BillService, BillProvider, BillInfo } from '../../core/services/bill.service';
import { TopupService } from '../../core/services/topup.service';
import { OtpDialogComponent } from '../../shared/components/otp-dialog/otp-dialog.component';
import { CurrencyVndPipe } from '../../shared/pipes/currency-vnd.pipe';
import { WalletInfo } from '../../core/models/wallet.model';

@Component({
  selector: 'app-bill-payment',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    CurrencyVndPipe,
    OtpDialogComponent,
  ],
  styles: [`
    .balance-card {
      background: linear-gradient(135deg, #1a1040 0%, #0f1f3d 100%);
      border: 1px solid rgba(245,158,11,0.25);
      border-radius: 20px;
      padding: 24px 28px;
      position: relative;
      overflow: hidden;
    }
    .balance-card::before {
      content: '';
      position: absolute;
      top: -30px; right: -30px;
      width: 160px; height: 160px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(245,158,11,0.10), transparent 70%);
      pointer-events: none;
    }
    .wallet-icon-wrap {
      width: 56px; height: 56px;
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.2);
      flex-shrink: 0;
    }

    .bill-card {
      background: #1E293B;
      border: 1px solid rgba(245,158,11,0.3);
      border-radius: 16px;
      padding: 20px;
    }

    .info-box {
      border-radius: 12px;
      padding: 14px 16px;
      font-size: 13px;
      line-height: 1.5;
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }
    .info-green {
      background: rgba(34,197,94,0.08);
      border: 1px solid rgba(34,197,94,0.18);
      color: #86EFAC;
    }

    .success-banner {
      width: 100%;
      height: 44px;
      background: #16A34A;
      border-radius: 9999px;
      color: #fff;
      font-weight: 600;
      font-size: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      animation: bannerIn 220ms cubic-bezier(.22,1,.36,1) forwards;
    }
    @keyframes bannerIn {
      from { opacity: 0; transform: translateY(6px) scale(0.98); }
      to   { opacity: 1; transform: translateY(0)   scale(1); }
    }

    :host ::ng-deep .mat-mdc-tab-body-wrapper { padding-top: 0; }
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
    :host ::ng-deep .mat-mdc-tab-labels { border-bottom: 1px solid #334155; }
    :host ::ng-deep .mdc-tab--active .mdc-tab__text-label { color: #F59E0B !important; }
    :host ::ng-deep .mat-mdc-tab-header .mdc-tab-indicator__content--underline { border-color: #F59E0B !important; }
    :host ::ng-deep .mat-mdc-select-value { color: #F8FAFC !important; }
    :host ::ng-deep .mat-mdc-option { color: #F8FAFC; }
  `],
  template: `
    <div class="max-w-2xl mx-auto">
      <h1 class="text-2xl font-bold mb-6" style="color:#F8FAFC">Thanh toán hoá đơn</h1>

      <!-- Balance card -->
      <div class="balance-card mb-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs font-medium uppercase tracking-widest mb-1" style="color:#94A3B8">Số dư ví</p>
            @if (wallet()) {
              <div class="flex items-center gap-1 mt-0.5">
                @if (balanceVisible()) {
                  <p class="text-3xl font-bold" style="color:#F59E0B">{{ wallet()!.balance | currencyVnd }}</p>
                } @else {
                  <p class="text-3xl font-bold" style="color:#F59E0B;letter-spacing:6px">••••••</p>
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
              <div class="h-9 w-44 rounded-lg animate-pulse mt-1" style="background:#334155"></div>
            }
          </div>
          <div class="wallet-icon-wrap">
            <mat-icon style="color:#F59E0B; font-size:28px; width:28px; height:28px; line-height:28px">
              account_balance_wallet
            </mat-icon>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <mat-tab-group animationDuration="200ms" (selectedTabChange)="onTabChange()">
        @for (tab of tabs; track tab.type) {
          <mat-tab [label]="tab.label">
            <div class="pt-5">
              <form [formGroup]="lookupForm" (ngSubmit)="lookup(tab.type)">

                <!-- Provider selector -->
                <mat-form-field appearance="outline" class="w-full mb-1">
                  <mat-label style="color:#94A3B8">Nhà cung cấp</mat-label>
                  <mat-select formControlName="providerCode">
                    @for (p of filteredProviders(tab.type); track p.id) {
                      <mat-option [value]="p.code">{{ p.name }}</mat-option>
                    }
                  </mat-select>
                  @if (lookupForm.get('providerCode')?.hasError('required') &&
                       lookupForm.get('providerCode')?.touched) {
                    <mat-error>Vui lòng chọn nhà cung cấp</mat-error>
                  }
                </mat-form-field>

                <!-- Customer code -->
                <mat-form-field appearance="outline" class="w-full mb-1">
                  <mat-label style="color:#94A3B8">Mã khách hàng / Số hợp đồng</mat-label>
                  <input matInput formControlName="customerCode"
                         placeholder="{{ tab.placeholder }}" autocomplete="off">
                  @if (lookupForm.get('customerCode')?.hasError('required') &&
                       lookupForm.get('customerCode')?.touched) {
                    <mat-error>Vui lòng nhập mã khách hàng</mat-error>
                  }
                </mat-form-field>

                <!-- Lookup hint -->
                <div class="info-box info-green mb-4">
                  <mat-icon class="!text-base shrink-0" style="color:#22C55E;margin-top:1px">info_outline</mat-icon>
                  <span>{{ tab.hint }}</span>
                </div>

                <button mat-flat-button type="submit" class="w-full h-11"
                        style="background:#F59E0B;color:#0F172A;font-weight:600;border-radius:9999px"
                        [disabled]="lookupForm.invalid || looking()">
                  @if (looking()) {
                    <mat-spinner diameter="18" style="display:inline-flex;margin-right:8px" />
                  }
                  Tra cứu hoá đơn
                </button>
              </form>

              <!-- Bill result -->
              @if (bill()) {
                <div class="bill-card mt-5">
                  <p class="text-xs font-semibold uppercase tracking-widest mb-4" style="color:#94A3B8">
                    Thông tin hoá đơn
                  </p>
                  <div class="space-y-3 mb-4">
                    <div class="flex justify-between text-sm">
                      <span style="color:#94A3B8">Nhà cung cấp</span>
                      <span class="font-medium" style="color:#F8FAFC">{{ bill()!.providerName }}</span>
                    </div>
                    <div class="flex justify-between text-sm">
                      <span style="color:#94A3B8">Mã khách hàng</span>
                      <span class="font-mono" style="color:#F8FAFC">{{ bill()!.customerCode }}</span>
                    </div>
                    <div class="flex justify-between text-sm">
                      <span style="color:#94A3B8">Hạn thanh toán</span>
                      <span style="color:#F59E0B">{{ bill()!.dueDate | date:'dd/MM/yyyy' }}</span>
                    </div>
                    <div style="height:1px;background:#334155;margin:4px 0"></div>
                    <div class="flex justify-between">
                      <span class="text-sm font-semibold" style="color:#F8FAFC">Số tiền</span>
                      <span class="text-xl font-bold" style="color:#F59E0B">
                        {{ bill()!.amount | currencyVnd }}
                      </span>
                    </div>
                  </div>

                  @if (paySuccess()) {
                    <div class="success-banner">
                      <mat-icon class="!text-base">check_circle</mat-icon>
                      {{ paySuccess() }}
                    </div>
                  } @else {
                    <button mat-flat-button class="w-full h-11"
                            style="background:#16A34A;color:#fff;font-weight:600;border-radius:9999px"
                            [disabled]="paying()"
                            (click)="payBill()">
                      @if (paying()) {
                        <mat-spinner diameter="18" style="display:inline-flex;margin-right:8px" />
                      }
                      Thanh toán {{ bill()!.amount | currencyVnd }}
                    </button>
                  }
                </div>
              }

              <!-- Error state -->
              @if (lookError()) {
                <div class="mt-4 p-4 rounded-xl text-sm" style="background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);color:#FCA5A5">
                  <mat-icon class="!text-base align-middle mr-1" style="color:#EF4444">error_outline</mat-icon>
                  {{ lookError() }}
                </div>
              }
            </div>
          </mat-tab>
        }
      </mat-tab-group>
    </div>
  `,
})
export class BillPaymentComponent implements OnInit {
  private billService = inject(BillService);
  private topupService = inject(TopupService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  wallet = signal<WalletInfo | null>(null);
  balanceVisible = signal(true);
  providers = signal<BillProvider[]>([]);
  bill = signal<BillInfo | null>(null);
  looking = signal(false);
  paying = signal(false);
  lookError = signal<string | null>(null);
  paySuccess = signal<string | null>(null);

  tabs = [
    {
      label: 'Điện',
      type: 'ELECTRICITY',
      placeholder: 'VD: PE00123456789',
      hint: 'Nhập mã đồng hồ điện (13 chữ số) in trên hoá đơn điện.',
    },
    {
      label: 'Nước',
      type: 'WATER',
      placeholder: 'VD: SW01234567',
      hint: 'Nhập mã thuê bao nước (10 ký tự) in trên hoá đơn nước.',
    },
    {
      label: 'Internet',
      type: 'INTERNET',
      placeholder: 'VD: VNPT12345678',
      hint: 'Nhập số hợp đồng internet (tiền tố nhà mạng + 8 chữ số).',
    },
    {
      label: 'Viễn thông',
      type: 'TELCO',
      placeholder: 'VD: 0901234567',
      hint: 'Nhập số điện thoại 10 chữ số đã đăng ký dịch vụ.',
    },
  ];

  lookupForm: FormGroup = this.fb.group({
    providerCode: ['', Validators.required],
    customerCode: ['', Validators.required],
  });

  ngOnInit(): void {
    this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
    this.billService.listProviders().subscribe({ next: p => this.providers.set(p), error: () => {} });
  }

  filteredProviders(type: string): BillProvider[] {
    return this.providers().filter(p => p.serviceType === type);
  }

  onTabChange(): void {
    this.bill.set(null);
    this.lookError.set(null);
    this.paySuccess.set(null);
    this.lookupForm.reset();
  }

  lookup(type: string): void {
    if (this.lookupForm.invalid) {
      this.lookupForm.markAllAsTouched();
      return;
    }
    const { providerCode, customerCode } = this.lookupForm.value;
    this.looking.set(true);
    this.bill.set(null);
    this.lookError.set(null);
    this.paySuccess.set(null);
    this.billService.lookupBill(providerCode, customerCode).subscribe({
      next: (b) => {
        this.looking.set(false);
        this.bill.set(b);
      },
      error: (err) => {
        this.looking.set(false);
        this.lookError.set(err?.error?.message ?? 'Không tìm thấy hoá đơn phù hợp');
      },
    });
  }

  payBill(): void {
    const b = this.bill();
    if (!b) return;

    this.paying.set(true);
    this.billService.requestBillOtp().subscribe({
      next: () => {
        this.paying.set(false);
        const ref = this.dialog.open(OtpDialogComponent, {
          width: '380px',
          data: {
            title: 'Xác thực thanh toán hoá đơn',
            message: `Mã OTP đã gửi đến email. Nhập mã để thanh toán ${new Intl.NumberFormat('vi-VN').format(b.amount)} ₫`,
          },
        });
        ref.afterClosed().subscribe((otpCode: string | null) => {
          if (!otpCode) return;
          this.paying.set(true);
          this.billService.payBill({ billCode: b.billCode, otpCode }).subscribe({
            next: () => {
              this.paying.set(false);
              this.paySuccess.set('Thanh toán thành công!');
              this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
              setTimeout(() => {
                this.paySuccess.set(null);
                this.bill.set(null);
                this.lookupForm.reset();
              }, 3000);
            },
            error: (err) => {
              this.paying.set(false);
              this.snackBar.open(err?.error?.message ?? 'Thanh toán thất bại', 'Đóng', {
                duration: 4000,
                panelClass: ['snack-error'],
              });
            },
          });
        });
      },
      error: (err) => {
        this.paying.set(false);
        this.snackBar.open(err?.error?.message ?? 'Không thể gửi OTP', 'Đóng', { duration: 4000 });
      },
    });
  }
}
