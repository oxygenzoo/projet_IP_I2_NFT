import { Injectable } from '@angular/core';
import { AuthError, createClient, Provider, Session, SupabaseClient, User } from '@supabase/supabase-js';

import { environment } from '../../environments/environment';

export type SocialProvider = 'google' | 'apple' | 'facebook';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface SignupCredentials extends LoginCredentials {
  fullName?: string;
}

export interface AuthResult {
  success: boolean;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly supabase: SupabaseClient | null = this.createSupabaseClient();

  async login(credentials: LoginCredentials): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    const { error } = await client.auth.signInWithPassword({
      email: credentials.email,
      password: credentials.password,
    });

    return this.toAuthResult(error, 'Connexion réussie.');
  }

  async signup(credentials: SignupCredentials): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    const redirectTo = this.getAuthRedirectUrl();
    const { data, error } = await client.auth.signUp({
      email: credentials.email,
      password: credentials.password,
      options: {
        data: {
          full_name: credentials.fullName ?? '',
        },
        emailRedirectTo: redirectTo,
      },
    });

    if (error) {
      return this.toAuthResult(error);
    }

    if (!data.session) {
      return {
        success: true,
        message: 'Compte créé. Vérifiez vos emails pour confirmer votre inscription.',
      };
    }

    return { success: true, message: 'Compte créé.' };
  }

  async loginWithSocialProvider(provider: SocialProvider): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    const { error } = await client.auth.signInWithOAuth({
      provider: provider as Provider,
      options: {
        redirectTo: this.getAuthRedirectUrl(),
      },
    });

    return this.toAuthResult(error);
  }

  async logout(): Promise<void> {
    await this.supabase?.auth.signOut();
  }

  async getSession(): Promise<Session | null> {
    const { data } = (await this.supabase?.auth.getSession()) ?? { data: { session: null } };
    return data.session;
  }

  async getCurrentUser(): Promise<User | null> {
    const { data } = (await this.supabase?.auth.getUser()) ?? { data: { user: null } };
    return data.user;
  }

  async isAuthenticated(): Promise<boolean> {
    return Boolean(await this.getSession());
  }

  async getAccessToken(): Promise<string | null> {
    return (await this.getSession())?.access_token ?? null;
  }

  onAuthStateChange(callback: (session: Session | null) => void): () => void {
    const subscription = this.supabase?.auth.onAuthStateChange((_event, session) => callback(session));
    return () => subscription?.data.subscription.unsubscribe();
  }

  private createSupabaseClient(): SupabaseClient | null {
    if (!environment.supabaseUrl || !environment.supabaseAnonKey) {
      return null;
    }

    return createClient(environment.supabaseUrl, environment.supabaseAnonKey, {
      auth: {
        autoRefreshToken: true,
        detectSessionInUrl: true,
        persistSession: true,
      },
    });
  }

  private requireClient(): SupabaseClient | null {
    return this.supabase;
  }

  private missingSupabaseConfigResult(): AuthResult {
    return {
      success: false,
      message: 'Supabase Auth n’est pas configuré. Ajoutez SUPABASE_URL et SUPABASE_ANON_KEY dans Vercel.',
    };
  }

  private getAuthRedirectUrl(): string {
    if (typeof window === 'undefined') {
      return '/home';
    }

    return `${window.location.origin}/home`;
  }

  private toAuthResult(error: AuthError | null, successMessage?: string): AuthResult {
    if (!error) {
      return { success: true, message: successMessage };
    }

    return {
      success: false,
      message: this.translateAuthError(error.message),
    };
  }

  private translateAuthError(message: string): string {
    const normalized = message.toLowerCase();

    if (normalized.includes('invalid login credentials')) {
      return 'Email ou mot de passe incorrect.';
    }

    if (normalized.includes('email not confirmed')) {
      return 'Votre email doit être confirmé avant la connexion.';
    }

    if (normalized.includes('user already registered')) {
      return 'Un compte existe déjà avec cette adresse email.';
    }

    if (normalized.includes('password')) {
      return 'Le mot de passe ne respecte pas les règles de sécurité.';
    }

    return message;
  }
}
