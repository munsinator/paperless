import type { LogLevel } from './services/logger.service';

export const environment = {
  production: false,
  apiUrl: '/api',
  logLevel: 'debug' as LogLevel
};