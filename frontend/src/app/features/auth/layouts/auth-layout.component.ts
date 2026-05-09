import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, MatIconModule],
  template: `
    <div class="min-h-screen flex" style="background:#0F172A">

      <!-- Left branding panel -->
      <div class="hidden lg:flex lg:w-[45%] flex-col items-center justify-center p-12 relative overflow-hidden"
           style="background:linear-gradient(135deg,#0B1120 0%,#1a1040 100%)">

        <!-- Decorative glow -->
        <div class="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-72 h-72 rounded-full opacity-20"
             style="background:radial-gradient(circle,#F59E0B,transparent 70%)"></div>

        <div class="relative z-10 text-center">
          <!-- Icon badge -->
          <div class="inline-flex items-center justify-center w-20 h-20 rounded-2xl mb-6"
               style="background:rgba(245,158,11,0.15);border:1px solid rgba(245,158,11,0.3)">
            <mat-icon class="!text-5xl !w-12 !h-12" style="color:#F59E0B">account_balance_wallet</mat-icon>
          </div>

          <h1 class="text-4xl font-bold mb-3 tracking-tight" style="color:#F8FAFC">EzWallet</h1>
          <p class="text-base max-w-xs mx-auto leading-relaxed" style="color:#94A3B8">
            Quản lý tài chính thông minh.<br>Giao dịch an toàn, nhanh chóng.
          </p>

          <!-- Feature list -->
          <div class="mt-10 flex flex-col gap-4 text-left w-full max-w-xs mx-auto">
            @for (feat of features; track feat.icon) {
              <div class="flex items-center gap-3 rounded-xl p-3"
                   style="background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.06)">
                <div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                     style="background:rgba(245,158,11,0.15)">
                  <mat-icon class="!text-base !w-4 !h-4" style="color:#F59E0B">{{ feat.icon }}</mat-icon>
                </div>
                <span class="text-sm" style="color:#CBD5E1">{{ feat.label }}</span>
              </div>
            }
          </div>
        </div>
      </div>

      <!-- Right form panel -->
      <div class="w-full lg:w-[55%] flex items-center justify-center p-6 sm:p-10"
           style="background:#0F172A">
        <div class="w-full max-w-sm">
          <!-- Mobile logo -->
          <div class="lg:hidden text-center mb-8">
            <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl mb-3"
                 style="background:rgba(245,158,11,0.15);border:1px solid rgba(245,158,11,0.3)">
              <mat-icon class="!text-3xl !w-8 !h-8" style="color:#F59E0B">account_balance_wallet</mat-icon>
            </div>
            <h1 class="text-xl font-bold" style="color:#F8FAFC">EzWallet</h1>
          </div>

          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {
  readonly features = [
    { icon: 'send',             label: 'Chuyển tiền tức thì' },
    { icon: 'receipt_long',     label: 'Thanh toán hoá đơn dễ dàng' },
    { icon: 'lock',             label: 'Bảo mật đa lớp' },
  ];
}
