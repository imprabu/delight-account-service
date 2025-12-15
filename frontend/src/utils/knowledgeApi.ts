/**
 * API service for Knowledge Base endpoints
 */

import { apiGet, apiPost, apiPut, apiDelete, apiRequest } from './api';

export interface Document {
  id: number;
  title: string;
  type: 'TEXT' | 'PDF';
  embeddingStatus: 'COMPLETED' | 'PROCESSING' | 'PENDING' | 'FAILED';
  fileSize?: number;
  createdAt: string;
  updatedAt: string;
  tags?: string[];
  tokenCount?: number;
  vectorId?: string;
  content?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Stats {
  totalDocuments: number;
  completedDocuments: number;
  processingDocuments: number;
  failedDocuments: number;
  articlesCount: number;
  answerFoundPercentage: number;
  averageSearchTimeMs: number;
}

export interface DocumentFilters {
  page?: number;
  size?: number;
  type?: 'TEXT' | 'PDF';
  status?: 'COMPLETED' | 'PROCESSING' | 'PENDING' | 'FAILED';
  search?: string;
  tags?: string; // comma-separated
}

export interface BotConfig {
  id?: number;
  botName: string;
  welcomeMessage: string;
  language: string;
  autoDetectLanguage: boolean;
  aiMode: 'MANAGED' | 'OWN';
  managedModelId?: number;
  personalityInstructions: string;
  showTypingIndicator: boolean;
  showSourceDocuments: boolean;
  rememberConversation: boolean;
  conversationMemoryLimit: number;
  maxResults?: number;
  answerQualityThreshold?: number;
  splitMethod?: string;
  articleSize?: string;
  autoUpdateDocuments?: boolean;
}

export interface SourceDocument {
  documentId: number;
  title: string;
  similarity: number;
  chunkId?: string;
}

export interface QueryResponse {
  queryId: number;
  query?: string; // The original query text
  status: string;
  response?: string;
  sourceDocuments?: SourceDocument[];
  contextDocuments?: string; // JSON string for backward compatibility
  similarityScores?: string; // JSON string for backward compatibility
  tokenCount?: number;
  costUsd?: number;
  processingTimeMs?: number;
  errorMessage?: string;
  createdAt?: string; // Timestamp for history display
}


class KnowledgeAPI {
  private baseURL = '/rag/knowledge';

  async getDocuments(filters: DocumentFilters = {}): Promise<PaginatedResponse<Document>> {
    const params = new URLSearchParams();
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());
    if (filters.type) params.append('type', filters.type);
    if (filters.status) params.append('status', filters.status);
    if (filters.search) params.append('search', filters.search);
    if (filters.tags) params.append('tags', filters.tags);

    const queryString = params.toString();
    const url = queryString ? `${this.baseURL}?${queryString}` : this.baseURL;
    return apiGet<PaginatedResponse<Document>>(url);
  }

  async getDocument(id: number): Promise<Document> {
    return apiGet<Document>(`${this.baseURL}/${id}`);
  }

  async uploadDocument(file: File, title: string): Promise<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);

    // apiRequest will automatically detect FormData and not set Content-Type
    return apiRequest<Document>(`${this.baseURL}/upload`, {
      method: 'POST',
      body: formData,
    });
  }

  async createDocument(title: string, content: string): Promise<Document> {
    return apiPost<Document>(this.baseURL, { title, content });
  }

  async updateDocument(id: number, title: string, content: string): Promise<Document> {
    return apiPut<Document>(`${this.baseURL}/${id}`, { title, content });
  }

  async deleteDocument(id: number): Promise<void> {
    return apiDelete(`${this.baseURL}/${id}`);
  }

  async permanentlyDeleteDocument(id: number): Promise<void> {
    return apiDelete(`${this.baseURL}/${id}/permanent`);
  }

  async getStats(): Promise<Stats> {
    return apiGet<Stats>(`${this.baseURL}/stats`);
  }

  async addTags(id: number, tags: string[]): Promise<Document> {
    return apiPost<Document>(`${this.baseURL}/${id}/tags`, { tags });
  }

  async removeTag(id: number, tagName: string): Promise<void> {
    return apiDelete(`${this.baseURL}/${id}/tags/${encodeURIComponent(tagName)}`);
  }

  async getUploadInfo() {
    return apiGet(`${this.baseURL}/upload/info`);
  }

  // Bot config endpoints
  async getBotConfig(): Promise<BotConfig> {
    return apiGet<BotConfig>('/rag/bot');
  }

  async updateBotConfig(config: Partial<BotConfig>): Promise<BotConfig> {
    return apiPut<BotConfig>('/rag/bot', config);
  }

  // Test query endpoint
  async testQuery(
    query: string, 
    tags?: string[], 
    tagFilterMode?: 'AND' | 'OR'
  ): Promise<QueryResponse> {
    const body: any = { query };
    if (tags && tags.length > 0) {
      body.tags = tags;
      body.tagFilterMode = tagFilterMode || 'OR';
    }
    return apiPost<QueryResponse>('/rag/query', body);
  }

  // Query history endpoints
  async getQueryHistory(page: number = 0, size: number = 20): Promise<PaginatedResponse<QueryResponse>> {
    return apiGet<PaginatedResponse<QueryResponse>>(`/rag/queries?page=${page}&size=${size}`);
  }

  async getQueryById(id: number): Promise<QueryResponse> {
    return apiGet<QueryResponse>(`/rag/query/${id}`);
  }


  // Tag endpoints
  async getAllTags(): Promise<string[]> {
    return apiGet<string[]>(`${this.baseURL}/tags`);
  }
}

export const knowledgeAPI = new KnowledgeAPI();

