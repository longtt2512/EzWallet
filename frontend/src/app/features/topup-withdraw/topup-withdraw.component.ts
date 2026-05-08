import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Stub - Member 2 sẽ phát triển:
 *  - Form nạp tiền (chọn ngân hàng, số tiền)
 *  - Form rút tiền
 *  - Danh sách thẻ liên kết
 */
@Component({
  selector: 'app-topup-withdraw',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h1 class="text-2xl font-bold mb-4">Nạp / Rút tiền</h1>
      <p class="text-gray-500">Module sẽ được Member 2 hoàn thiện.</p>
    </div>
  `,
})
export class TopupWithdrawComponent {}
