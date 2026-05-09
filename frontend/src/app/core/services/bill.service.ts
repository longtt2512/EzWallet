import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TransactionResponse } from '../models/transaction.model';

export interface BillProvider {
  id: number;
  code: string;
  name: string;
  serviceType: string;
}

export interface BillInfo {
  id: number;
  billCode: string;
  providerName: string;
  providerCode: string;
  customerCode: string;
  amount: number;
  status: 'UNPAID' | 'PAID' | 'EXPIRED' | 'CANCELLED';
  dueDate?: string;
}

export interface BillPayRequest {
  billCode: string;
  otpCode?: string;
  idempotencyKey?: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class BillService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  listProviders(): Observable<BillProvider[]> {
    return this.http
      .get<ApiResponse<BillProvider[]>>(`${this.base}/bills/providers`)
      .pipe(map(r => r.data));
  }

  lookupBill(providerCode: string, customerCode: string): Observable<BillInfo> {
    return this.http
      .get<ApiResponse<BillInfo>>(`${this.base}/bills/lookup`, {
        params: { providerCode, customerCode }
      })
      .pipe(map(r => r.data));
  }

  requestBillOtp(): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.base}/bills/otp`, {})
      .pipe(map(() => undefined));
  }

  payBill(req: BillPayRequest): Observable<BillInfo> {
    return this.http
      .post<ApiResponse<BillInfo>>(`${this.base}/bills/pay`, req)
      .pipe(map(r => r.data));
  }
}
