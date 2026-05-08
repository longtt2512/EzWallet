export interface LoginRequest {
  username: string;
  password: string;
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
