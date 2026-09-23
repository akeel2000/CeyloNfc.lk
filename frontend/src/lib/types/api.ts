export interface ApiSuccess<T> {
  success: true;
  data: T;
  message: string | null;
  timestamp: string;
}

export interface ApiError {
  success: false;
  error: {
    code: string;
    message: string;
  };
  timestamp: string;
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError;

export interface UserMe {
  uuid: string;
  email: string;
  phone: string | null;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
}
