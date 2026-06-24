import { generatedEnvironment } from './environment.generated';

export const environment = {
  production: false,
  apiUrl: generatedEnvironment.apiUrl,
} as const;
