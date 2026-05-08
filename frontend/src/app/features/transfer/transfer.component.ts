import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Stub - Member 3 (Long) sẽ phát triển:
 *  - Chuyển tiền P2P trong ví (theo username/SĐT)
 *  - Chuyển tiền liên ngân hàng
 *  - Quét QR thanh toán merchant
 *  - Xác thực OTP trước khi confirm
 */
@Component({
  selector: 'app-transfer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h1 class="text-2xl font-bold mb-4">Chuyển tiền</h1>
      <p class="text-gray-500">Module sẽ được Member 3 (Long) hoàn thiện.</p>
    </div>
  `,
})
export class TransferComponent {}
