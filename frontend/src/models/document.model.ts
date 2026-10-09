export interface Document {
    id: string;
    title: string;
    category?: string;
    createdAt: string;
    tags?: string[];
    summary?: string;
    ocrText?: string;
    fileUrl?: string;
}