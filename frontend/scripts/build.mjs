import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectDir = dirname(dirname(fileURLToPath(import.meta.url)));
const cliPath = join(projectDir, 'node_modules', '@angular', 'cli', 'bin', 'ng.js');

const result = spawnSync(process.execPath, [cliPath, 'build', '--progress=false'], {
  cwd: projectDir,
  env: {
    ...process.env,
    INIT_CWD: projectDir,
    PWD: projectDir,
    npm_config_prefix: projectDir,
  },
  stdio: 'inherit',
  shell: false,
});

process.exit(result.status ?? 1);
