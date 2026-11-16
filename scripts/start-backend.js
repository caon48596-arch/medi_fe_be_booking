const { spawn } = require('child_process');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const isWin = process.platform === 'win32';
const cmd = isWin ? 'mvnw.cmd' : './mvnw';

const child = spawn(`${cmd} spring-boot:run`, {
  cwd: rootDir,
  stdio: 'inherit',
  shell: true,
});

child.on('exit', (code) => {
  process.exit(code || 0);
});
