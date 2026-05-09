import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models/auth.model';

function passwordMatchValidator(g: AbstractControl): ValidationErrors | null {
  const pw = g.get('newPassword')?.value;
  const confirm = g.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { passwordMismatch: true } : null;
}

const MAX_AVATAR_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatProgressSpinnerModule,
  ],
  styles: [`
    :host { display: block; }

    .page-card {
      background: #111827;
      border: 1px solid #1E293B;
      border-radius: 16px;
      overflow: hidden;
    }

    /* ---------- Avatar ---------- */
    .avatar-wrap {
      position: relative;
      width: 80px;
      height: 80px;
      border-radius: 50%;
      cursor: pointer;
      flex-shrink: 0;
    }

    .avatar-img {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      object-fit: cover;
      border: 2px solid rgba(245,158,11,0.4);
      display: block;
    }

    .avatar-placeholder {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: rgba(245,158,11,0.12);
      border: 2px solid rgba(245,158,11,0.3);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .avatar-overlay {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      background: rgba(0,0,0,0.58);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      opacity: 0;
      transition: opacity 200ms ease;
      backdrop-filter: blur(2px);
    }
    .avatar-wrap:hover .avatar-overlay { opacity: 1; }

    .avatar-upload-spinner {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      background: rgba(0,0,0,0.65);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* ---------- Header ---------- */
    .card-header {
      background: linear-gradient(135deg, #1a2540 0%, #0f1729 100%);
      border-bottom: 1px solid #1E293B;
      padding: 20px 24px;
    }

    /* ---------- Badges ---------- */
    .tier-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 10px;
      border-radius: 9999px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .tier-bronze   { background: rgba(180,115,60,0.15);  color: #CD7F32; border: 1px solid rgba(205,127,50,0.3); }
    .tier-silver   { background: rgba(160,160,160,0.15); color: #A8A9AD; border: 1px solid rgba(168,169,173,0.3); }
    .tier-gold     { background: rgba(255,200,0,0.12);   color: #F59E0B; border: 1px solid rgba(245,158,11,0.3); }
    .tier-platinum { background: rgba(100,200,255,0.12); color: #38BDF8; border: 1px solid rgba(56,189,248,0.3); }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 10px;
      border-radius: 9999px;
      font-size: 11px;
      font-weight: 600;
    }
    .status-active   { background: rgba(22,163,74,0.15);   color: #22C55E; border: 1px solid rgba(34,197,94,0.25); }
    .status-locked   { background: rgba(239,68,68,0.12);   color: #F87171; border: 1px solid rgba(248,113,113,0.25); }
    .status-pending  { background: rgba(245,158,11,0.12);  color: #F59E0B; border: 1px solid rgba(245,158,11,0.25); }
    .status-banned   { background: rgba(100,116,139,0.12); color: #94A3B8; border: 1px solid rgba(148,163,184,0.25); }

    /* ---------- Read-only fields ---------- */
    .readonly-field {
      background: rgba(255,255,255,0.03);
      border: 1px solid #1E293B;
      border-radius: 8px;
      padding: 10px 14px;
    }

    /* ---------- Tabs ---------- */
    ::ng-deep .mat-mdc-tab-group .mat-mdc-tab .mdc-tab-indicator__content--underline {
      border-color: #F59E0B !important;
    }
    ::ng-deep .mat-mdc-tab-group .mat-mdc-tab.mdc-tab--active .mdc-tab__text-label {
      color: #F59E0B !important;
    }
    ::ng-deep .mat-mdc-tab-group .mat-mdc-tab .mdc-tab__text-label { color: #94A3B8; }
    ::ng-deep .mat-mdc-tab-group .mat-mdc-tab-header {
      background: #0f1729;
      border-bottom: 1px solid #1E293B;
    }

    /* ---------- Dark form fields ---------- */
    ::ng-deep .profile-form .mat-mdc-text-field-wrapper { background: rgba(255,255,255,0.04) !important; }
    ::ng-deep .profile-form .mdc-text-field--outlined .mdc-notched-outline__leading,
    ::ng-deep .profile-form .mdc-text-field--outlined .mdc-notched-outline__notch,
    ::ng-deep .profile-form .mdc-text-field--outlined .mdc-notched-outline__trailing { border-color: #334155 !important; }
    ::ng-deep .profile-form .mdc-text-field--focused .mdc-notched-outline__leading,
    ::ng-deep .profile-form .mdc-text-field--focused .mdc-notched-outline__notch,
    ::ng-deep .profile-form .mdc-text-field--focused .mdc-notched-outline__trailing { border-color: #F59E0B !important; }
    ::ng-deep .profile-form .mat-mdc-form-field-label,
    ::ng-deep .profile-form .mat-mdc-floating-label { color: #94A3B8 !important; }
    ::ng-deep .profile-form .mdc-text-field--focused .mat-mdc-floating-label { color: #F59E0B !important; }
    ::ng-deep .profile-form input.mat-mdc-input-element { color: #F8FAFC !important; }

    /* ---------- Success banner ---------- */
    .success-banner {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      height: 44px;
      background: #16A34A;
      border-radius: 9999px;
      color: #ffffff;
      font-size: 14px;
      font-weight: 600;
      animation: bannerIn 220ms cubic-bezier(.22,1,.36,1) forwards;
    }
    @keyframes bannerIn {
      from { opacity: 0; transform: translateY(6px) scale(0.98); }
      to   { opacity: 1; transform: translateY(0)   scale(1);    }
    }
    .banner-icon { font-size: 18px; width: 18px; height: 18px; }

    /* ---------- Submit button ---------- */
    .submit-btn {
      width: 100%;
      height: 44px;
      background: linear-gradient(135deg, #F59E0B, #D97706) !important;
      color: #1a1a1a !important;
      font-weight: 700 !important;
      font-size: 14px !important;
      border-radius: 9999px !important;
      letter-spacing: 0.3px;
    }
    .submit-btn:disabled { opacity: 0.5 !important; }
  `],
  template: `
    <!-- Hidden file input -->
    <input #avatarInput type="file" accept="image/jpeg,image/png,image/webp,image/gif"
           style="display:none" (change)="onAvatarFileChange($event)">

    <div class="max-w-2xl mx-auto">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-white">Tài khoản</h1>
        <p class="text-slate-400 text-sm mt-1">Quản lý thông tin cá nhân và bảo mật</p>
      </div>

      <div class="page-card">
        <!-- Profile summary header -->
        <div class="card-header">
          <div class="flex items-center gap-5">

            <!-- Avatar with upload overlay -->
            <div class="avatar-wrap" (click)="triggerAvatarInput()"
                 [attr.aria-label]="'Thay đổi ảnh đại diện'" role="button" tabindex="0"
                 (keydown.enter)="triggerAvatarInput()" (keydown.space)="triggerAvatarInput()">

              @if (avatarPreview() || profile()?.avatarUrl) {
                <img class="avatar-img" [src]="avatarPreview() || profile()!.avatarUrl"
                     alt="Ảnh đại diện" (error)="onImgError($event)">
              } @else {
                <div class="avatar-placeholder">
                  <mat-icon style="font-size:36px;width:36px;height:36px;color:#F59E0B">person</mat-icon>
                </div>
              }

              @if (avatarUploading()) {
                <div class="avatar-upload-spinner">
                  <mat-spinner diameter="28"
                    style="--mdc-circular-progress-active-indicator-color:#F59E0B"/>
                </div>
              } @else {
                <div class="avatar-overlay">
                  <mat-icon style="font-size:20px;width:20px;height:20px;color:#fff">photo_camera</mat-icon>
                  <span style="font-size:9px;color:#fff;font-weight:600;letter-spacing:0.3px">Thay đổi</span>
                </div>
              }
            </div>

            <!-- Name + badges -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-white font-semibold text-lg truncate">
                  {{ profile()?.fullName || profile()?.username || '...' }}
                </span>
                @if (profile()) {
                  <span class="tier-badge" [class]="tierClass(profile()!.tier)">
                    {{ profile()!.tier }}
                  </span>
                  <span class="status-badge" [class]="statusClass(profile()!.status)">
                    <span style="width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block"></span>
                    {{ statusLabel(profile()!.status) }}
                  </span>
                }
              </div>
              <p class="text-slate-400 text-sm mt-0.5 truncate">{{ profile()?.email || '' }}</p>
              @if (avatarError()) {
                <p class="text-red-400 text-xs mt-1">{{ avatarError() }}</p>
              }
              @if (!avatarUploading()) {
                <p class="text-slate-500 text-xs mt-1">Nhấn vào ảnh để thay đổi · Tối đa 5MB · JPG, PNG, WebP</p>
              }
            </div>
          </div>
        </div>

        <!-- Tabs -->
        <mat-tab-group animationDuration="200ms" [disableRipple]="true">

          <!-- Tab 1: Profile Info -->
          <mat-tab label="Thông tin hồ sơ">
            <div class="p-6 profile-form">
              @if (loadingProfile()) {
                <div class="flex justify-center py-8">
                  <mat-spinner diameter="32"/>
                </div>
              } @else {
                <div class="grid grid-cols-1 gap-4 mb-5">
                  <div class="readonly-field">
                    <p class="text-xs text-slate-500 mb-1">Tên đăng nhập</p>
                    <p class="text-slate-300 font-medium text-sm">{{ profile()?.username || '—' }}</p>
                  </div>
                  <div class="readonly-field">
                    <p class="text-xs text-slate-500 mb-1">Email</p>
                    <p class="text-slate-300 font-medium text-sm">{{ profile()?.email || '—' }}</p>
                  </div>
                </div>

                <form [formGroup]="profileForm" (ngSubmit)="saveProfile()" class="flex flex-col gap-4">
                  <mat-form-field appearance="outline" class="w-full">
                    <mat-label>Họ và tên</mat-label>
                    <input matInput formControlName="fullName" autocomplete="name" placeholder="Nguyễn Văn A">
                    @if (profileForm.get('fullName')?.hasError('maxlength') && profileForm.get('fullName')?.touched) {
                      <mat-error>Họ và tên tối đa 100 ký tự</mat-error>
                    }
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="w-full">
                    <mat-label>Số điện thoại</mat-label>
                    <input matInput formControlName="phone" autocomplete="tel" placeholder="0912345678">
                    @if (profileForm.get('phone')?.hasError('pattern') && profileForm.get('phone')?.touched) {
                      <mat-error>Số điện thoại không hợp lệ (VD: 0912345678)</mat-error>
                    }
                  </mat-form-field>

                  @if (profileError()) {
                    <div class="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 flex items-center gap-3">
                      <mat-icon style="color:#F87171;font-size:18px;width:18px;height:18px">error_outline</mat-icon>
                      <p class="text-red-400 text-sm">{{ profileError() }}</p>
                    </div>
                  }

                  <div class="mt-2">
                    @if (profileSuccess()) {
                      <div class="success-banner">
                        <mat-icon class="banner-icon">check_circle</mat-icon>
                        {{ profileSuccess() }}
                      </div>
                    } @else {
                      <button type="submit" class="submit-btn" mat-flat-button
                              [disabled]="profileForm.invalid || savingProfile()">
                        @if (savingProfile()) {
                          <mat-spinner diameter="18"
                            style="display:inline-block;margin-right:8px;--mdc-circular-progress-active-indicator-color:#1a1a1a"/>
                        }
                        Lưu thay đổi
                      </button>
                    }
                  </div>
                </form>
              }
            </div>
          </mat-tab>

          <!-- Tab 2: Change Password -->
          <mat-tab label="Đổi mật khẩu">
            <div class="p-6 profile-form">
              <form [formGroup]="pwForm" (ngSubmit)="changePassword()" class="flex flex-col gap-4">
                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Mật khẩu hiện tại</mat-label>
                  <input matInput [type]="hideOld() ? 'password' : 'text'"
                         formControlName="currentPassword" autocomplete="current-password">
                  <button type="button" matSuffix
                          (click)="hideOld.set(!hideOld())"
                          style="background:none;border:none;padding:4px;cursor:pointer;display:flex;align-items:center;outline:none">
                    <mat-icon style="font-size:18px;width:18px;height:18px;color:#64748B">
                      {{ hideOld() ? 'visibility_off' : 'visibility' }}
                    </mat-icon>
                  </button>
                  @if (pwForm.get('currentPassword')?.hasError('required') && pwForm.get('currentPassword')?.touched) {
                    <mat-error>Vui lòng nhập mật khẩu hiện tại</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Mật khẩu mới</mat-label>
                  <input matInput [type]="hideNew() ? 'password' : 'text'"
                         formControlName="newPassword" autocomplete="new-password">
                  <button type="button" matSuffix
                          (click)="hideNew.set(!hideNew())"
                          style="background:none;border:none;padding:4px;cursor:pointer;display:flex;align-items:center;outline:none">
                    <mat-icon style="font-size:18px;width:18px;height:18px;color:#64748B">
                      {{ hideNew() ? 'visibility_off' : 'visibility' }}
                    </mat-icon>
                  </button>
                  @if (pwForm.get('newPassword')?.hasError('required') && pwForm.get('newPassword')?.touched) {
                    <mat-error>Vui lòng nhập mật khẩu mới</mat-error>
                  }
                  @if (pwForm.get('newPassword')?.hasError('minlength') && pwForm.get('newPassword')?.touched) {
                    <mat-error>Mật khẩu mới tối thiểu 8 ký tự</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Xác nhận mật khẩu mới</mat-label>
                  <input matInput [type]="hideConfirm() ? 'password' : 'text'"
                         formControlName="confirmPassword" autocomplete="new-password">
                  <button type="button" matSuffix
                          (click)="hideConfirm.set(!hideConfirm())"
                          style="background:none;border:none;padding:4px;cursor:pointer;display:flex;align-items:center;outline:none">
                    <mat-icon style="font-size:18px;width:18px;height:18px;color:#64748B">
                      {{ hideConfirm() ? 'visibility_off' : 'visibility' }}
                    </mat-icon>
                  </button>
                  @if (pwForm.hasError('passwordMismatch') && pwForm.get('confirmPassword')?.touched) {
                    <mat-error>Mật khẩu xác nhận không khớp</mat-error>
                  }
                </mat-form-field>

                @if (pwError()) {
                  <div class="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 flex items-center gap-3">
                    <mat-icon style="color:#F87171;font-size:18px;width:18px;height:18px">error_outline</mat-icon>
                    <p class="text-red-400 text-sm">{{ pwError() }}</p>
                  </div>
                }

                <div class="mt-2">
                  @if (pwSuccess()) {
                    <div class="success-banner">
                      <mat-icon class="banner-icon">check_circle</mat-icon>
                      {{ pwSuccess() }}
                    </div>
                  } @else {
                    <button type="submit" class="submit-btn" mat-flat-button
                            [disabled]="pwForm.invalid || changingPw()">
                      @if (changingPw()) {
                        <mat-spinner diameter="18"
                          style="display:inline-block;margin-right:8px;--mdc-circular-progress-active-indicator-color:#1a1a1a"/>
                      }
                      Đổi mật khẩu
                    </button>
                  }
                </div>
              </form>
            </div>
          </mat-tab>

        </mat-tab-group>
      </div>
    </div>
  `,
})
export class ProfileComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  @ViewChild('avatarInput') private avatarInput!: ElementRef<HTMLInputElement>;

  profile = signal<UserProfile | null>(null);
  loadingProfile = signal(true);
  savingProfile = signal(false);
  changingPw = signal(false);
  avatarUploading = signal(false);
  avatarPreview = signal<string | null>(null);
  avatarError = signal<string | null>(null);

  profileSuccess = signal<string | null>(null);
  profileError = signal<string | null>(null);
  pwSuccess = signal<string | null>(null);
  pwError = signal<string | null>(null);

  hideOld = signal(true);
  hideNew = signal(true);
  hideConfirm = signal(true);

  profileForm: FormGroup = this.fb.group({
    fullName: ['', Validators.maxLength(100)],
    phone: ['', Validators.pattern(/^(\+84|0)[3-9]\d{8}$/)],
  });

  pwForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  ngOnInit(): void {
    this.auth.getProfile().subscribe({
      next: (res) => {
        this.loadingProfile.set(false);
        if (res.success && res.data) {
          this.profile.set(res.data);
          this.profileForm.patchValue({
            fullName: res.data.fullName ?? '',
            phone: res.data.phone ?? '',
          });
        }
      },
      error: () => this.loadingProfile.set(false),
    });
  }

  triggerAvatarInput(): void {
    this.avatarInput.nativeElement.click();
  }

  onAvatarFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    this.avatarError.set(null);

    if (!file.type.startsWith('image/')) {
      this.avatarError.set('Chỉ chấp nhận file ảnh (JPG, PNG, WebP)');
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      this.avatarError.set('Ảnh quá lớn — tối đa 5MB');
      return;
    }

    // Show local preview immediately
    const reader = new FileReader();
    reader.onload = (e) => this.avatarPreview.set(e.target?.result as string);
    reader.readAsDataURL(file);

    this.avatarUploading.set(true);
    this.auth.uploadAvatar(file).subscribe({
      next: (res) => {
        this.avatarUploading.set(false);
        if (res.success && res.data) {
          this.profile.set(res.data);
          this.avatarPreview.set(null); // backend URL now takes over
        }
      },
      error: (err) => {
        this.avatarUploading.set(false);
        this.avatarPreview.set(null);
        this.avatarError.set(err?.error?.message ?? 'Tải ảnh lên thất bại');
      },
    });
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    this.savingProfile.set(true);
    this.profileError.set(null);
    const { fullName, phone } = this.profileForm.value;
    this.auth.updateProfile(fullName, phone).subscribe({
      next: (res) => {
        this.savingProfile.set(false);
        if (res.success && res.data) this.profile.set(res.data);
        this.profileSuccess.set('Cập nhật hồ sơ thành công!');
        setTimeout(() => this.profileSuccess.set(null), 3000);
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.profileError.set(err?.error?.message ?? 'Cập nhật thất bại');
      },
    });
  }

  changePassword(): void {
    if (this.pwForm.invalid) return;
    this.changingPw.set(true);
    this.pwError.set(null);
    const { currentPassword, newPassword, confirmPassword } = this.pwForm.value;
    this.auth.changePassword(currentPassword, newPassword, confirmPassword).subscribe({
      next: () => {
        this.changingPw.set(false);
        this.pwForm.reset();
        this.pwSuccess.set('Đổi mật khẩu thành công!');
        setTimeout(() => this.pwSuccess.set(null), 3000);
      },
      error: (err) => {
        this.changingPw.set(false);
        this.pwError.set(err?.error?.message ?? 'Đổi mật khẩu thất bại');
      },
    });
  }

  tierClass(tier: string): string { return `tier-${tier.toLowerCase()}`; }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'status-active', LOCKED: 'status-locked',
      PENDING_VERIFICATION: 'status-pending', BANNED: 'status-banned',
    };
    return map[status] ?? 'status-pending';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Hoạt động', LOCKED: 'Bị khoá',
      PENDING_VERIFICATION: 'Chờ xác thực', BANNED: 'Bị cấm',
    };
    return map[status] ?? status;
  }
}
