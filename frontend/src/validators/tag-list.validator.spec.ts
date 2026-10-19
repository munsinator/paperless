import { FormControl } from '@angular/forms';
import {
    duplicateTagsValidator,
    hasDuplicateTags,
    hasWhitespaceInTags,
    noWhitespaceInTagsValidator
} from './tag-list.validator';

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

    it('detects whitespace inside a tag but allows spaces around comma-separated tags', () => {
        expect(hasWhitespaceInTags('Bank Statement, Rechnung')).toBe(true);
        expect(hasWhitespaceInTags('BankStatement, Rechnung')).toBe(false);
    });

    it('updates the duplicate error as the value changes', () => {
        const control = new FormControl('Studium', duplicateTagsValidator);

        control.setValue('Studium, Studium');

        expect(control.errors).toEqual({ duplicateTags: true });
    });

    it('marks tags containing internal whitespace invalid', () => {
        const control = new FormControl('Bank Statement', noWhitespaceInTagsValidator);

        expect(control.errors).toEqual({ whitespaceInTag: true });
    });
});