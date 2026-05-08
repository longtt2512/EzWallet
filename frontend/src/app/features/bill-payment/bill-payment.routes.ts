import { Routes } from '@angular/router';

/**
 * Routes module Thanh toán hoá đơn - Member 4 hoàn thiện chi tiết.
 */
export const BILL_PAYMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./bill-payment.component').then(
        (m) => m.BillPaymentComponent
      ),
  },
];
