import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Lịch sử giao dịch - dùng chung cho mọi module, hoàn thiện chi tiết bởi Member 4.
 */
@Component({
  selector: 'app-transaction-history',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h1 class="text-2xl font-bold mb-4">Lịch sử giao dịch</h1>
      <p class="text-gray-500">Module sẽ được hoàn thiện sau.</p>
    </div>
  `,
})
export class TransactionHistoryComponent {}
