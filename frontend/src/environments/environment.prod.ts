// Variables must be provided at build time (via Vercel env vars or a local .env.local file).
// Missing variables cause an immediate error when the app loads.
const _env: Record<string, string | undefined> = {
  NG_APP_API_URL: import.meta.env['NG_APP_API_URL'],
};

function requireEnv(key: string): string {
  const value = _env[key];
  if (!value) throw new Error(`[Build] Missing required env variable: ${key}`);
  return value;
}

export const environment = {
  production: true,
  apiBaseUrl: requireEnv('NG_APP_API_URL'),
};
