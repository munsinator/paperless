export enum DocumentType {
    RECEIPT = 'RECEIPT',
    CONTRACT = 'CONTRACT',
    BANK_STATEMENT = 'BANK_STATEMENT',
    CERTIFICATE = 'CERTIFICATE',
    NOTE = 'NOTE',
    PASSPORT = 'PASSPORT',
    OTHER = 'OTHER'
}

const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
    [DocumentType.RECEIPT]: 'Rechnung',
    [DocumentType.CONTRACT]: 'Vertrag',
    [DocumentType.BANK_STATEMENT]: 'Bank / Finanzen',
    [DocumentType.CERTIFICATE]: 'Zertifikat',
    [DocumentType.NOTE]: 'Notiz',
    [DocumentType.PASSPORT]: 'Reisepass',
    [DocumentType.OTHER]: 'Sonstiges'
};

export function getDocumentTypeLabel(documentType: DocumentType | undefined): string {
    return documentType ? DOCUMENT_TYPE_LABELS[documentType] : '';
}

export const DOCUMENT_TYPE_OPTIONS = Object.values(DocumentType).map(value => ({
    label: getDocumentTypeLabel(value),
    value
}));

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