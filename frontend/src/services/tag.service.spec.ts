import { FormControl } from '@angular/forms';
import { TagService } from './tag.service';

describe('TagService', () => {
    const tagService = new TagService();

    it('parses and formats comma-separated tags consistently', () => {
        expect(tagService.parseTags(' Studium, Rechnung ')).toEqual(['Studium', 'Rechnung']);
        expect(tagService.formatTags(['Studium', 'Rechnung'])).toBe('Studium, Rechnung');
    });

    it('returns no tags for empty input', () => {
        expect(tagService.parseTags('')).toEqual([]);
        expect(tagService.formatTags([])).toBe('');
    });

    it('detects duplicate tags regardless of case or surrounding whitespace', () => {
        const control = new FormControl('Studium,  studium ', tagService.validator);

        expect(control.errors).toEqual({ duplicateTags: true });
    });

    it('rejects empty entries between commas', () => {
        const control = new FormControl('Studium, , Rechnung', tagService.validator);

        expect(control.errors).toEqual({ emptyTag: true });
    });

    it('rejects too many tags and tags that exceed the length limit', () => {
        const tooManyTags = new FormControl(
            Array.from({ length: 21 }, (_, index) => `Tag${index}`).join(','),
            tagService.validator
        );
        const tagTooLong = new FormControl('a'.repeat(51), tagService.validator);

        expect(tooManyTags.errors).toEqual({ tooManyTags: true });
        expect(tagTooLong.errors).toEqual({ tagTooLong: true });
    });

    it('rejects internal whitespace but permits separator whitespace around commas', () => {
        const internalWhitespace = new FormControl('Bank Statement', tagService.validator);
        const separatorWhitespace = new FormControl('BankStatement, Rechnung', tagService.validator);

        expect(internalWhitespace.errors).toEqual({ whitespaceInTag: true });
        expect(separatorWhitespace.valid).toBe(true);
    });

    it('revalidates duplicate tags whenever the control value changes', () => {
        const control = new FormControl('Studium', tagService.validator);

        control.setValue('Studium, Studium');

        expect(control.errors).toEqual({ duplicateTags: true });
    });
});
