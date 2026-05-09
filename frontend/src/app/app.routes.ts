import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ----- Auth (Member 1) -----
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  // ----- Khu vực bảo vệ -----
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layouts/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },
      // Member 2
      {
        path: 'topup-withdraw',
        loadChildren: () =>
          import('./features/topup-withdraw/topup-withdraw.routes').then(
            (m) => m.TOPUP_WITHDRAW_ROUTES
          ),
      },
      // Member 3 (Long)
      {
        path: 'transfer',
        loadChildren: () =>
          import('./features/transfer/transfer.routes').then(
            (m) => m.TRANSFER_ROUTES
          ),
      },
      // Member 4
      {
        path: 'bills',
        loadChildren: () =>
          import('./features/bill-payment/bill-payment.routes').then(
            (m) => m.BILL_PAYMENT_ROUTES
          ),
      },
      {
        path: 'history',
        loadComponent: () =>
          import(
            './features/transaction-history/transaction-history.component'
          ).then((m) => m.TransactionHistoryComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(
            (m) => m.ProfileComponent
          ),
      },
    ],
  },

  { path: '**', redirectTo: 'dashboard' },
];
