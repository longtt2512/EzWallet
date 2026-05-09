export type TransactionType = 'TOPUP' | 'WITHDRAW' | 'TRANSFER' | 'BILL_PAYMENT';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface TransactionResponse {
  id: number;
  transactionRef: string;
  type: TransactionType;
  status: TransactionStatus;
  amount: number;
  fee: number;
  currency: string;
  description?: string;
  failureReason?: string;
  completedAt?: string;
  createdAt: string;
  direction?: 'CREDIT' | 'DEBIT';
  sourceBalanceBefore?: number;
  sourceBalanceAfter?: number;
  targetBalanceBefore?: number;
  targetBalanceAfter?: number;
}

export interface TransactionFilter {
  type?: TransactionType;
  status?: TransactionStatus;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}
