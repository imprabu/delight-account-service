import { useState, useEffect } from 'react';
import { Plus, Check, ExternalLink, Zap, Settings as SettingsIcon, Play, Edit, Trash2, X, Eye, EyeOff, CheckCircle, AlertCircle, Loader, Code, Link2, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api';
import { syncMcpResources, getMcpResources, McpResource } from '../utils/integrationsApi';

interface IntegrationsProps {
  onNavigate: (screen: string) => void;
}

interface Integration {
  id: number;
  name: string;
  description?: string;
  integrationType: 'mcp' | 'custom_api';
  status: 'active' | 'inactive' | 'error';
  usedByCount: number;
  lastTestedAt?: string;
  lastTestStatus?: 'success' | 'failed' | 'not_tested';
  lastTestError?: string;
  serverUrl?: string; // MCP only
  method?: string; // Custom API only
  endpoint?: string; // Custom API only
  authType?: string; // Custom API only
  metadata?: any;
  resources?: McpResource[]; // MCP resources
  lastSyncedAt?: string; // Last sync time for MCP
  syncStatus?: 'idle' | 'syncing' | 'success' | 'error'; // Sync status
}

export function Integrations({ onNavigate }: IntegrationsProps) {
  const [selectedTab, setSelectedTab] = useState<'mcps' | 'apis'>('mcps');
  const [showAddModal, setShowAddModal] = useState(false);
  const [integrations, setIntegrations] = useState<Integration[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingIntegration, setEditingIntegration] = useState<Integration | null>(null);
  const [testingIntegrationId, setTestingIntegrationId] = useState<number | null>(null);
  const [syncingIntegrationId, setSyncingIntegrationId] = useState<number | null>(null);
  const [expandedIntegrations, setExpandedIntegrations] = useState<Set<number>>(new Set());
  const [selectedResource, setSelectedResource] = useState<McpResource | null>(null);

  // Fetch integrations on mount and when tab changes
  useEffect(() => {
    fetchIntegrations();
  }, [selectedTab]);

  const fetchIntegrations = async () => {
    try {
      setLoading(true);
      setError(null);
      const type = selectedTab === 'mcps' ? 'mcp' : 'custom_api';
      const response = await apiGet<Integration[]>(`/rag/integrations?integrationType=${type}`);
      setIntegrations(response || []);
    } catch (err: any) {
      console.error('Error fetching integrations:', err);
      setError(err.message || 'Failed to load integrations');
      setIntegrations([]);
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async (id: number) => {
    try {
      setTestingIntegrationId(id);
      const response = await apiPost<{ status: string; message: string }>(`/rag/integrations/${id}/test`);
      
      if (response.status === 'success') {
        // Refresh integrations to get updated test status
        await fetchIntegrations();
        alert('Connection test successful!');
      } else {
        alert(`Connection test failed: ${response.message}`);
      }
    } catch (err: any) {
      console.error('Error testing connection:', err);
      alert(`Failed to test connection: ${err.message}`);
    } finally {
      setTestingIntegrationId(null);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this integration?')) {
      return;
    }

    try {
      await apiDelete(`/rag/integrations/${id}`);
      await fetchIntegrations();
      alert('Integration deleted successfully');
    } catch (err: any) {
      console.error('Error deleting integration:', err);
      alert(`Failed to delete integration: ${err.message}`);
    }
  };

  const handleSaveIntegration = async () => {
    await fetchIntegrations();
    setShowAddModal(false);
    setEditingIntegration(null);
  };

  const handleEdit = (integration: Integration) => {
    setEditingIntegration(integration);
    setShowAddModal(true);
  };

  const handleSyncResources = async (integrationId: number) => {
    try {
      setSyncingIntegrationId(integrationId);
      const response = await syncMcpResources(integrationId);
      
      // Update integration sync status
      setIntegrations(prev => prev.map(integration => 
        integration.id === integrationId
          ? { ...integration, syncStatus: 'syncing' as const }
          : integration
      ));
      
      // Wait a bit then refresh to get updated resources
      setTimeout(async () => {
        await fetchIntegrations();
        await loadResourcesForIntegration(integrationId);
      }, 2000);
      
      alert('MCP resource sync started. Resources will be updated shortly.');
    } catch (err: any) {
      console.error('Error syncing resources:', err);
      alert(`Failed to sync resources: ${err.message}`);
      
      // Update integration sync status to error
      setIntegrations(prev => prev.map(integration => 
        integration.id === integrationId
          ? { ...integration, syncStatus: 'error' as const }
          : integration
      ));
    } finally {
      setSyncingIntegrationId(null);
    }
  };

  const loadResourcesForIntegration = async (integrationId: number) => {
    try {
      const resources = await getMcpResources(integrationId);
      setIntegrations(prev => prev.map(integration => 
        integration.id === integrationId
          ? { ...integration, resources, syncStatus: 'success' as const }
          : integration
      ));
    } catch (err: any) {
      console.error('Error loading resources:', err);
    }
  };

  const toggleResourcesExpanded = async (integrationId: number) => {
    const newExpanded = new Set(expandedIntegrations);
    if (newExpanded.has(integrationId)) {
      newExpanded.delete(integrationId);
    } else {
      newExpanded.add(integrationId);
      // Load resources if not already loaded
      const integration = integrations.find(i => i.id === integrationId);
      if (integration && (!integration.resources || integration.resources.length === 0)) {
        await loadResourcesForIntegration(integrationId);
      }
    }
    setExpandedIntegrations(newExpanded);
  };

  const connectedMCPs = integrations.filter(i => i.integrationType === 'mcp');
  const customAPIs = integrations.filter(i => i.integrationType === 'custom_api');

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Integrations</h1>
          <p className="text-gray-600 text-sm md:text-base">Connect tools and APIs that your AI assistant can use</p>
        </div>
        <button 
          onClick={() => {
            setEditingIntegration(null);
            setShowAddModal(true);
          }}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto"
        >
          <Plus className="w-5 h-5" />
          Add Integration
        </button>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-xl p-4 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <div className="text-red-900 mb-1">Error Loading Integrations</div>
            <div className="text-sm text-red-700">{error}</div>
            <button
              onClick={fetchIntegrations}
              className="mt-2 text-sm text-red-700 underline hover:text-red-900"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Info Banner */}
      <div className="mb-6 bg-gradient-to-r from-purple-50 to-orange-50 border border-purple-200 rounded-xl p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 bg-purple-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <div className="flex-1">
            <h3 className="text-gray-900 mb-2">How Integrations Work</h3>
            <p className="text-gray-700 text-sm mb-3">
              When your AI assistant detects what a customer needs, it can automatically take actions like 
              creating support tickets, looking up order status, or notifying your team - all without manual work.
            </p>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="flex items-start gap-2">
                <Code className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                <div>
                  <span className="text-gray-900 block mb-1">MCP Tools</span>
                  <span className="text-gray-600 text-xs">Pre-built tools from external services</span>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Link2 className="w-4 h-4 text-orange-600 mt-0.5 flex-shrink-0" />
                <div>
                  <span className="text-gray-900 block mb-1">Custom APIs</span>
                  <span className="text-gray-600 text-xs">Your own web services and endpoints</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200">
        <div className="flex gap-6">
          <button
            onClick={() => setSelectedTab('mcps')}
            className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
              selectedTab === 'mcps'
                ? 'border-purple-600 text-purple-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            <Code className="w-4 h-4" />
            MCP Tools
          </button>
          <button
            onClick={() => setSelectedTab('apis')}
            className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
              selectedTab === 'apis'
                ? 'border-purple-600 text-purple-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            <Link2 className="w-4 h-4" />
            Custom APIs
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader className="w-8 h-8 animate-spin text-gray-400" />
          <span className="ml-3 text-gray-600">Loading integrations...</span>
        </div>
      ) : selectedTab === 'mcps' ? (
        <div className="space-y-6">
          {/* Connected MCPs */}
          <div>
            <h2 className="text-gray-900 mb-4">Connected MCP Tools ({connectedMCPs.length})</h2>
            <div className="space-y-4">
              {connectedMCPs.map((mcp) => (
                <div key={mcp.id} className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-start gap-4 flex-1">
                      <div className="w-12 h-12 bg-purple-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <Code className="w-6 h-6 text-purple-600" />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="text-gray-900">{mcp.name}</h3>
                          <span className={`px-2 py-1 rounded text-xs flex items-center gap-1 ${
                            mcp.status === 'active' ? 'bg-green-50 text-green-700' :
                            mcp.status === 'error' ? 'bg-red-50 text-red-700' :
                            'bg-gray-50 text-gray-700'
                          }`}>
                            {mcp.lastTestStatus === 'success' && <Check className="w-3 h-3" />}
                            {mcp.status === 'active' ? 'Connected' : mcp.status === 'error' ? 'Error' : 'Inactive'}
                          </span>
                        </div>
                        {mcp.description && (
                          <p className="text-gray-600 text-sm mb-2">{mcp.description}</p>
                        )}
                        {mcp.serverUrl && (
                          <div className="text-xs text-gray-500 font-mono bg-gray-50 px-2 py-1 rounded inline-block mb-2">
                            {mcp.serverUrl}
                          </div>
                        )}
                        <div className="flex items-center gap-2 text-xs text-gray-500">
                          <span>{mcp.resources?.length || 0} synced resources</span>
                          <span>•</span>
                          <span>Used in {mcp.usedByCount || 0} intents</span>
                          {mcp.lastSyncedAt && (
                            <>
                              <span>•</span>
                              <span>Last synced: {new Date(mcp.lastSyncedAt).toLocaleDateString()}</span>
                            </>
                          )}
                          {mcp.lastTestedAt && (
                            <>
                              <span>•</span>
                              <span>Last tested: {new Date(mcp.lastTestedAt).toLocaleDateString()}</span>
                            </>
                          )}
                        </div>
                        {mcp.lastTestError && (
                          <div className="mt-2 text-xs text-red-600 bg-red-50 px-2 py-1 rounded">
                            {mcp.lastTestError}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleSyncResources(mcp.id)}
                        disabled={syncingIntegrationId === mcp.id}
                        className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2 disabled:opacity-50"
                        title="Sync MCP Resources"
                      >
                        {syncingIntegrationId === mcp.id ? (
                          <Loader className="w-4 h-4 animate-spin" />
                        ) : (
                          <RefreshCw className="w-4 h-4" />
                        )}
                        Sync
                      </button>
                      <button
                        onClick={() => handleTestConnection(mcp.id)}
                        disabled={testingIntegrationId === mcp.id}
                        className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2 disabled:opacity-50"
                      >
                        {testingIntegrationId === mcp.id ? (
                          <Loader className="w-4 h-4 animate-spin" />
                        ) : (
                          <Play className="w-4 h-4" />
                        )}
                        Test
                      </button>
                      <button
                        onClick={() => handleEdit(mcp)}
                        className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                      >
                        Settings
                      </button>
                      <button
                        onClick={() => handleDelete(mcp.id)}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <Trash2 className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                  </div>

                  {/* MCP Resources Section */}
                  <div className="border-t border-gray-200 pt-4 mt-4">
                    <button
                      onClick={() => toggleResourcesExpanded(mcp.id)}
                      className="w-full flex items-center justify-between text-sm text-gray-700 hover:text-gray-900 mb-2"
                    >
                      <span className="font-medium">
                        Synced Resources ({mcp.resources?.length || 0})
                      </span>
                      {expandedIntegrations.has(mcp.id) ? (
                        <ChevronUp className="w-4 h-4" />
                      ) : (
                        <ChevronDown className="w-4 h-4" />
                      )}
                    </button>
                    
                    {expandedIntegrations.has(mcp.id) && (
                      <div className="mt-3 space-y-3">
                        {mcp.resources && mcp.resources.length > 0 ? (
                          <>
                            {/* Group by content type */}
                            {['tool', 'resource', 'prompt', 'schema', 'capability'].map((type) => {
                              const typeResources = mcp.resources!.filter(r => r.contentType === type);
                              if (typeResources.length === 0) return null;
                              
                              return (
                                <div key={type} className="bg-gray-50 rounded-lg p-3">
                                  <div className="text-xs font-semibold text-gray-600 mb-2 uppercase">
                                    {type}s ({typeResources.length})
                                  </div>
                                  <div className="space-y-2">
                                    {typeResources.map((resource) => (
                                      <div
                                        key={resource.id}
                                        onClick={() => setSelectedResource(resource)}
                                        className="bg-white border border-gray-200 rounded-lg p-3 cursor-pointer hover:border-purple-300 transition-colors"
                                      >
                                        <div className="flex items-start justify-between">
                                          <div className="flex-1">
                                            <div className="flex items-center gap-2 mb-1">
                                              <span className="font-medium text-sm text-gray-900">{resource.name}</span>
                                              {resource.category && (
                                                <span className="px-2 py-0.5 bg-purple-100 text-purple-700 rounded text-xs">
                                                  {resource.category}
                                                </span>
                                              )}
                                              {resource.priority > 0 && (
                                                <span className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
                                                  Priority: {resource.priority}
                                                </span>
                                              )}
                                            </div>
                                            {resource.description && (
                                              <p className="text-xs text-gray-600 line-clamp-2">{resource.description}</p>
                                            )}
                                          </div>
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              );
                            })}
                          </>
                        ) : (
                          <div className="text-sm text-gray-500 text-center py-4">
                            No resources synced yet. Click "Sync" to fetch resources from the MCP server.
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Empty State */}
          {connectedMCPs.length === 0 && (
            <div className="bg-white rounded-xl border-2 border-dashed border-gray-300 p-12 text-center">
              <div className="w-16 h-16 bg-purple-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Code className="w-8 h-8 text-purple-600" />
              </div>
              <h3 className="text-gray-900 mb-2">No MCP Tools Connected</h3>
              <p className="text-gray-600 mb-4">Connect your first MCP tool to give your AI assistant more capabilities</p>
              <button 
                onClick={() => {
                  setEditingIntegration(null);
                  setShowAddModal(true);
                }}
                className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
              >
                Add MCP Tool
              </button>
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-6">
          {/* Custom APIs */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-gray-900">Your Custom APIs ({customAPIs.length})</h2>
            </div>
            <div className="space-y-4">
              {customAPIs.map((api) => (
                <div key={api.id} className="bg-white rounded-xl border border-gray-200 p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-start gap-4 flex-1">
                      <div className="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                        <Link2 className="w-6 h-6 text-blue-600" />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="text-gray-900">{api.name}</h3>
                          <span className={`px-2 py-0.5 rounded text-xs font-mono ${
                            api.method === 'GET' ? 'bg-green-100 text-green-700' :
                            api.method === 'POST' ? 'bg-blue-100 text-blue-700' :
                            api.method === 'PUT' ? 'bg-orange-100 text-orange-700' :
                            api.method === 'PATCH' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-red-100 text-red-700'
                          }`}>
                            {api.method}
                          </span>
                          <span className={`px-2 py-0.5 rounded text-xs ${
                            api.status === 'active' ? 'bg-green-50 text-green-700' :
                            api.status === 'error' ? 'bg-red-50 text-red-700' :
                            'bg-gray-50 text-gray-700'
                          }`}>
                            {api.status === 'active' ? 'Active' : api.status === 'error' ? 'Error' : 'Inactive'}
                          </span>
                          {api.authType && api.authType !== 'none' && (
                            <span className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs">
                              {api.authType}
                            </span>
                          )}
                        </div>
                        {api.description && (
                          <p className="text-gray-600 text-sm mb-2">{api.description}</p>
                        )}
                        {api.endpoint && (
                          <div className="font-mono text-sm text-gray-700 bg-gray-50 px-3 py-2 rounded border border-gray-200 mb-2">
                            {api.endpoint}
                          </div>
                        )}
                        <div className="text-xs text-gray-500">
                          Used in {api.usedByCount || 0} intents
                          {api.lastTestedAt && (
                            <> • Last tested: {new Date(api.lastTestedAt).toLocaleDateString()}</>
                          )}
                        </div>
                        {api.lastTestError && (
                          <div className="mt-2 text-xs text-red-600 bg-red-50 px-2 py-1 rounded">
                            {api.lastTestError}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleTestConnection(api.id)}
                        disabled={testingIntegrationId === api.id}
                        className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2 disabled:opacity-50"
                      >
                        {testingIntegrationId === api.id ? (
                          <Loader className="w-4 h-4 animate-spin" />
                        ) : (
                          <Play className="w-4 h-4" />
                        )}
                        Test
                      </button>
                      <button
                        onClick={() => handleEdit(api)}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <Edit className="w-4 h-4 text-gray-600" />
                      </button>
                      <button
                        onClick={() => handleDelete(api.id)}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <Trash2 className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Empty State */}
          {customAPIs.length === 0 && (
            <div className="bg-white rounded-xl border-2 border-dashed border-gray-300 p-12 text-center">
              <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Link2 className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-gray-900 mb-2">No Custom APIs</h3>
              <p className="text-gray-600 mb-4">Add your own API endpoints to connect your services</p>
              <button 
                onClick={() => {
                  setEditingIntegration(null);
                  setShowAddModal(true);
                }}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Add Custom API
              </button>
            </div>
          )}
        </div>
      )}

      {/* Help Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-6 mt-6">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <Zap className="w-5 h-5 text-white" />
          </div>
          <div>
            <h3 className="text-gray-900 mb-2">How to Use Integrations</h3>
            <p className="text-gray-700 text-sm mb-3">
              Connect your tools here, then configure them in the "Intents" section to tell your bot when to use them. 
              For example: "When a customer asks about orders, use the Check Order Status API."
            </p>
            <div className="space-y-2 text-sm">
              <div className="flex items-center gap-2 text-gray-700">
                <div className="w-1.5 h-1.5 bg-blue-600 rounded-full" />
                <span><strong>MCP Tools:</strong> Connect to external services like Zendesk, Slack, Salesforce</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700">
                <div className="w-1.5 h-1.5 bg-blue-600 rounded-full" />
                <span><strong>Custom APIs:</strong> Connect to your own website, database, or internal systems</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Add/Edit Integration Modal */}
      {showAddModal && (
        <AddIntegrationModal
          integration={editingIntegration}
          integrationType={editingIntegration?.integrationType || (selectedTab === 'mcps' ? 'mcp' : 'custom_api')}
          onClose={() => {
            setShowAddModal(false);
            setEditingIntegration(null);
          }}
          onSave={handleSaveIntegration}
        />
      )}

      {/* Resource Details Modal */}
      {selectedResource && (
        <ResourceDetailsModal
          resource={selectedResource}
          onClose={() => setSelectedResource(null)}
        />
      )}
    </div>
  );
}

// Resource Details Modal Component
interface ResourceDetailsModalProps {
  resource: McpResource;
  onClose: () => void;
}

function ResourceDetailsModal({ resource, onClose }: ResourceDetailsModalProps) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white rounded-xl max-w-2xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 md:p-6 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-gray-900 text-lg md:text-xl mb-1">{resource.name}</h2>
            <p className="text-gray-600 text-sm">MCP Resource Details</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Content Type</label>
              <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900 capitalize">
                {resource.contentType}
              </div>
            </div>

            {resource.description && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900">
                  {resource.description}
                </div>
              </div>
            )}

            {resource.category && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900">
                  {resource.category}
                </div>
              </div>
            )}

            {resource.useCases && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Use Cases</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900 whitespace-pre-wrap">
                  {resource.useCases}
                </div>
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900">
                  {resource.priority}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">MCP Identifier</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900 font-mono">
                  {resource.mcpIdentifier}
                </div>
              </div>
            </div>

            {resource.lastSyncedAt && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last Synced</label>
                <div className="px-3 py-2 bg-gray-50 rounded-lg text-sm text-gray-900">
                  {new Date(resource.lastSyncedAt).toLocaleString()}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 flex justify-end flex-shrink-0">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

// Add Integration Modal Component
interface AddIntegrationModalProps {
  integration?: Integration | null;
  integrationType: 'mcp' | 'custom_api';
  onClose: () => void;
  onSave: () => void;
}

function AddIntegrationModal({ integration, integrationType: initialType, onClose, onSave }: AddIntegrationModalProps) {
  const [type, setType] = useState<'mcp' | 'api' | null>(initialType === 'mcp' ? 'mcp' : 'api');
  const [formData, setFormData] = useState<any>({
    // MCP fields
    name: integration?.name || '',
    description: integration?.description || '',
    mcpServerUrl: integration?.serverUrl || '',
    mcpApiKey: '',
    
    // API fields
    apiName: integration?.name || '',
    apiMethod: integration?.method || 'GET',
    apiEndpoint: integration?.endpoint || '',
    apiAuthType: integration?.authType || 'none',
    apiAuthValue: '',
    apiHeaders: '',
    apiBody: '',
    apiDescription: integration?.description || '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string>('');

  const handleTest = async () => {
    setIsConnecting(true);
    setConnectionStatus('idle');
    setErrorMessage('');

    try {
      if (integration) {
        // Test existing integration
        const response = await apiPost<{ status: string; message: string }>(`/rag/integrations/${integration.id}/test`);
        if (response.status === 'success') {
          setConnectionStatus('success');
        } else {
          setConnectionStatus('error');
          setErrorMessage(response.message || 'Connection test failed');
        }
      } else {
        // For new integrations, just validate URL format
        if (type === 'mcp') {
          if (!formData.mcpServerUrl) {
            setConnectionStatus('error');
            setErrorMessage('Please enter a server URL');
            return;
          }
          setConnectionStatus('success');
          setErrorMessage('Note: Connection will be tested when you save');
        } else {
          if (!formData.apiEndpoint || (!formData.apiEndpoint.startsWith('http://') && !formData.apiEndpoint.startsWith('https://'))) {
            setConnectionStatus('error');
            setErrorMessage('Please enter a valid URL starting with http:// or https://');
            return;
          }
          setConnectionStatus('success');
          setErrorMessage('Note: Connection will be tested when you save');
        }
      }
    } catch (err: any) {
      console.error('Connection test error:', err);
      setConnectionStatus('error');
      setErrorMessage(err.message || 'Failed to test connection. Please check your configuration.');
    } finally {
      setIsConnecting(false);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    setErrorMessage('');

    try {
      if (type === 'mcp') {
        if (!formData.mcpServerUrl || !formData.name) {
          setErrorMessage('Please fill in all required fields');
          setIsSaving(false);
          return;
        }

        if (integration) {
          // Update existing
          await apiPut(`/rag/integrations/${integration.id}`, {
            name: formData.name,
            description: formData.description,
            serverUrl: formData.mcpServerUrl,
            apiKey: formData.mcpApiKey || undefined,
          });
        } else {
          // Create new
          await apiPost('/rag/integrations/mcp', {
            name: formData.name,
            description: formData.description,
            serverUrl: formData.mcpServerUrl,
            apiKey: formData.mcpApiKey || undefined,
          });
        }
      } else {
        if (!formData.apiEndpoint || !formData.apiName) {
          setErrorMessage('Please fill in all required fields');
          setIsSaving(false);
          return;
        }

        if (integration) {
          // Update existing
          await apiPut(`/rag/integrations/${integration.id}`, {
            name: formData.apiName,
            description: formData.apiDescription,
            method: formData.apiMethod,
            endpoint: formData.apiEndpoint,
            authType: formData.apiAuthType,
            authValue: formData.apiAuthValue || undefined,
            headers: formData.apiHeaders ? JSON.parse(formData.apiHeaders) : undefined,
            bodyTemplate: formData.apiBody || undefined,
          });
        } else {
          // Create new
          await apiPost('/rag/integrations/custom-api', {
            name: formData.apiName,
            description: formData.apiDescription,
            method: formData.apiMethod,
            endpoint: formData.apiEndpoint,
            authType: formData.apiAuthType,
            authValue: formData.apiAuthValue || undefined,
            headers: formData.apiHeaders ? JSON.parse(formData.apiHeaders) : undefined,
            bodyTemplate: formData.apiBody || undefined,
          });
        }
      }

      onSave();
    } catch (err: any) {
      console.error('Save error:', err);
      setErrorMessage(err.message || 'Failed to save integration');
    } finally {
      setIsSaving(false);
    }
  };

  // If editing, set the type from integration
  useEffect(() => {
    if (integration) {
      setType(integration.integrationType === 'mcp' ? 'mcp' : 'api');
    }
  }, [integration]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white rounded-xl max-w-2xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 md:p-6 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-gray-900 text-lg md:text-xl mb-1">
              {integration ? 'Edit Integration' : 'Add Integration'}
            </h2>
            <p className="text-gray-600 text-sm">Connect an MCP tool or custom API</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="space-y-6">
            {/* Select Integration Type */}
            {!type && (
              <div>
                <label className="block text-gray-700 mb-3">What would you like to connect?</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <button
                    onClick={() => setType('mcp')}
                    className="p-4 md:p-6 border-2 border-gray-200 rounded-lg hover:border-purple-400 transition-all text-left"
                  >
                    <div className="w-12 h-12 bg-purple-50 rounded-lg flex items-center justify-center mb-3">
                      <Code className="w-6 h-6 text-purple-600" />
                    </div>
                    <h3 className="text-gray-900 mb-2">MCP Tool</h3>
                    <p className="text-sm text-gray-600 mb-3">
                      Connect to external services like Zendesk, Salesforce, Slack, etc.
                    </p>
                    <div className="text-xs text-purple-600">
                      Pre-built tools and resources →
                    </div>
                  </button>

                  <button
                    onClick={() => setType('api')}
                    className="p-4 md:p-6 border-2 border-gray-200 rounded-lg hover:border-blue-400 transition-all text-left"
                  >
                    <div className="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center mb-3">
                      <Link2 className="w-6 h-6 text-blue-600" />
                    </div>
                    <h3 className="text-gray-900 mb-2">Custom API</h3>
                    <p className="text-sm text-gray-600 mb-3">
                      Connect to your own web service, database, or internal system
                    </p>
                    <div className="text-xs text-blue-600">
                      Your own endpoints →
                    </div>
                  </button>
                </div>
              </div>
            )}

            {/* MCP Configuration */}
            {type === 'mcp' && (
              <>
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Code className="w-5 h-5 text-purple-600 mt-0.5" />
                    <div>
                      <h3 className="text-gray-900 mb-1">MCP Tool Connection</h3>
                      <p className="text-sm text-gray-700">
                        Connect to an MCP server to access pre-built tools and resources
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-gray-700 mb-2">
                      Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none"
                      placeholder="e.g., Zendesk Support Tool"
                    />
                  </div>

                  <div>
                    <label className="block text-gray-700 mb-2">
                      Description
                    </label>
                    <textarea
                      rows={2}
                      value={formData.description}
                      onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 outline-none resize-none"
                      placeholder="What does this integration do?"
                    />
                  </div>

                  <div>
                    <label className="block text-gray-700 mb-2">
                      MCP Server URL <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={formData.mcpServerUrl}
                      onChange={(e) => setFormData({ ...formData, mcpServerUrl: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none font-mono text-sm"
                      placeholder="mcp://your-server.com or https://mcp.example.com"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      The URL of your MCP server endpoint
                    </p>
                  </div>

                  <div>
                    <label className="block text-gray-700 mb-2">
                      API Token / Authentication Key
                    </label>
                    <div className="relative">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={formData.mcpApiKey}
                        onChange={(e) => setFormData({ ...formData, mcpApiKey: e.target.value })}
                        className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none font-mono text-sm"
                        placeholder="Enter your API token or authentication key"
                      />
                      <button
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-2 top-1/2 -translate-y-1/2 p-2 hover:bg-gray-100 rounded"
                        type="button"
                      >
                        {showPassword ? (
                          <EyeOff className="w-4 h-4 text-gray-600" />
                        ) : (
                          <Eye className="w-4 h-4 text-gray-600" />
                        )}
                      </button>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      Optional: If your MCP server requires authentication, enter your API token or key here
                    </p>
                  </div>

                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-xs text-blue-900">
                      💡 After connecting, all available tools and resources from this MCP server will be accessible
                    </p>
                  </div>
                </div>
              </>
            )}

            {/* API Configuration */}
            {type === 'api' && (
              <>
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Link2 className="w-5 h-5 text-blue-600 mt-0.5" />
                    <div>
                      <h3 className="text-gray-900 mb-1">Custom API Configuration</h3>
                      <p className="text-sm text-gray-700">
                        Connect to your own web service or endpoint
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-gray-700 mb-2">
                      Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={formData.apiName}
                      onChange={(e) => setFormData({ ...formData, apiName: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                      placeholder="e.g., Order Status API, User Profile Service"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      A descriptive name for this API integration
                    </p>
                  </div>

                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block text-gray-700 mb-2">
                        Method <span className="text-red-500">*</span>
                      </label>
                      <select 
                        value={formData.apiMethod}
                        onChange={(e) => setFormData({ ...formData, apiMethod: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      >
                        <option value="GET">GET</option>
                        <option value="POST">POST</option>
                        <option value="PUT">PUT</option>
                        <option value="PATCH">PATCH</option>
                        <option value="DELETE">DELETE</option>
                      </select>
                    </div>
                    <div className="col-span-2">
                      <label className="block text-gray-700 mb-2">
                        Endpoint URL <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="text"
                        value={formData.apiEndpoint}
                        onChange={(e) => setFormData({ ...formData, apiEndpoint: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
                        placeholder="https://api.yoursite.com/endpoint"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-gray-700 mb-2">Authentication</label>
                    <select 
                      value={formData.apiAuthType}
                      onChange={(e) => setFormData({ ...formData, apiAuthType: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="none">None</option>
                      <option value="apikey">API Key</option>
                      <option value="bearer">Bearer Token</option>
                      <option value="basic">Basic Auth</option>
                    </select>
                    {formData.apiAuthType === 'none' && (
                      <p className="text-xs text-gray-500 mt-1">
                        Select an authentication method above to enter your API token or credentials
                      </p>
                    )}
                  </div>

                  {formData.apiAuthType !== 'none' && (
                    <div>
                      <label className="block text-gray-700 mb-2">
                        {formData.apiAuthType === 'apikey' ? 'API Key' :
                         formData.apiAuthType === 'bearer' ? 'Bearer Token' :
                         'Username:Password'} <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={formData.apiAuthValue}
                          onChange={(e) => setFormData({ ...formData, apiAuthValue: e.target.value })}
                          className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
                          placeholder={
                            formData.apiAuthType === 'basic' 
                              ? 'username:password' 
                              : formData.apiAuthType === 'apikey'
                              ? 'Enter your API key'
                              : 'Enter your bearer token'
                          }
                        />
                        <button
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-2 hover:bg-gray-100 rounded"
                          type="button"
                        >
                          {showPassword ? (
                            <EyeOff className="w-4 h-4 text-gray-600" />
                          ) : (
                            <Eye className="w-4 h-4 text-gray-600" />
                          )}
                        </button>
                      </div>
                      <p className="text-xs text-gray-500 mt-1">
                        {formData.apiAuthType === 'basic' 
                          ? 'Format: username:password' 
                          : 'Your credentials will be encrypted and stored securely'}
                      </p>
                    </div>
                  )}

                  <div>
                    <label className="block text-gray-700 mb-2">
                      Description
                    </label>
                    <textarea
                      rows={2}
                      value={formData.apiDescription}
                      onChange={(e) => setFormData({ ...formData, apiDescription: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                      placeholder="What does this API do?"
                    />
                  </div>

                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-xs text-blue-900">
                      🔒 Your API credentials are encrypted and stored securely
                    </p>
                  </div>
                </div>
              </>
            )}

            {/* Error Message */}
            {errorMessage && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <div className="text-red-900 mb-1">Error</div>
                  <div className="text-sm text-red-700">{errorMessage}</div>
                </div>
              </div>
            )}

            {/* Connection Status */}
            {connectionStatus === 'success' && (
              <div className="p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <div className="text-green-900 mb-1">Connection Successful!</div>
                  <div className="text-sm text-green-700">
                    {type === 'mcp' 
                      ? 'MCP server connected. Available tools will be loaded.'
                      : 'API endpoint is reachable and ready to use.'}
                  </div>
                </div>
              </div>
            )}

            {connectionStatus === 'error' && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <div className="text-red-900 mb-1">Connection Failed</div>
                  <div className="text-sm text-red-700">
                    {errorMessage || 'Unable to connect. Please check your configuration and try again.'}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 flex items-center justify-between flex-shrink-0 bg-gray-50">
          <button
            onClick={() => {
              if (type) {
                setType(null);
                setConnectionStatus('idle');
                setErrorMessage('');
              } else {
                onClose();
              }
            }}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
          >
            {type ? 'Back' : 'Cancel'}
          </button>
          {type && (
            <div className="flex gap-2">
              <button
                onClick={handleTest}
                disabled={isConnecting}
                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isConnecting ? (
                  <>
                    <Loader className="w-4 h-4 animate-spin" />
                    Testing...
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4" />
                    {integration ? 'Test Connection' : 'Validate'}
                  </>
                )}
              </button>
              <button
                onClick={handleSave}
                disabled={isSaving}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSaving ? (
                  <>
                    <Loader className="w-5 h-5 animate-spin" />
                    {integration ? 'Updating...' : 'Saving...'}
                  </>
                ) : (
                  <>
                    <Check className="w-5 h-5" />
                    {integration ? 'Update Integration' : 'Save Integration'}
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
