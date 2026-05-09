import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRippleModule } from '@angular/material/core';

import { AuthService } from '@core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatRippleModule,
  ],
  styles: [`
    :host { display: flex; height: 100vh; overflow: hidden; }

    .sidebar {
      width: 240px;
      min-width: 240px;
      background: #0B1120;
      border-right: 1px solid #1E293B;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 16px;
      border-radius: 10px;
      margin: 2px 8px;
      color: #94A3B8;
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 150ms ease;
    }
    .nav-item:hover {
      background: #1E293B;
      color: #F8FAFC;
    }
    .nav-item.active {
      background: rgba(245,158,11,0.15);
      color: #F59E0B;
    }
    .nav-item.active mat-icon { color: #F59E0B; }
    .nav-item mat-icon { font-size: 20px; width: 20px; height: 20px; transition: color 150ms ease; }

    .main-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      background: #0F172A;
    }

    .topbar {
      height: 56px;
      min-height: 56px;
      background: #0B1120;
      border-bottom: 1px solid #1E293B;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 24px;
    }

    .topbar-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      overflow: hidden;
      cursor: pointer;
      border: 2px solid rgba(245,158,11,0.35);
      transition: border-color 150ms ease, box-shadow 150ms ease;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(245,158,11,0.12);
    }
    .topbar-avatar:hover {
      border-color: rgba(245,158,11,0.7);
      box-shadow: 0 0 0 3px rgba(245,158,11,0.15);
    }
    .topbar-avatar img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .page-area {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
    }
  `],
  template: `
    <aside class="sidebar">
      <!-- Logo -->
      <div class="flex items-center gap-3 px-5 py-5 border-b border-slate-800">
        <div class="w-8 h-8 rounded-lg bg-amber-500 flex items-center justify-center shrink-0">
          <mat-icon class="!text-base !w-5 !h-5 text-slate-900">account_balance_wallet</mat-icon>
        </div>
        <span class="text-base font-bold text-white tracking-tight">EzWallet</span>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 py-4 overflow-y-auto">
        @for (item of navItems; track item.path) {
          <a class="nav-item"
             [routerLink]="item.path"
             routerLinkActive="active"
             [routerLinkActiveOptions]="{ exact: false }">
            <mat-icon>{{ item.icon }}</mat-icon>
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>

      <!-- Logout at bottom -->
      <div class="p-4 border-t border-slate-800">
        <button class="nav-item w-full hover:!bg-red-500/10 hover:!text-red-400"
                (click)="logout()">
          <mat-icon>logout</mat-icon>
          <span>Đăng xuất</span>
        </button>
      </div>
    </aside>

    <div class="main-content">
      <!-- Topbar -->
      <header class="topbar">
        <span class="text-sm text-slate-400 font-medium">{{ currentPageLabel() }}</span>
        <div class="flex items-center gap-2">
          <div class="topbar-avatar" (click)="router.navigate(['/profile'])"
               [matTooltip]="auth.currentUser()?.fullName || auth.currentUser()?.username || 'Tài khoản'"
               aria-label="Đến trang tài khoản">
            @if (auth.currentUser()?.avatarUrl) {
              <img [src]="auth.currentUser()!.avatarUrl" alt="Ảnh đại diện"
                   (error)="onAvatarImgError($event)">
            } @else {
              <mat-icon class="!text-base !w-4 !h-4 text-amber-400">person</mat-icon>
            }
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main class="page-area">
        <router-outlet />
      </main>
    </div>
  `,
})
export class MainLayoutComponent {
  readonly auth = inject(AuthService);
  readonly router = inject(Router);

  readonly navItems: NavItem[] = [
    { path: '/dashboard',               label: 'Tổng quan',     icon: 'dashboard' },
    { path: '/topup-withdraw',          label: 'Nạp / Rút tiền', icon: 'account_balance_wallet' },
    { path: '/transfer',                label: 'Chuyển tiền',   icon: 'send' },
    { path: '/bills',                   label: 'Hoá đơn',       icon: 'receipt_long' },
    { path: '/history',                 label: 'Lịch sử GD',    icon: 'history' },
    { path: '/profile',                  label: 'Tài khoản',    icon: 'person' },
  ];

  currentPageLabel(): string {
    const url = this.router.url;
    return this.navItems.find(n => url.startsWith(n.path))?.label ?? 'EzWallet';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/auth/login']);
  }

  onAvatarImgError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }
}
