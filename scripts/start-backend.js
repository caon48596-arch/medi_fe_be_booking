const { spawn, execSync } = require('child_process');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const isWin = process.platform === 'win32';

let wslKeeper = null;
if (isWin) {
  try {
    execSync('wsl -d Ubuntu bash -c "sudo service postgresql start && sudo service redis-server start"', {
      stdio: 'ignore',
      timeout: 10000,
    });
    wslKeeper = spawn('wsl', ['-d', 'Ubuntu', 'sleep', 'infinity'], { stdio: 'ignore' });
  } catch (err) {
    console.warn('[BACKEND] Note on auto-starting WSL services:', err.message);
  }
}

const cmd = isWin ? 'mvnw.cmd' : './mvnw';
const child = spawn(`${cmd} spring-boot:run`, {
  cwd: rootDir,
  stdio: 'inherit',
  shell: true,
  env: {
    ...process.env,
    JAVA_TOOL_OPTIONS: '-Duser.timezone=UTC',
  },
});

const cleanup = (code) => {
  if (wslKeeper) {
    try {
      wslKeeper.kill();
    } catch (e) {}
  }
  process.exit(code || 0);
};

child.on('exit', cleanup);
process.on('SIGINT', () => cleanup(0));
process.on('SIGTERM', () => cleanup(0));

