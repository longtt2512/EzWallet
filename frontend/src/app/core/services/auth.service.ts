import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '@env/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  LoginRequest,
  LoginResponse,
  OtpResendRequest,
  OtpVerifyRequest,
  RegisterRequest,
  UserProfile,
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;

  readonly isAuthenticated = signal<boolean>(this.hasValidToken());
  readonly currentUser = signal<UserProfile | null>(null);

  login(payload: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, payload)
      .pipe(
        tap((res) => {
          if (res.success && res.data) {
            this.saveTokens(res.data);
            this.currentUser.set(res.data.user);
            this.isAuthenticated.set(true);
          }
        }),
      );
  }

  register(payload: RegisterRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/register`, payload);
  }

  verifyOtp(payload: OtpVerifyRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/otp/verify`, payload);
  }

  resendOtp(payload: OtpResendRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/otp/resend`, payload);
  }

  forgotPassword(identifier: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/forgot-password`, { identifier });
  }

  resetPassword(identifier: string, otpCode: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-password`, { identifier, otpCode, newPassword });
  }

  uploadAvatar(file: File): Observable<ApiResponse<UserProfile>> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ApiResponse<UserProfile>>(`${this.apiUrl}/avatar`, fd).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.currentUser.set(res.data);
        }
      }),
    );
  }

  getProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.apiUrl}/profile`).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.currentUser.set(res.data);
        }
      }),
    );
  }

  updateProfile(fullName: string, phone: string): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.apiUrl}/profile`, { fullName, phone }).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.currentUser.set(res.data);
        }
      }),
    );
  }

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/change-password`, {
      currentPassword, newPassword, confirmPassword,
    });
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({ error: () => {} });
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

  private saveTokens(tokens: LoginResponse): void {
    localStorage.setItem(environment.jwtAccessKey, tokens.accessToken);
    localStorage.setItem(environment.jwtRefreshKey, tokens.refreshToken);
  }

  private hasValidToken(): boolean {
    return !!localStorage.getItem(environment.jwtAccessKey);
  }
}
