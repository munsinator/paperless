export type DocumentType =
    | 'RECEIPT'
    | 'CONTRACT'
    | 'BANK_STATEMENT'
    | 'CERTIFICATE'
    | 'NOTE'
    | 'PASSPORT'
    | 'OTHER';

export interface Document {
    id: string;
    title: string;
    documentType?: DocumentType;
    createdAt: string;
    tags?: string[];
    summary?: string;
    ocrText?: string;
    fileUrl?: string;
}

export function documentTypeLabel(documentType: DocumentType | undefined): string {
    if (!documentType) return '';
    const labels: Record<DocumentType, string> = {
        RECEIPT: 'Rechnung',
        CONTRACT: 'Vertrag',
        BANK_STATEMENT: 'Bank / Finanzen',
        CERTIFICATE: 'Zertifikat',
        NOTE: 'Notiz',
        PASSPORT: 'Reisepass',
        OTHER: 'Sonstiges'
    };
    return labels[documentType];
}