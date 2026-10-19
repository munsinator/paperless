import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function hasDuplicateTags(value: string): boolean {
    const tags = value.split(',').map(tag => tag.trim().toLocaleLowerCase()).filter(Boolean);
    return new Set(tags).size !== tags.length;
}

// ValidatorFn = Angular Validator; bestimmte Ein- und Ausgaben
export const duplicateTagsValidator: ValidatorFn = (
    // das Formular, das geprüft wird
    control: AbstractControl
): ValidationErrors | null => {
    // 
    const value = String(control.value ?? '');
    return hasDuplicateTags(value) ? { duplicateTags: true } : null;
};