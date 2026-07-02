import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const target = fileURLToPath(new URL('../src/environments/environment.generated.ts', import.meta.url));
const envPath = resolve(dirname(target), '../../.env');

try {
  const envFile = await readFile(envPath, 'utf8');

  for (const line of envFile.split(/\r?\n/)) {
    const trimmed = line.trim();

    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }

    const separatorIndex = trimmed.indexOf('=');

    if (separatorIndex === -1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    let value = trimmed.slice(separatorIndex + 1).trim();

    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }

    process.env[key] ??= value;
  }
} catch (error) {
  if (error.code !== 'ENOENT') {
    throw error;
  }
}

const apiUrl = process.env.API_URL || process.env.NG_APP_API_URL || 'http://localhost:8080';
const supabaseUrl = process.env.SUPABASE_URL || process.env.NG_APP_SUPABASE_URL || '';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || process.env.NG_APP_SUPABASE_ANON_KEY || '';
const googleClientId = process.env.GOOGLE_CLIENT_ID || process.env.NG_APP_GOOGLE_CLIENT_ID || '';
const googleApiKey = process.env.GOOGLE_API_KEY || process.env.NG_APP_GOOGLE_API_KEY || '';

const content = `export const generatedEnvironment = {
  apiUrl: ${JSON.stringify(apiUrl)},
  supabaseUrl: ${JSON.stringify(supabaseUrl)},
  supabaseAnonKey: ${JSON.stringify(supabaseAnonKey)},
  googleClientId: ${JSON.stringify(googleClientId)},
  googleApiKey: ${JSON.stringify(googleApiKey)},
} as const;
`;

await mkdir(dirname(target), { recursive: true });
await writeFile(target, content);

console.log(`Generated Angular environment with API_URL=${apiUrl}`);
