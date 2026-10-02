/**
 * API utility functions for MCP resources and intent-integration mappings.
 */

import { apiGet, apiPost, apiPut, apiDelete } from './api';

/**
 * MCP Resource types
 */
export type McpContentType = 'tool' | 'resource' | 'prompt' | 'schema' | 'capability';

/**
 * Integration mapping types
 */
export type IntegrationMappingType = 'mcp' | 'custom_api';

/**
 * MCP Resource interface
 */
export interface McpResource {
  id: number;
  contentType: McpContentType;
  name: string;
  description?: string;
  mcpIdentifier: string;
  parametersSchema?: any;
  examples?: any;
  category?: string;
  useCases?: string;
  priority: number;
  lastSyncedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Sync response interface
 */
export interface SyncResponse {
  status: 'queued' | 'processing' | 'completed' | 'failed';
  message: string;
  syncedAt?: string;
}

/**
 * Intent Integration Mapping interface
 */
export interface IntentIntegrationMapping {
  id: number;
  intentActionId: number;
  integrationType: IntegrationMappingType;
  mcpResourceId?: number;
  customApiMetadataId?: number;
  customApiIntegrationId?: number; // Backward compatibility - derived from metadata
  customApiMetadata?: {
    description?: string;
    useCases?: string;
    category?: string;
    priority?: number;
  };
  mcpResource?: McpResource;
  customApiIntegration?: any;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Create mapping request interface
 */
export interface CreateMappingRequest {
  integrationType: IntegrationMappingType;
  mcpResourceId?: number;
  customApiMetadataId?: number; // Changed from customApiIntegrationId
  // Backward compatibility - will map to customApiMetadataId
  customApiIntegrationId?: number;
}

/**
 * Sync MCP resources for an integration.
 * 
 * @param integrationId Integration ID
 * @returns Sync response
 */
export async function syncMcpResources(integrationId: number): Promise<SyncResponse> {
  return apiPost<SyncResponse>(`/rag/integrations/${integrationId}/sync`);
}

/**
 * Get MCP resources for an integration.
 * 
 * @param integrationId Integration ID
 * @param contentType Optional content type filter
 * @returns List of MCP resources
 */
export async function getMcpResources(
  integrationId: number,
  contentType?: McpContentType
): Promise<McpResource[]> {
  const url = contentType
    ? `/rag/integrations/${integrationId}/resources?contentType=${contentType}`
    : `/rag/integrations/${integrationId}/resources`;
  return apiGet<McpResource[]>(url);
}

/**
 * Update MCP resource metadata.
 * 
 * @param integrationId Integration ID
 * @param resourceId Resource ID
 * @param data Partial resource data to update (category, useCases, priority)
 * @returns Updated resource
 */
export async function updateMcpResource(
  integrationId: number,
  resourceId: number,
  data: { category?: string; useCases?: string; priority?: number }
): Promise<McpResource> {
  return apiPut<McpResource>(`/rag/integrations/${integrationId}/resources/${resourceId}`, data);
}

/**
 * Get intent-integration mappings for an intent action.
 * 
 * @param intentId Intent ID
 * @param actionId Action ID
 * @returns List of mappings
 */
export async function getIntentIntegrationMappings(
  intentId: number,
  actionId: number
): Promise<IntentIntegrationMapping[]> {
  return apiGet<IntentIntegrationMapping[]>(`/rag/intents/${intentId}/actions/${actionId}/mappings`);
}

/**
 * Create a new intent-integration mapping.
 * 
 * @param intentId Intent ID
 * @param actionId Action ID
 * @param mapping Mapping request
 * @returns Created mapping
 */
export async function createIntentIntegrationMapping(
  intentId: number,
  actionId: number,
  mapping: CreateMappingRequest
): Promise<IntentIntegrationMapping> {
  return apiPost<IntentIntegrationMapping>(
    `/rag/intents/${intentId}/actions/${actionId}/mappings`,
    mapping
  );
}

/**
 * Delete an intent-integration mapping.
 * 
 * @param intentId Intent ID
 * @param actionId Action ID
 * @param mappingId Mapping ID
 */
export async function deleteIntentIntegrationMapping(
  intentId: number,
  actionId: number,
  mappingId: number
): Promise<void> {
  return apiDelete(`/rag/intents/${intentId}/actions/${actionId}/mappings/${mappingId}`);
}

