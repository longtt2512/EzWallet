import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { ErrorStateMatcher } from '@angular/material/core';

import {
  TopupService,
  BankAccount,
} from '../../core/services/topup.service';
import { OtpDialogComponent } from '../../shared/components/otp-dialog/otp-dialog.component';
import { CurrencyVndPipe } from '../../shared/pipes/currency-vnd.pipe';
import { VndInputDirective } from '../../shared/directives/vnd-input.directive';
import { VND_MIN_TOPUP, VND_MIN_WITHDRAW, vndMultiple } from '../../shared/validators/vnd-amount.validator';
import { WalletInfo } from '../../core/models/wallet.model';

class SubmittedOrDirtyMatcher implements ErrorStateMatcher {
  constructor(private submitted: () => boolean) {}
  isErrorState(control: AbstractControl | null, _form: FormGroupDirective | NgForm | null): boolean {
    return !!(control?.invalid && (this.submitted() || (control.dirty && control.touched)));
  }
}

@Component({
  selector: 'app-topup-withdraw',
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
    VndInputDirective,
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
      background: radial-gradient(circle, rgba(245,158,11,0.1), transparent 70%);
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
    .info-box {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 16px; border-radius: 12px;
      font-size: 13px; line-height: 1.5;
    }
    .info-icon {
      font-size: 18px !important;
      width: 18px !important; height: 18px !important;
      flex-shrink: 0;
    }
    .info-green {
      background: rgba(34,197,94,0.08);
      border: 1px solid rgba(34,197,94,0.15);
      color: #86EFAC;
    }
    .info-green .info-icon { color: #22C55E; }
    .info-amber {
      background: rgba(245,158,11,0.08);
      border: 1px solid rgba(245,158,11,0.15);
      color: #FDE68A;
    }
    .info-amber .info-icon { color: #F59E0B; }
    .fee-card {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 12px;
      padding: 14px 16px;
      margin-bottom: 16px;
    }
    .fee-row {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 13px; padding: 3px 0;
    }
    .fee-divider { height: 1px; background: #334155; margin: 8px 0; }
    .bank-card {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 14px;
      padding: 14px 16px;
      display: flex; align-items: center; gap: 12px;
      transition: border-color 150ms ease;
    }
    .bank-card:hover { border-color: #475569; }
    .bank-icon-wrap {
      width: 40px; height: 40px; border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(245,158,11,0.1); flex-shrink: 0;
    }
    .default-badge {
      font-size: 11px; font-weight: 600;
      padding: 3px 8px; border-radius: 20px;
      background: rgba(34,197,94,0.12); color: #22C55E;
    }
    .section-sep { height: 1px; background: #334155; }
    .success-banner {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      width: 100%; height: 44px;
      background: #16A34A;
      border-radius: 9999px;
      color: #ffffff;
      font-size: 14px; font-weight: 600;
      animation: bannerIn 220ms cubic-bezier(.22,1,.36,1) forwards;
    }
    @keyframes bannerIn {
      from { opacity: 0; transform: translateY(6px) scale(0.98); }
      to   { opacity: 1; transform: translateY(0)   scale(1);    }
    }
    .banner-icon {
      font-size: 20px !important;
      width: 20px !important; height: 20px !important;
    }
  `],
  template: `
    <div class="max-w-2xl mx-auto">

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

      <mat-tab-group animationDuration="200ms" [selectedIndex]="selectedTab()">

        <!-- ── Tab 1: Nạp tiền ──────────────────────────────────────── -->
        <mat-tab label="Nạp tiền">
          <div class="pt-5">
            <form [formGroup]="depositForm" (ngSubmit)="submitDeposit()">

              <mat-form-field class="w-full mb-1">
                <mat-label>Tài khoản ngân hàng</mat-label>
                <mat-select formControlName="bankAccountId" [errorStateMatcher]="depositMatcher">
                  @for (acc of bankAccounts(); track acc.id) {
                    <mat-option [value]="acc.id">
                      {{ acc.bankCode }} — {{ acc.maskedAccountNumber }}
                      @if (acc.isDefault) { (Mặc định) }
                    </mat-option>
                  }
                </mat-select>
                @if (showErr(depositForm, 'bankAccountId', 'required', depositSubmitted())) {
                  <mat-error>Vui lòng chọn tài khoản</mat-error>
                }
              </mat-form-field>

              <mat-form-field class="w-full mb-3">
                <mat-label>Số tiền nạp</mat-label>
                <input matInput vndInput formControlName="amount"
                       [errorStateMatcher]="depositMatcher"
                       placeholder="10.000">
                <span matSuffix style="color:#64748B; padding-right:4px">₫</span>
                @if (showErr(depositForm, 'amount', 'required', depositSubmitted())) {
                  <mat-error>Vui lòng nhập số tiền</mat-error>
                }
                @if (showErr(depositForm, 'amount', 'min', depositSubmitted())) {
                  <mat-error>Số tiền nạp tối thiểu là 10.000 ₫</mat-error>
                }
                @if (showErr(depositForm, 'amount', 'vndMultiple', depositSubmitted())) {
                  <mat-error>Số tiền phải là bội số của 1.000 ₫</mat-error>
                }
              </mat-form-field>

              <div class="info-box info-amber mb-5">
                <mat-icon class="info-icon">shield</mat-icon>
                <span>Nạp tiền yêu cầu xác thực OTP qua email. Không mất phí giao dịch.</span>
              </div>

              @if (depositSuccess()) {
                <div class="success-banner">
                  <mat-icon class="banner-icon">check_circle</mat-icon>
                  {{ depositSuccess() }}
                </div>
              } @else {
                <button mat-flat-button color="primary" type="submit"
                        class="w-full !h-11 text-base font-semibold"
                        [disabled]="depositing()">
                  @if (depositing()) {
                    <mat-spinner diameter="20" class="inline-block mr-2" />
                  } @else {
                    <mat-icon class="mr-1 !text-lg" style="vertical-align:middle">add_circle_outline</mat-icon>
                  }
                  Nạp tiền
                </button>
              }
            </form>
          </div>
        </mat-tab>

        <!-- ── Tab 2: Rút tiền ──────────────────────────────────────── -->
        <mat-tab label="Rút tiền">
          <div class="pt-5">
            <form [formGroup]="withdrawForm" (ngSubmit)="submitWithdraw()">

              <mat-form-field class="w-full mb-1">
                <mat-label>Tài khoản ngân hàng nhận</mat-label>
                <mat-select formControlName="bankAccountId" [errorStateMatcher]="withdrawMatcher">
                  @for (acc of bankAccounts(); track acc.id) {
                    <mat-option [value]="acc.id">
                      {{ acc.bankCode }} — {{ acc.maskedAccountNumber }}
                      @if (acc.isDefault) { (Mặc định) }
                    </mat-option>
                  }
                </mat-select>
                @if (showErr(withdrawForm, 'bankAccountId', 'required', withdrawSubmitted())) {
                  <mat-error>Vui lòng chọn tài khoản</mat-error>
                }
              </mat-form-field>

              <mat-form-field class="w-full mb-1">
                <mat-label>Số tiền rút</mat-label>
                <input matInput vndInput formControlName="amount"
                       [errorStateMatcher]="withdrawMatcher"
                       placeholder="50.000"
                       (input)="onWithdrawAmountChange()">
                <span matSuffix style="color:#64748B; padding-right:4px">₫</span>
                @if (showErr(withdrawForm, 'amount', 'required', withdrawSubmitted())) {
                  <mat-error>Vui lòng nhập số tiền</mat-error>
                }
                @if (showErr(withdrawForm, 'amount', 'min', withdrawSubmitted())) {
                  <mat-error>Số tiền rút tối thiểu là 50.000 ₫</mat-error>
                }
                @if (showErr(withdrawForm, 'amount', 'vndMultiple', withdrawSubmitted())) {
                  <mat-error>Số tiền phải là bội số của 1.000 ₫</mat-error>
                }
              </mat-form-field>

              @if (withdrawFeeInfo()) {
                <div class="fee-card">
                  <div class="fee-row">
                    <span style="color:#94A3B8">Số tiền rút</span>
                    <span style="color:#F8FAFC">{{ withdrawForm.value.amount | currencyVnd }}</span>
                  </div>
                  <div class="fee-row">
                    <span style="color:#94A3B8">Phí giao dịch</span>
                    <span style="color:#F59E0B">{{ withdrawFeeInfo()!.fee | currencyVnd }}</span>
                  </div>
                  <div class="fee-divider"></div>
                  <div class="fee-row">
                    <span class="font-semibold" style="color:#F8FAFC">Tổng khấu trừ</span>
                    <span class="font-bold" style="color:#EF4444">{{ withdrawFeeInfo()!.total | currencyVnd }}</span>
                  </div>
                </div>
              }

              <div class="info-box info-amber mb-5">
                <mat-icon class="info-icon">shield</mat-icon>
                <span>Rút tiền yêu cầu xác thực OTP qua email.</span>
              </div>

              @if (withdrawSuccess()) {
                <div class="success-banner">
                  <mat-icon class="banner-icon">check_circle</mat-icon>
                  {{ withdrawSuccess() }}
                </div>
              } @else {
                <button mat-flat-button color="primary" type="submit"
                        class="w-full !h-11 text-base font-semibold"
                        [disabled]="withdrawing()">
                  @if (withdrawing()) {
                    <mat-spinner diameter="20" class="inline-block mr-2" />
                  } @else {
                    <mat-icon class="mr-1 !text-lg" style="vertical-align:middle">remove_circle_outline</mat-icon>
                  }
                  Rút tiền
                </button>
              }
            </form>
          </div>
        </mat-tab>

        <!-- ── Tab 3: Tài khoản NH ──────────────────────────────────── -->
        <mat-tab label="Tài khoản NH">
          <div class="pt-5">
            @for (acc of bankAccounts(); track acc.id) {
              <div class="bank-card mb-3">
                <div class="bank-icon-wrap">
                  <mat-icon style="color:#F59E0B">account_balance</mat-icon>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-semibold text-sm" style="color:#F8FAFC">{{ acc.bankCode }}</p>
                  <p class="text-xs mt-0.5" style="color:#94A3B8">{{ acc.maskedAccountNumber }}</p>
                  <p class="text-xs" style="color:#64748B">{{ acc.accountHolder }}</p>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  @if (acc.isDefault) {
                    <span class="default-badge">Mặc định</span>
                  } @else {
                    <button mat-stroked-button class="!text-xs !h-8 !min-w-0 !px-2"
                            (click)="setDefault(acc.id)">Đặt mặc định</button>
                  }
                  <button mat-icon-button (click)="deleteAccount(acc.id)">
                    <mat-icon class="!text-lg" style="color:#EF4444">delete_outline</mat-icon>
                  </button>
                </div>
              </div>
            }

            @if (bankAccounts().length === 0) {
              <div class="flex flex-col items-center py-10 gap-2">
                <mat-icon class="!text-4xl opacity-20" style="color:#94A3B8">account_balance</mat-icon>
                <p class="text-sm" style="color:#64748B">Chưa có tài khoản ngân hàng nào được liên kết.</p>
              </div>
            }

            <div class="section-sep my-5"></div>
            <p class="text-xs font-semibold uppercase tracking-widest mb-4" style="color:#64748B">Liên kết tài khoản mới</p>

            <form [formGroup]="linkForm" (ngSubmit)="linkAccount()">
              <mat-form-field class="w-full mb-1">
                <mat-label>Mã ngân hàng (VD: VCB, TCB, MB)</mat-label>
                <input matInput formControlName="bankCode"
                       [errorStateMatcher]="linkMatcher" placeholder="VCB">
                @if (showErr(linkForm, 'bankCode', 'required', linkSubmitted())) {
                  <mat-error>Vui lòng nhập mã ngân hàng</mat-error>
                }
              </mat-form-field>
              <mat-form-field class="w-full mb-1">
                <mat-label>Số tài khoản</mat-label>
                <input matInput formControlName="accountNumber"
                       [errorStateMatcher]="linkMatcher">
                @if (showErr(linkForm, 'accountNumber', 'required', linkSubmitted())) {
                  <mat-error>Vui lòng nhập số tài khoản</mat-error>
                }
              </mat-form-field>
              <mat-form-field class="w-full mb-4">
                <mat-label>Tên chủ tài khoản</mat-label>
                <input matInput formControlName="accountHolder"
                       [errorStateMatcher]="linkMatcher">
                @if (showErr(linkForm, 'accountHolder', 'required', linkSubmitted())) {
                  <mat-error>Vui lòng nhập tên chủ tài khoản</mat-error>
                }
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit"
                      class="w-full !h-11 font-semibold"
                      [disabled]="linking()">
                @if (linking()) { <mat-spinner diameter="18" class="inline-block mr-1" /> }
                Liên kết tài khoản
              </button>
            </form>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
})
export class TopupWithdrawComponent implements OnInit {
  private topupService = inject(TopupService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);

  wallet = signal<WalletInfo | null>(null);
  balanceVisible = signal(true);
  bankAccounts = signal<BankAccount[]>([]);
  depositing = signal(false);
  withdrawing = signal(false);
  depositSuccess = signal<string | null>(null);
  withdrawSuccess = signal<string | null>(null);
  linking = signal(false);
  withdrawFeeInfo = signal<{ fee: number; total: number } | null>(null);
  selectedTab = signal(0);

  depositSubmitted = signal(false);
  withdrawSubmitted = signal(false);
  linkSubmitted = signal(false);


  depositMatcher = new SubmittedOrDirtyMatcher(() => this.depositSubmitted());
  withdrawMatcher = new SubmittedOrDirtyMatcher(() => this.withdrawSubmitted());
  linkMatcher = new SubmittedOrDirtyMatcher(() => this.linkSubmitted());

  depositForm: FormGroup = this.fb.group({
    bankAccountId: [null, Validators.required],
    amount: [null, [Validators.required, Validators.min(VND_MIN_TOPUP), vndMultiple]],
  });

  withdrawForm: FormGroup = this.fb.group({
    bankAccountId: [null, Validators.required],
    amount: [null, [Validators.required, Validators.min(VND_MIN_WITHDRAW), vndMultiple]],
  });

  linkForm: FormGroup = this.fb.group({
    bankCode: ['', Validators.required],
    accountNumber: ['', Validators.required],
    accountHolder: ['', Validators.required],
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const tab = parseInt(params['tab'] ?? '0', 10);
      this.selectedTab.set(isNaN(tab) ? 0 : tab);
    });
    this.loadWallet();
    this.loadBankAccounts();
  }

  showErr(form: FormGroup, field: string, error: string, submitted: boolean): boolean {
    const c = form.get(field);
    return !!(c?.hasError(error) && (submitted || (c.dirty && c.touched)));
  }

  onWithdrawAmountChange(): void {
    const amount = this.withdrawForm.value.amount;
    if (amount && amount >= VND_MIN_WITHDRAW) {
      this.withdrawFeeInfo.set({ fee: 5000, total: amount + 5000 });
    } else {
      this.withdrawFeeInfo.set(null);
    }
  }

  submitDeposit(): void {
    this.depositSubmitted.set(true);
    if (this.depositForm.invalid) return;
    const { bankAccountId, amount } = this.depositForm.value;

    this.depositing.set(true);
    this.topupService.requestDepositOtp().subscribe({
      next: () => {
        this.depositing.set(false);
        const ref = this.dialog.open(OtpDialogComponent, {
          width: '380px',
          data: {
            title: 'Xác thực nạp tiền',
            message: `Mã OTP đã gửi đến email. Nhập mã để nạp ${amount?.toLocaleString('vi-VN')} ₫`,
          },
        });
        ref.afterClosed().subscribe((otpCode: string | null) => {
          if (!otpCode) return;
          this.depositing.set(true);
          this.topupService.deposit({ bankAccountId, amount, otpCode }).subscribe({
            next: () => {
              this.depositing.set(false);
              this.depositForm.reset();
              this.depositSubmitted.set(false);
              this.loadWallet();
              this.depositSuccess.set('Nạp tiền thành công!');
              setTimeout(() => this.depositSuccess.set(null), 3000);
            },
            error: (err) => {
              this.depositing.set(false);
              const msg = err?.error?.message ?? 'Nạp tiền thất bại';
              this.snackBar.open(msg, 'Đóng', { duration: 4000 });
            },
          });
        });
      },
      error: (err) => {
        this.depositing.set(false);
        const msg = err?.error?.message ?? 'Không thể gửi OTP';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }

  submitWithdraw(): void {
    this.withdrawSubmitted.set(true);
    if (this.withdrawForm.invalid) return;
    const { bankAccountId, amount } = this.withdrawForm.value;

    this.withdrawing.set(true);
    this.topupService.requestWithdrawOtp().subscribe({
      next: () => {
        this.withdrawing.set(false);
        const ref = this.dialog.open(OtpDialogComponent, {
          width: '380px',
          data: {
            title: 'Xác thực rút tiền',
            message: `Mã OTP đã gửi đến email. Nhập mã để rút ${amount?.toLocaleString('vi-VN')} ₫`,
          },
        });

        ref.afterClosed().subscribe((otpCode: string | null) => {
          if (!otpCode) return;
          this.withdrawing.set(true);
          this.topupService.withdraw({ bankAccountId, amount, otpCode }).subscribe({
            next: () => {
              this.withdrawing.set(false);
              this.withdrawForm.reset();
              this.withdrawSubmitted.set(false);
              this.withdrawFeeInfo.set(null);
              this.loadWallet();
              this.withdrawSuccess.set('Rút tiền thành công!');
              setTimeout(() => this.withdrawSuccess.set(null), 3000);
            },
            error: (err) => {
              this.withdrawing.set(false);
              const msg = err?.error?.message ?? 'Rút tiền thất bại';
              this.snackBar.open(msg, 'Đóng', { duration: 4000 });
            },
          });
        });
      },
      error: (err) => {
        this.withdrawing.set(false);
        const msg = err?.error?.message ?? 'Không thể gửi OTP';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }

  linkAccount(): void {
    this.linkSubmitted.set(true);
    if (this.linkForm.invalid) return;
    this.linking.set(true);
    this.topupService.linkBankAccount(this.linkForm.value).subscribe({
      next: () => {
        this.linking.set(false);
        this.snackBar.open('Liên kết thành công!', 'Đóng', {
          duration: 3000,
          panelClass: ['snack-success'],
        });
        this.linkForm.reset();
        this.linkSubmitted.set(false);
        this.loadBankAccounts();
      },
      error: (err) => {
        this.linking.set(false);
        const msg = err?.error?.message ?? 'Liên kết thất bại';
        this.snackBar.open(msg, 'Đóng', { duration: 4000 });
      },
    });
  }

  setDefault(id: number): void {
    this.topupService.setDefaultBankAccount(id).subscribe({
      next: () => {
        this.loadBankAccounts();
        this.snackBar.open('Đã đặt tài khoản mặc định', 'Đóng', {
          duration: 2000,
          panelClass: ['snack-success'],
        });
      },
      error: () => this.snackBar.open('Không thể đặt mặc định', 'Đóng', { duration: 3000 }),
    });
  }

  deleteAccount(id: number): void {
    this.topupService.deleteBankAccount(id).subscribe({
      next: () => this.loadBankAccounts(),
      error: () => this.snackBar.open('Không thể xoá tài khoản', 'Đóng', { duration: 3000 }),
    });
  }

  private loadWallet(): void {
    this.topupService.getWallet().subscribe({
      next: (w) => this.wallet.set(w),
      error: () => {},
    });
  }

  private loadBankAccounts(): void {
    this.topupService.listBankAccounts().subscribe({
      next: (list) => this.bankAccounts.set(list),
      error: () => {},
    });
  }
}
