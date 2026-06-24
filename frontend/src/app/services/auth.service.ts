import { Injectable } from '@angular/core';

export type SocialProvider = 'google' | 'apple' | 'facebook';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResult {
  success: boolean;
}

/**
 * Front-end authentication boundary.
 *
 * Replace the mocked methods with Supabase Auth calls when the backend
 * integration is ready. Consumers do not need to know which provider is used.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly mockDelay = 700;

  login(_credentials: LoginCredentials): Promise<AuthResult> {
    return this.mockSuccess();
  }

  loginWithSocialProvider(_provider: SocialProvider): Promise<AuthResult> {
    return this.mockSuccess();
  }

  private async mockSuccess(): Promise<AuthResult> {
    await new Promise<void>((resolve) => setTimeout(resolve, this.mockDelay));
    return { success: true };
  }
}
