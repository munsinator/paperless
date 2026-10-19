import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function hasDuplicateTags(value: string): boolean {
    const tags = value.split(',').map(tag => tag.trim().toLocaleLowerCase()).filter(Boolean);
    return new Set(tags).size !== tags.length;
}

export function hasWhitespaceInTags(value: string): boolean {
    return value.split(',').some(tag => /\s/.test(tag.trim()));
}

export const noWhitespaceInTagsValidator: ValidatorFn = (
    control: AbstractControl
): ValidationErrors | null => {
    const value = String(control.value ?? '');
    return hasWhitespaceInTags(value) ? { whitespaceInTag: true } : null;
};

// ValidatorFn = Angular Validator; bestimmte Ein- und Ausgaben
export const duplicateTagsValidator: ValidatorFn = (
    // das Formular, das geprüft wird
    control: AbstractControl
): ValidationErrors | null => {
    // 
    const value = String(control.value ?? '');
    return hasDuplicateTags(value) ? { duplicateTags: true } : null;
};