import { Injectable } from '@angular/core';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

@Injectable({ providedIn: 'root' })
export class TagService {
    private static readonly maxTags = 20;
    private static readonly maxTagLength = 50;

    readonly validator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        const value = String(control.value ?? '');
        if (!value.trim()) return null;

        const tags = value.split(',').map(tag => tag.trim());
        if (tags.some(tag => !tag)) return { emptyTag: true };
        if (tags.length > TagService.maxTags) return { tooManyTags: true };
        if (tags.some(tag => tag.length > TagService.maxTagLength)) return { tagTooLong: true };
        if (this.hasDuplicates(tags)) return { duplicateTags: true };
        if (tags.some(tag => /\s/.test(tag))) return { whitespaceInTag: true };
        return null;
    };

    parseTags(value: string): string[] {
        return value.split(',').map(tag => tag.trim()).filter(Boolean);
    }

    formatTags(tags: string[] | null | undefined): string {
        return tags?.join(', ') ?? '';
    }

    private hasDuplicates(tags: string[]): boolean {
        const normalizedTags = tags.map(tag => tag.toLocaleLowerCase());
        return new Set(normalizedTags).size !== normalizedTags.length;
    }
}
