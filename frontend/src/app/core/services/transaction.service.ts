import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TransactionResponse, TransactionFilter } from '../models/transaction.model';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionHistoryService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  list(filter: TransactionFilter = {}): Observable<PageResponse<TransactionResponse>> {
    const params: Record<string, string> = {};
    if (filter.type) params['type'] = filter.type;
    if (filter.status) params['status'] = filter.status;
    if (filter.dateFrom) params['dateFrom'] = filter.dateFrom;
    if (filter.dateTo) params['dateTo'] = filter.dateTo;
    if (filter.page != null) params['page'] = String(filter.page);
    if (filter.size != null) params['size'] = String(filter.size);

    return this.http
      .get<ApiResponse<PageResponse<TransactionResponse>>>(`${this.base}/transactions`, { params })
      .pipe(map(r => r.data));
  }
}
