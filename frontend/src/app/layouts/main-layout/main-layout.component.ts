import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';

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
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatSidenavModule,
    MatListModule,
  ],
  template: `
    <mat-sidenav-container class="h-screen">
      <mat-sidenav mode="side" opened class="w-60 p-4 bg-white border-r">
        <h1 class="text-xl font-bold mb-6 text-primary">EzWallet</h1>
        <mat-nav-list>
          @for (item of navItems; track item.path) {
            <a
              mat-list-item
              [routerLink]="item.path"
              routerLinkActive="bg-blue-50 text-primary"
              [routerLinkActiveOptions]="{ exact: false }">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary" class="flex justify-between">
          <span>Ví điện tử EzWallet</span>
          <button mat-button (click)="logout()">
            <mat-icon>logout</mat-icon>
            Đăng xuất
          </button>
        </mat-toolbar>

        <main class="p-6">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class MainLayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems: NavItem[] = [
    { path: '/dashboard',       label: 'Trang chủ',     icon: 'dashboard' },
    { path: '/topup-withdraw',  label: 'Nạp/Rút tiền',  icon: 'account_balance_wallet' },
    { path: '/transfer',        label: 'Chuyển tiền',   icon: 'send' },
    { path: '/bills',           label: 'Hoá đơn',       icon: 'receipt_long' },
    { path: '/history',         label: 'Lịch sử',       icon: 'history' },
  ];

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/auth/login']);
  }
}
