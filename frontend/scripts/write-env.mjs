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

const content = `export const generatedEnvironment = {
  apiUrl: ${JSON.stringify(apiUrl)},
} as const;
`;

await mkdir(dirname(target), { recursive: true });
await writeFile(target, content);

console.log(`Generated Angular environment with API_URL=${apiUrl}`);
