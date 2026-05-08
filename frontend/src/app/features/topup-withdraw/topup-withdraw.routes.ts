import { Routes } from '@angular/router';

/**
 * Routes module Nạp/Rút tiền - Member 2 hoàn thiện chi tiết.
 */
export const TOPUP_WITHDRAW_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./topup-withdraw.component').then(
        (m) => m.TopupWithdrawComponent
      ),
  },
];
