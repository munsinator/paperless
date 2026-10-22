import { Injectable } from '@angular/core';
import { environment } from '../environment';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LEVEL_ORDER: Record<LogLevel, number> = { debug: 0, info: 1, warn: 2, error: 3 };

// zentrale Stelle für Log-Ausgaben; unterhalb von environment.logLevel wird nichts ausgegeben
// debug = Details für die Entwicklung, info = wichtige Aktionen, warn = abgefangene Probleme, error = Fehler
@Injectable({ providedIn: 'root' })
export class LoggerService {
    private readonly minLevel = LEVEL_ORDER[environment.logLevel];

    // ... = Rest-Parameter; beliebig viele zusätzliche Informationen; unterschiedliche Datentypen
    debug(source: string, message: string, ...details: unknown[]): void {
        this.write('debug', source, message, details);
    }

    info(source: string, message: string, ...details: unknown[]): void {
        this.write('info', source, message, details);
    }

    warn(source: string, message: string, ...details: unknown[]): void {
        this.write('warn', source, message, details);
    }

    error(source: string, message: string, ...details: unknown[]): void {
        this.write('error', source, message, details);
    }

    // z.B. 2026-10-21T08:30:00.000Z INFO [DocumentService] Document loaded
    private write(level: LogLevel, source: string, message: string, details: unknown[]): void {
        if (LEVEL_ORDER[level] < this.minLevel) return;

        const line = `${new Date().toISOString()} ${level.toUpperCase()} [${source}] ${message}`;
        console[level](line, ...details);
    }
}
