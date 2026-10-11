import { FormControl } from '@angular/forms';
import { duplicateTagsValidator, hasDuplicateTags } from './tag-list.validator';

describe('duplicateTagsValidator', () => {
    it('detects repeated tags regardless of whitespace and letter case', () => {
        expect(hasDuplicateTags('Studium,  studium ')).toBe(true);
    });

    it('allows distinct tags', () => {
        expect(hasDuplicateTags('Studium, Rechnung')).toBe(false);
    });

    it('treats empty tag entries as a separate validation concern', () => {
        expect(hasDuplicateTags('Studium, , Rechnung')).toBe(false);
    });

    it('marks a form control invalid when tags are repeated', () => {
        const control = new FormControl('Studium, STUDIUM', duplicateTagsValidator);

        expect(control.errors).toEqual({ duplicateTags: true });
    });
});