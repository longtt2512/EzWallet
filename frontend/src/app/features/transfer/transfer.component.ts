import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
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
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ErrorStateMatcher } from '@angular/material/core';

import { TransferService, QrInfo } from '../../core/services/transfer.service';
import { TopupService } from '../../core/services/topup.service';
import { OtpDialogComponent } from '../../shared/components/otp-dialog/otp-dialog.component';
import { CurrencyVndPipe } from '../../shared/pipes/currency-vnd.pipe';
import { VndInputDirective } from '../../shared/directives/vnd-input.directive';
import { VND_MIN_TRANSFER, vndMultiple } from '../../shared/validators/vnd-amount.validator';
import { WalletInfo } from '../../core/models/wallet.model';

class SubmittedOrDirtyMatcher implements ErrorStateMatcher {
  constructor(private submitted: () => boolean) {}
  isErrorState(control: AbstractControl | null, _form: FormGroupDirective | NgForm | null): boolean {
    return !!(control?.invalid && (this.submitted() || (control.dirty && control.touched)));
  }
}

@Component({
  selector: 'app-transfer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
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
    .info-amber {
      background: rgba(245,158,11,0.08);
      border: 1px solid rgba(245,158,11,0.15);
      color: #FDE68A;
    }
    .info-amber .info-icon { color: #F59E0B; }
    .qr-card {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 16px;
      padding: 20px;
      text-align: center;
      margin-top: 20px;
    }
    .qr-info-row {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 13px; padding: 6px 0;
    }
    .qr-info-sep { height: 1px; background: #334155; margin: 8px 0; }
    .qr-lookup-card {
      background: #1E293B;
      border: 1px solid #334155;
      border-radius: 12px;
      padding: 14px 16px;
      margin-bottom: 16px;
    }
    .qr-upload-zone {
      border: 2px dashed #334155;
      border-radius: 16px;
      padding: 32px 16px;
      text-align: center;
      cursor: pointer;
      transition: border-color 150ms ease, background 150ms ease;
    }
    .qr-upload-zone:hover, .qr-upload-zone.drag-over {
      border-color: rgba(245,158,11,0.5);
      background: rgba(245,158,11,0.04);
    }
    .qr-save-btn {
      background: rgba(34,197,94,0.1);
      border: 1px solid rgba(34,197,94,0.25);
      border-radius: 10px;
      color: #22C55E;
      padding: 8px 16px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: background 150ms ease;
    }
    .qr-save-btn:hover { background: rgba(34,197,94,0.18); }
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

      <mat-tab-group animationDuration="200ms">

        <!-- ── Tab 1: Chuyển khoản P2P ───────────────────────────────── -->
        <mat-tab label="Chuyển khoản">
          <div class="pt-5">
            <form [formGroup]="p2pForm" (ngSubmit)="submitP2P()">

              <mat-form-field class="w-full mb-1">
                <mat-label>Người nhận (username / email / SĐT)</mat-label>
                <input matInput formControlName="receiverIdentifier"
                       [errorStateMatcher]="p2pMatcher"
                       placeholder="username, email hoặc số điện thoại">
                @if (showErr(p2pForm, 'receiverIdentifier', 'required', p2pSubmitted())) {
                  <mat-error>Vui lòng nhập thông tin người nhận</mat-error>
                }
              </mat-form-field>

              <mat-form-field class="w-full mb-3">
                <mat-label>Số tiền chuyển</mat-label>
                <input matInput vndInput formControlName="amount"
                       [errorStateMatcher]="p2pMatcher"
                       placeholder="10.000">
                <span matSuffix style="color:#64748B; padding-right:4px">₫</span>
                @if (showErr(p2pForm, 'amount', 'required', p2pSubmitted())) {
                  <mat-error>Vui lòng nhập số tiền</mat-error>
                }
                @if (showErr(p2pForm, 'amount', 'min', p2pSubmitted())) {
                  <mat-error>Số tiền chuyển tối thiểu là 10.000 ₫</mat-error>
                }
                @if (showErr(p2pForm, 'amount', 'vndMultiple', p2pSubmitted())) {
                  <mat-error>Số tiền phải là bội số của 1.000 ₫</mat-error>
                }
              </mat-form-field>

              <mat-form-field class="w-full mb-3">
                <mat-label>Nội dung chuyển khoản (tuỳ chọn)</mat-label>
                <input matInput formControlName="note" placeholder="Ví dụ: Trả tiền ăn">
              </mat-form-field>

              <div class="info-box info-amber mb-5">
                <mat-icon class="info-icon">shield</mat-icon>
                <span>Giao dịch yêu cầu xác thực OTP qua email.</span>
              </div>

              @if (p2pSuccess()) {
                <div class="success-banner">
                  <mat-icon class="banner-icon">check_circle</mat-icon>
                  {{ p2pSuccess() }}
                </div>
              } @else {
                <button mat-flat-button color="primary" type="submit"
                        class="w-full !h-11 text-base font-semibold"
                        [disabled]="transferring()">
                  @if (transferring()) {
                    <mat-spinner diameter="20" class="inline-block mr-2" />
                  } @else {
                    <mat-icon class="mr-1 !text-lg" style="vertical-align:middle">send</mat-icon>
                  }
                  Chuyển tiền
                </button>
              }
            </form>
          </div>
        </mat-tab>

        <!-- ── Tab 2: Tạo mã QR ──────────────────────────────────────── -->
        <mat-tab label="Tạo QR">
          <div class="pt-5">
            <form [formGroup]="qrGenerateForm" (ngSubmit)="submitGenerateQr()">

              <mat-form-field class="w-full mb-1">
                <mat-label>Số tiền yêu cầu</mat-label>
                <input matInput vndInput formControlName="amount"
                       [errorStateMatcher]="qrMatcher"
                       placeholder="10.000">
                <span matSuffix style="color:#64748B; padding-right:4px">₫</span>
                @if (showErr(qrGenerateForm, 'amount', 'required', qrSubmitted())) {
                  <mat-error>Vui lòng nhập số tiền</mat-error>
                }
                @if (showErr(qrGenerateForm, 'amount', 'min', qrSubmitted())) {
                  <mat-error>Số tiền tối thiểu là 10.000 ₫</mat-error>
                }
                @if (showErr(qrGenerateForm, 'amount', 'vndMultiple', qrSubmitted())) {
                  <mat-error>Số tiền phải là bội số của 1.000 ₫</mat-error>
                }
              </mat-form-field>

              <mat-form-field class="w-full mb-4">
                <mat-label>Nội dung (tuỳ chọn)</mat-label>
                <input matInput formControlName="note">
              </mat-form-field>

              <button mat-flat-button color="primary" type="submit"
                      class="w-full !h-11 font-semibold"
                      [disabled]="generatingQr()">
                @if (generatingQr()) {
                  <mat-spinner diameter="20" class="inline-block mr-2" />
                } @else {
                  <mat-icon class="mr-1 !text-lg" style="vertical-align:middle">qr_code_2</mat-icon>
                }
                Tạo mã QR
              </button>
            </form>

            @if (generatedQr()) {
              <div class="qr-card">
                <img [src]="generatedQr()!.qrImageUrl" alt="QR Code"
                     class="mx-auto w-48 h-48 mb-4 rounded-xl">
                <p class="text-2xl font-bold mb-1" style="color:#F59E0B">
                  {{ generatedQr()!.amount | currencyVnd }}
                </p>
                @if (generatedQr()!.note) {
                  <p class="text-sm mb-2" style="color:#94A3B8">{{ generatedQr()!.note }}</p>
                }
                <p class="text-xs" style="color:#64748B">
                  Hết hạn: {{ generatedQr()!.expiresAt | date:'HH:mm · dd/MM/yyyy' }}
                </p>
                <p class="text-xs mt-1 font-mono" style="color:#475569">
                  ID: {{ generatedQr()!.qrId }}
                </p>
                <div class="mt-4">
                  <button class="qr-save-btn" type="button" (click)="saveQr()">
                    <mat-icon style="font-size:16px;width:16px;height:16px">download</mat-icon>
                    Lưu ảnh QR
                  </button>
                </div>
              </div>
            }
          </div>
        </mat-tab>

        <!-- ── Tab 3: Quét QR ────────────────────────────────────────── -->
        <mat-tab label="Quét QR">
          <div class="pt-5">
            <!-- Hidden file input -->
            <input #qrFileInput type="file" accept="image/*" style="display:none"
                   (change)="onQrFileSelected($event)">

            <!-- Upload zone (shown when no QR decoded yet) -->
            @if (!qrInfo()) {
              <div class="qr-upload-zone mb-4"
                   [class.drag-over]="dragging()"
                   (click)="qrFileInput.click()"
                   (dragover)="$event.preventDefault(); dragging.set(true)"
                   (dragleave)="dragging.set(false)"
                   (drop)="onQrDrop($event)">
                @if (decoding()) {
                  <mat-spinner diameter="32" class="mx-auto mb-3" />
                  <p class="text-sm" style="color:#94A3B8">Đang đọc mã QR…</p>
                } @else {
                  <mat-icon class="!text-5xl mb-3" style="color:#475569">qr_code_scanner</mat-icon>
                  <p class="text-sm font-medium mb-1" style="color:#CBD5E1">Tải lên ảnh mã QR</p>
                  <p class="text-xs" style="color:#64748B">Nhấn để chọn ảnh hoặc kéo thả vào đây</p>
                }
              </div>
              @if (decodeError()) {
                <p class="text-xs mb-4 text-center" style="color:#EF4444">{{ decodeError() }}</p>
              }
            }

            <!-- QR info card after successful decode -->
            @if (qrInfo()) {
              <div class="qr-lookup-card mb-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-xs font-semibold uppercase tracking-widest" style="color:#64748B">Thông tin mã QR</span>
                  <button type="button" (click)="clearQr()"
                          style="background:none;border:none;cursor:pointer;display:flex;align-items:center;padding:2px;outline:none">
                    <mat-icon style="font-size:16px;width:16px;height:16px;color:#64748B">close</mat-icon>
                  </button>
                </div>
                <div class="qr-info-row">
                  <span style="color:#94A3B8">Số tiền</span>
                  <span class="font-bold" style="color:#F59E0B">{{ qrInfo()!.amount | currencyVnd }}</span>
                </div>
                @if (qrInfo()!.note) {
                  <div class="qr-info-sep"></div>
                  <div class="qr-info-row">
                    <span style="color:#94A3B8">Nội dung</span>
                    <span style="color:#F8FAFC">{{ qrInfo()!.note }}</span>
                  </div>
                }
              </div>
            }

            <form [formGroup]="qrPayForm" (ngSubmit)="submitPayQr()">
              <div class="info-box info-amber mb-5">
                <mat-icon class="info-icon">shield</mat-icon>
                <span>Thanh toán yêu cầu xác thực OTP qua email.</span>
              </div>

              @if (qrSuccess()) {
                <div class="success-banner">
                  <mat-icon class="banner-icon">check_circle</mat-icon>
                  {{ qrSuccess() }}
                </div>
              } @else {
                <button mat-flat-button color="primary" type="submit"
                        class="w-full !h-11 font-semibold"
                        [disabled]="!qrInfo() || paying()">
                  @if (paying()) {
                    <mat-spinner diameter="20" class="inline-block mr-2" />
                  } @else {
                    <mat-icon class="mr-1 !text-lg" style="vertical-align:middle">qr_code_scanner</mat-icon>
                  }
                  Thanh toán QR
                </button>
              }
            </form>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
})
export class TransferComponent implements OnInit {
  @ViewChild('qrFileInput') qrFileInput!: ElementRef<HTMLInputElement>;

  private transferService = inject(TransferService);
  private topupService = inject(TopupService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  wallet = signal<WalletInfo | null>(null);
  balanceVisible = signal(true);
  transferring = signal(false);
  generatingQr = signal(false);
  paying = signal(false);
  decoding = signal(false);
  dragging = signal(false);
  decodeError = signal<string | null>(null);
  p2pSuccess = signal<string | null>(null);
  qrSuccess = signal<string | null>(null);
  generatedQr = signal<QrInfo | null>(null);
  qrInfo = signal<QrInfo | null>(null);
  private currentQrId: string | null = null;

  p2pSubmitted = signal(false);
  qrSubmitted = signal(false);

  p2pMatcher = new SubmittedOrDirtyMatcher(() => this.p2pSubmitted());
  qrMatcher = new SubmittedOrDirtyMatcher(() => this.qrSubmitted());

  p2pForm: FormGroup = this.fb.group({
    receiverIdentifier: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(VND_MIN_TRANSFER), vndMultiple]],
    note: [''],
  });

  qrGenerateForm: FormGroup = this.fb.group({
    amount: [null, [Validators.required, Validators.min(VND_MIN_TRANSFER), vndMultiple]],
    note: [''],
  });

  qrPayForm: FormGroup = this.fb.group({});

  ngOnInit(): void {
    this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
  }

  showErr(form: FormGroup, field: string, error: string, submitted: boolean): boolean {
    const c = form.get(field);
    return !!(c?.hasError(error) && (submitted || (c.dirty && c.touched)));
  }

  // ── Save QR image ────────────────────────────────────────────────────
  saveQr(): void {
    const qr = this.generatedQr();
    if (!qr) return;
    fetch(qr.qrImageUrl)
      .then(r => r.blob())
      .then(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `qr-${qr.qrId}.png`;
        a.click();
        URL.revokeObjectURL(url);
      })
      .catch(() => window.open(qr.qrImageUrl, '_blank'));
  }

  // ── QR image upload & decode ─────────────────────────────────────────
  onQrFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.decodeQrFromFile(file);
    (event.target as HTMLInputElement).value = '';
  }

  onQrDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file?.type.startsWith('image/')) this.decodeQrFromFile(file);
  }

  clearQr(): void {
    this.qrInfo.set(null);
    this.currentQrId = null;
    this.decodeError.set(null);
  }

  private decodeQrFromFile(file: File): void {
    this.decoding.set(true);
    this.decodeError.set(null);
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d')!;
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        import('jsqr').then(({ default: jsQR }) => {
          const result = jsQR(imageData.data, imageData.width, imageData.height);
          this.decoding.set(false);
          if (!result) {
            this.decodeError.set('Không đọc được mã QR. Vui lòng thử ảnh khác.');
            return;
          }
          const qrId = this.extractQrId(result.data);
          if (!qrId) {
            this.decodeError.set('Mã QR không hợp lệ (không phải mã EzWallet).');
            return;
          }
          this.currentQrId = qrId;
          this.transferService.getQr(qrId).subscribe({
            next: (info) => this.qrInfo.set(info),
            error: () => this.decodeError.set('Mã QR không hợp lệ hoặc đã hết hạn.'),
          });
        });
      };
      img.src = e.target!.result as string;
    };
    reader.readAsDataURL(file);
  }

  private extractQrId(content: string): string | null {
    try {
      const url = new URL(content);
      return url.searchParams.get('qrId');
    } catch {
      return null;
    }
  }

  // ── Transfer ──────────────────────────────────────────────────────────
  submitP2P(): void {
    this.p2pSubmitted.set(true);
    if (this.p2pForm.invalid) return;
    const { receiverIdentifier, amount, note } = this.p2pForm.value;

    this.transferring.set(true);
    this.transferService.requestTransferOtp().subscribe({
      next: () => {
        this.transferring.set(false);
        const ref = this.dialog.open(OtpDialogComponent, {
          width: '380px',
          data: {
            title: 'Xác thực chuyển tiền',
            message: `Nhập OTP để chuyển ${new Intl.NumberFormat('vi-VN').format(amount)} ₫ đến ${receiverIdentifier}`,
          },
        });
        ref.afterClosed().subscribe((otpCode: string | null) => {
          if (!otpCode) return;
          this.transferring.set(true);
          this.transferService.p2pTransfer({ receiverIdentifier, amount, note, otpCode }).subscribe({
            next: () => {
              this.transferring.set(false);
              this.p2pForm.reset();
              this.p2pSubmitted.set(false);
              this.p2pSuccess.set('Chuyển tiền thành công!');
              setTimeout(() => this.p2pSuccess.set(null), 3000);
              this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
            },
            error: (err) => {
              this.transferring.set(false);
              this.snackBar.open(err?.error?.message ?? 'Chuyển tiền thất bại', 'Đóng', { duration: 4000 });
            },
          });
        });
      },
      error: (err) => {
        this.transferring.set(false);
        this.snackBar.open(err?.error?.message ?? 'Không thể gửi OTP', 'Đóng', { duration: 4000 });
      },
    });
  }

  submitGenerateQr(): void {
    this.qrSubmitted.set(true);
    if (this.qrGenerateForm.invalid) return;
    const { amount, note } = this.qrGenerateForm.value;
    this.generatingQr.set(true);
    this.transferService.generateQr({ amount, note }).subscribe({
      next: (qr) => {
        this.generatingQr.set(false);
        this.generatedQr.set(qr);
      },
      error: (err) => {
        this.generatingQr.set(false);
        this.snackBar.open(err?.error?.message ?? 'Tạo QR thất bại', 'Đóng', { duration: 4000 });
      },
    });
  }

  submitPayQr(): void {
    const qrId = this.currentQrId;
    if (!qrId || !this.qrInfo()) return;
    const amount = this.qrInfo()!.amount;

    this.paying.set(true);
    this.transferService.requestTransferOtp().subscribe({
      next: () => {
        this.paying.set(false);
        const ref = this.dialog.open(OtpDialogComponent, {
          width: '380px',
          data: {
            title: 'Xác thực thanh toán QR',
            message: `Nhập OTP để thanh toán ${new Intl.NumberFormat('vi-VN').format(amount)} ₫`,
          },
        });
        ref.afterClosed().subscribe((otpCode: string | null) => {
          if (!otpCode) return;
          this.paying.set(true);
          this.transferService.payQr({ qrId, otpCode }).subscribe({
            next: () => {
              this.paying.set(false);
              this.clearQr();
              this.qrSuccess.set('Thanh toán QR thành công!');
              setTimeout(() => this.qrSuccess.set(null), 3000);
              this.topupService.getWallet().subscribe({ next: w => this.wallet.set(w), error: () => {} });
            },
            error: (err) => {
              this.paying.set(false);
              this.snackBar.open(err?.error?.message ?? 'Thanh toán thất bại', 'Đóng', { duration: 4000 });
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
