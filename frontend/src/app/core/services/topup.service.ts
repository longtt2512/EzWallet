import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TransactionResponse } from '../models/transaction.model';
import { WalletInfo } from '../models/wallet.model';

export interface BankAccount {
  id: number;
  bankCode: string;
  maskedAccountNumber: string;
  accountHolder: string;
  isDefault: boolean;
  verified: boolean;
}

export interface BankAccountRequest {
  bankCode: string;
  accountNumber: string;
  accountHolder: string;
}

export interface TopupRequest {
  bankAccountId: number;
  amount: number;
  otpCode: string;
  idempotencyKey?: string;
}

export interface WithdrawRequest {
  bankAccountId: number;
  amount: number;
  otpCode: string;
  idempotencyKey?: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class TopupService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getWallet(): Observable<WalletInfo> {
    return this.http
      .get<ApiResponse<WalletInfo>>(`${this.base}/account/wallet`)
      .pipe(map(r => r.data));
  }

  listBankAccounts(): Observable<BankAccount[]> {
    return this.http
      .get<ApiResponse<BankAccount[]>>(`${this.base}/account/bank-accounts`)
      .pipe(map(r => r.data));
  }

  linkBankAccount(req: BankAccountRequest): Observable<BankAccount> {
    return this.http
      .post<ApiResponse<BankAccount>>(`${this.base}/account/bank-accounts`, req)
      .pipe(map(r => r.data));
  }

  setDefaultBankAccount(id: number): Observable<void> {
    return this.http
      .patch<ApiResponse<void>>(`${this.base}/account/bank-accounts/${id}/default`, {})
      .pipe(map(() => undefined));
  }

  deleteBankAccount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/account/bank-accounts/${id}`);
  }

  requestDepositOtp(): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.base}/topup/deposit/otp`, {})
      .pipe(map(() => undefined));
  }

  deposit(req: TopupRequest): Observable<TransactionResponse> {
    return this.http
      .post<ApiResponse<TransactionResponse>>(`${this.base}/topup/deposit`, req)
      .pipe(map(r => r.data));
  }

  requestWithdrawOtp(): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.base}/topup/withdraw/otp`, {})
      .pipe(map(() => undefined));
  }

  withdraw(req: WithdrawRequest): Observable<TransactionResponse> {
    return this.http
      .post<ApiResponse<TransactionResponse>>(`${this.base}/topup/withdraw`, req)
      .pipe(map(r => r.data));
  }
}
