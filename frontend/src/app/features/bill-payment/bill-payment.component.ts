import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Stub - Member 4 sẽ phát triển:
 *  - Tra cứu hoá đơn (điện, nước, internet, telco)
 *  - Thanh toán hoá đơn
 *  - Lịch sử thanh toán
 */
@Component({
  selector: 'app-bill-payment',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h1 class="text-2xl font-bold mb-4">Thanh toán hoá đơn</h1>
      <p class="text-gray-500">Module sẽ được Member 4 hoàn thiện.</p>
    </div>
  `,
})
export class BillPaymentComponent {}
