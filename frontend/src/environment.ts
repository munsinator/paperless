import type { LogLevel } from './services/logger.service';

export const environment = {
  production: false,
  apiUrl: '/api',
  useFakeBackend: false,
  logLevel: 'debug' as LogLevel
};