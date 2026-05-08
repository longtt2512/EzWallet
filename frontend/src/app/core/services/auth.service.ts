import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '@env/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthTokens,
  LoginRequest,
  RegisterRequest,
  UserProfile,
} from '../models/auth.model';

/**
 * Quản lý phiên đăng nhập, gọi API xác thực, lưu/đọc token.
 *
 * <p>Token lưu trong localStorage (đơn giản cho BTL).
 * Trong production thật nên dùng httpOnly cookie để chống XSS.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;

  /** Signal phản ánh trạng thái đăng nhập, dùng cho header / guard */
  readonly isAuthenticated = signal<boolean>(this.hasValidToken());
  readonly currentUser = signal<UserProfile | null>(null);

  login(payload: LoginRequest): Observable<ApiResponse<AuthTokens>> {
    return this.http
      .post<ApiResponse<AuthTokens>>(`${this.apiUrl}/login`, payload)
      .pipe(
        tap((res) => {
          if (res.success && res.data) {
            this.saveTokens(res.data);
            this.isAuthenticated.set(true);
          }
        })
      );
  }

  register(payload: RegisterRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.post<ApiResponse<UserProfile>>(
      `${this.apiUrl}/register`,
      payload
    );
  }

  logout(): void {
    localStorage.removeItem(environment.jwtAccessKey);
    localStorage.removeItem(environment.jwtRefreshKey);
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(environment.jwtAccessKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(environment.jwtRefreshKey);
  }

  private saveTokens(tokens: AuthTokens): void {
    localStorage.setItem(environment.jwtAccessKey, tokens.accessToken);
    localStorage.setItem(environment.jwtRefreshKey, tokens.refreshToken);
  }

  private hasValidToken(): boolean {
    return !!localStorage.getItem(environment.jwtAccessKey);
  }
}
