import { Injectable } from '@angular/core';
import { AuthError, createClient, Provider, Session, SupabaseClient, User as SupabaseUser } from '@supabase/supabase-js';

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

export interface ConnectedProfile {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  initials: string;
  plan: string;
}

export interface UpdateProfileInput {
  fullName: string;
  email: string;
  avatarUrl: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly supabase: SupabaseClient | null = this.createSupabaseClient();

  async login(credentials: LoginCredentials): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    try {
      const { error } = await client.auth.signInWithPassword({
        email: credentials.email,
        password: credentials.password,
      });

      return this.toAuthResult(error, 'Connexion réussie.');
    } catch (error) {
      return this.toUnexpectedErrorResult(error);
    }
  }

  async signup(credentials: SignupCredentials): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    try {
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
    } catch (error) {
      return this.toUnexpectedErrorResult(error);
    }
  }

  async loginWithSocialProvider(provider: SocialProvider): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    try {
      const { error } = await client.auth.signInWithOAuth({
        provider: provider as Provider,
        options: {
          redirectTo: this.getAuthRedirectUrl(),
        },
      });

      return this.toAuthResult(error);
    } catch (error) {
      return this.toUnexpectedErrorResult(error);
    }
  }

  async logout(): Promise<void> {
    await this.supabase?.auth.signOut();
  }

  async getSession(): Promise<Session | null> {
    const { data } = (await this.supabase?.auth.getSession()) ?? { data: { session: null } };
    return data.session;
  }

  async getCurrentUser(): Promise<SupabaseUser | null> {
    const { data } = (await this.supabase?.auth.getUser()) ?? { data: { user: null } };
    return data.user;
  }

  async getCurrentProfile(): Promise<ConnectedProfile | null> {
    const user = await this.getCurrentUser();
    return user ? this.toConnectedProfile(user) : null;
  }

  async updateProfile(profile: UpdateProfileInput): Promise<AuthResult> {
    const client = this.requireClient();

    if (!client) {
      return this.missingSupabaseConfigResult();
    }

    try {
      const fullName = profile.fullName.trim();
      const email = profile.email.trim();
      const avatarUrl = profile.avatarUrl.trim();
      const { error } = await client.auth.updateUser({
        email,
        data: {
          full_name: fullName,
          name: fullName,
          avatar_url: avatarUrl,
          picture: avatarUrl,
        },
      });

      return this.toAuthResult(error, 'Profil mis à jour.');
    } catch (error) {
      return this.toUnexpectedErrorResult(error);
    }
  }

  async isAuthenticated(): Promise<boolean> {
    return Boolean(await this.getSession());
  }

  async isAdmin(): Promise<boolean> {
    const user = await this.getCurrentUser();

    if (!user) {
      return false;
    }

    return this.hasAdminRole(user.app_metadata ?? {}) || this.hasAdminRole(user.user_metadata ?? {});
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
      message: this.translateAuthError(this.extractErrorMessage(error)),
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

    if (normalized.includes('signup') && normalized.includes('disabled')) {
      return 'Les inscriptions email sont désactivées dans Supabase.';
    }

    if (normalized.includes('email provider') || normalized.includes('provider is not enabled')) {
      return 'Le provider Email n’est pas activé dans Supabase.';
    }

    if (normalized.includes('captcha')) {
      return 'Supabase demande une configuration captcha pour cette inscription.';
    }

    if (normalized.includes('password')) {
      return 'Le mot de passe ne respecte pas les règles de sécurité.';
    }

    if (!message || message === '{}') {
      return 'Supabase a refusé la demande. Vérifiez que le provider Email est activé et que les inscriptions sont autorisées.';
    }

    return message;
  }

  private toUnexpectedErrorResult(error: unknown): AuthResult {
    return {
      success: false,
      message: this.translateAuthError(this.extractErrorMessage(error)),
    };
  }

  private toConnectedProfile(user: SupabaseUser): ConnectedProfile {
    const metadata = user.user_metadata ?? {};
    const email = user.email ?? '';
    const name = this.firstText(metadata, ['full_name', 'name', 'user_name', 'preferred_username'])
      || this.nameFromEmail(email)
      || 'Voyageur';
    const avatarUrl = this.firstText(metadata, ['avatar_url', 'picture']);

    return {
      id: user.id,
      email,
      name,
      avatarUrl: avatarUrl || null,
      initials: this.initialsFromName(name),
      plan: this.firstText(metadata, ['plan']) || 'Découverte',
    };
  }

  private firstText(metadata: Record<string, unknown>, keys: string[]): string {
    for (const key of keys) {
      const value = metadata[key];

      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }

    return '';
  }

  private hasAdminRole(metadata: Record<string, unknown>): boolean {
    const role = this.firstText(metadata, ['role', 'user_role', 'plan']).toLowerCase();

    if (role === 'admin') {
      return true;
    }

    const roles = metadata['roles'];
    return Array.isArray(roles) && roles.some((value) => typeof value === 'string' && value.toLowerCase() === 'admin');
  }

  private nameFromEmail(email: string): string {
    return email.split('@')[0]?.replace(/[._-]+/g, ' ').trim() ?? '';
  }

  private initialsFromName(name: string): string {
    const initials = name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');

    return initials || 'NF';
  }

  private extractErrorMessage(error: unknown): string {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    if (typeof error === 'object' && error !== null) {
      const maybeMessage = 'message' in error ? error.message : undefined;

      if (typeof maybeMessage === 'string' && maybeMessage) {
        return maybeMessage;
      }

      try {
        return JSON.stringify(error);
      } catch {
        return '';
      }
    }

    return typeof error === 'string' ? error : '';
  }
}
