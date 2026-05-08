import { Routes } from '@angular/router';

/**
 * Routes module Chuyển tiền + QR - Member 3 (Long) hoàn thiện chi tiết.
 */
export const TRANSFER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./transfer.component').then((m) => m.TransferComponent),
  },
];
