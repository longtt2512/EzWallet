import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TransactionResponse } from '../models/transaction.model';

export interface P2PTransferRequest {
  receiverIdentifier: string;
  amount: number;
  note?: string;
  otpCode: string;
  idempotencyKey?: string;
}

export interface QrGenerateRequest {
  amount: number;
  note?: string;
  expiresInMinutes?: number;
}

export interface QrPayRequest {
  qrId: string;
  otpCode: string;
  idempotencyKey?: string;
}

export interface QrInfo {
  qrId: string;
  qrImageUrl: string;
  amount: number;
  note?: string;
  expiresAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class TransferService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  requestTransferOtp(): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.base}/transfer/otp`, {})
      .pipe(map(() => undefined));
  }

  p2pTransfer(req: P2PTransferRequest): Observable<TransactionResponse> {
    return this.http
      .post<ApiResponse<TransactionResponse>>(`${this.base}/transfer/p2p`, req)
      .pipe(map(r => r.data));
  }

  generateQr(req: QrGenerateRequest): Observable<QrInfo> {
    return this.http
      .post<ApiResponse<QrInfo>>(`${this.base}/transfer/qr/generate`, req)
      .pipe(map(r => r.data));
  }

  getQr(qrId: string): Observable<QrInfo> {
    return this.http
      .get<ApiResponse<QrInfo>>(`${this.base}/transfer/qr/${qrId}`)
      .pipe(map(r => r.data));
  }

  payQr(req: QrPayRequest): Observable<TransactionResponse> {
    return this.http
      .post<ApiResponse<TransactionResponse>>(`${this.base}/transfer/qr/pay`, req)
      .pipe(map(r => r.data));
  }
}
