/**
 * Khớp với ApiResponse<T> ở backend.
 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data?: T;
  errors?: FieldError[];
  timestamp: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
