export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export type OtpPurpose = 'REGISTER' | 'RESET_PASSWORD' | 'TRANSFER' | 'WITHDRAW';

export interface OtpVerifyRequest {
  identifier: string;
  purpose: OtpPurpose;
  code: string;
}

export interface OtpResendRequest {
  identifier: string;
  purpose: OtpPurpose;
}

export interface RegisterRequest {
  username: string;
  email: string;
  phone: string;
  password: string;
  fullName?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  phone: string;
  fullName?: string;
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'LOCKED' | 'BANNED';
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
}
