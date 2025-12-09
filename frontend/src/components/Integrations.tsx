import { useState } from 'react';
import { Plus, Check, ExternalLink, Zap, Settings as SettingsIcon, Play, Edit, Trash2, X, Eye, EyeOff, CheckCircle, AlertCircle, Loader, Code, Link2 } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface IntegrationsProps {
  onNavigate: (screen: string) => void;
}

export function Integrations({ onNavigate }: IntegrationsProps) {
  const [selectedTab, setSelectedTab] = useState<'mcps' | 'apis'>('mcps');
  const [showAddModal, setShowAddModal] = useState(false);

  const connectedMCPs = [
    {
      id: 1,
      name: 'Zendesk Support Tool',
      description: 'Access Zendesk tickets and customer information',
      status: 'Connected',
      actions: ['create_ticket', 'search_articles', 'get_customer_info', 'update_ticket'],
      usedBy: 3,
      serverUrl: 'mcp://zendesk.server',
    },
    {
      id: 2,
      name: 'Salesforce CRM',
      description: 'Manage leads and customer records',
      status: 'Connected',
      actions: ['create_lead', 'search_contacts', 'update_record'],
      usedBy: 2,
      serverUrl: 'mcp://salesforce.server',
    },
    {
      id: 3,
      name: 'Slack Notifications',
      description: 'Send messages and notifications',
      status: 'Connected',
      actions: ['send_message', 'notify_channel'],
      usedBy: 4,
      serverUrl: 'mcp://slack.server',
    },
  ];

  const customAPIs = [
    {
      id: 1,
      name: 'Check Order Status',
      method: 'GET',
      endpoint: 'https://api.mystore.com/orders/{order_id}',
      description: 'Look up order and tracking information',
      status: 'Active',
      usedBy: 1,
      authType: 'API Key',
    },
    {
      id: 2,
      name: 'Get User Profile',
      method: 'GET',
      endpoint: 'https://api.mystore.com/users/{user_id}',
      description: 'Retrieve account and subscription details',
      status: 'Active',
      usedBy: 2,
      authType: 'Bearer Token',
    },
    {
      id: 3,
      name: 'Create Support Ticket',
      method: 'POST',
      endpoint: 'https://api.mystore.com/support/tickets',
      description: 'Submit new support request',
      status: 'Active',
      usedBy: 3,
      authType: 'API Key',
    },
  ];

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
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto"
        >
          <Plus className="w-5 h-5" />
          Add Integration
        </button>
      </div>

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

      {selectedTab === 'mcps' && (
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
                          <span className="px-2 py-1 bg-green-50 text-green-700 rounded text-xs flex items-center gap-1">
                            <Check className="w-3 h-3" />
                            {mcp.status}
                          </span>
                        </div>
                        <p className="text-gray-600 text-sm mb-2">{mcp.description}</p>
                        <div className="text-xs text-gray-500 font-mono bg-gray-50 px-2 py-1 rounded inline-block mb-2">
                          {mcp.serverUrl}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-gray-500">
                          <span>{mcp.actions.length} available actions</span>
                          <span>•</span>
                          <span>Used in {mcp.usedBy} intents</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm">
                        Settings
                      </button>
                      <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                        <Trash2 className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                  </div>

                  {/* Available Actions */}
                  <div className="border-t border-gray-200 pt-4">
                    <div className="text-sm text-gray-700 mb-2">Available Actions:</div>
                    <div className="flex flex-wrap gap-2">
                      {mcp.actions.map((action, idx) => (
                        <div key={idx} className="px-3 py-1.5 bg-purple-50 border border-purple-200 rounded-lg text-sm text-purple-700 flex items-center gap-2 font-mono">
                          <Play className="w-3 h-3" />
                          {action}
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Empty State or Add New */}
          {connectedMCPs.length === 0 && (
            <div className="bg-white rounded-xl border-2 border-dashed border-gray-300 p-12 text-center">
              <div className="w-16 h-16 bg-purple-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <Code className="w-8 h-8 text-purple-600" />
              </div>
              <h3 className="text-gray-900 mb-2">No MCP Tools Connected</h3>
              <p className="text-gray-600 mb-4">Connect your first MCP tool to give your AI assistant more capabilities</p>
              <button 
                onClick={() => setShowAddModal(true)}
                className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
              >
                Add MCP Tool
              </button>
            </div>
          )}
        </div>
      )}

      {selectedTab === 'apis' && (
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
                            'bg-orange-100 text-orange-700'
                          }`}>
                            {api.method}
                          </span>
                          <span className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs">
                            {api.authType}
                          </span>
                        </div>
                        <p className="text-gray-600 text-sm mb-2">{api.description}</p>
                        <div className="font-mono text-sm text-gray-700 bg-gray-50 px-3 py-2 rounded border border-gray-200 mb-2">
                          {api.endpoint}
                        </div>
                        <div className="text-xs text-gray-500">
                          Used in {api.usedBy} intents
                        </div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2">
                        <Play className="w-4 h-4" />
                        Test
                      </button>
                      <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                        <Edit className="w-4 h-4 text-gray-600" />
                      </button>
                      <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
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
                onClick={() => setShowAddModal(true)}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Add Custom API
              </button>
            </div>
          )}
        </div>
      )}

      {/* Help Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-6">
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

      {/* Add Integration Modal */}
      {showAddModal && (
        <AddIntegrationModal
          onClose={() => setShowAddModal(false)}
          onSave={(data) => {
            console.log('Integration saved:', data);
            setShowAddModal(false);
          }}
        />
      )}
    </div>
  );
}

// Add Integration Modal Component
function AddIntegrationModal({ onClose, onSave }: any) {
  const [integrationType, setIntegrationType] = useState<'mcp' | 'api' | null>(null);
  const [formData, setFormData] = useState<any>({
    // MCP fields
    mcpServerUrl: '',
    mcpApiKey: '',
    
    // API fields
    apiName: '',
    apiMethod: 'GET',
    apiEndpoint: '',
    apiAuthType: 'none',
    apiAuthValue: '',
    apiHeaders: '',
    apiBody: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'idle' | 'success' | 'error'>('idle');

  const handleTest = async () => {
    setIsConnecting(true);
    setConnectionStatus('idle');
    
    // Simulate testing connection
    setTimeout(() => {
      setIsConnecting(false);
      setConnectionStatus('success');
    }, 2000);
  };

  const handleSave = () => {
    onSave({
      type: integrationType,
      config: formData,
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white rounded-xl max-w-2xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 md:p-6 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-gray-900 text-lg md:text-xl mb-1">Add Integration</h2>
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
            {!integrationType && (
              <div>
                <label className="block text-gray-700 mb-3">What would you like to connect?</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <button
                    onClick={() => setIntegrationType('mcp')}
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
                      Pre-built tools with multiple actions →
                    </div>
                  </button>

                  <button
                    onClick={() => setIntegrationType('api')}
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
            {integrationType === 'mcp' && (
              <>
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Code className="w-5 h-5 text-purple-600 mt-0.5" />
                    <div>
                      <h3 className="text-gray-900 mb-1">MCP Tool Connection</h3>
                      <p className="text-sm text-gray-700">
                        Connect to an MCP server to access pre-built tools and actions
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
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
                      Authentication Key
                    </label>
                    <div className="relative">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={formData.mcpApiKey}
                        onChange={(e) => setFormData({ ...formData, mcpApiKey: e.target.value })}
                        className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none font-mono text-sm"
                        placeholder="Optional authentication key"
                      />
                      <button
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-2 top-1/2 -translate-y-1/2 p-2 hover:bg-gray-100 rounded"
                      >
                        {showPassword ? (
                          <EyeOff className="w-4 h-4 text-gray-600" />
                        ) : (
                          <Eye className="w-4 h-4 text-gray-600" />
                        )}
                      </button>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      If your MCP server requires authentication
                    </p>
                  </div>

                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-xs text-blue-900">
                      💡 After connecting, all available tools and actions from this MCP server will be accessible
                    </p>
                  </div>
                </div>
              </>
            )}

            {/* API Configuration */}
            {integrationType === 'api' && (
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
                      Action Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={formData.apiName}
                      onChange={(e) => setFormData({ ...formData, apiName: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                      placeholder="e.g., Check Order Status, Get User Profile"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      A descriptive name for what this API does
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
                  </div>

                  {formData.apiAuthType !== 'none' && (
                    <div>
                      <label className="block text-gray-700 mb-2">
                        {formData.apiAuthType === 'apikey' ? 'API Key' :
                         formData.apiAuthType === 'bearer' ? 'Bearer Token' :
                         'Username:Password'}
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
                              : 'Enter your authentication key'
                          }
                        />
                        <button
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-2 hover:bg-gray-100 rounded"
                        >
                          {showPassword ? (
                            <EyeOff className="w-4 h-4 text-gray-600" />
                          ) : (
                            <Eye className="w-4 h-4 text-gray-600" />
                          )}
                        </button>
                      </div>
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

            {/* Connection Status */}
            {connectionStatus === 'success' && (
              <div className="p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <div className="text-green-900 mb-1">Connection Successful!</div>
                  <div className="text-sm text-green-700">
                    {integrationType === 'mcp' 
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
                    Unable to connect. Please check your configuration and try again.
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
              if (integrationType) {
                setIntegrationType(null);
              } else {
                onClose();
              }
            }}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
          >
            {integrationType ? 'Back' : 'Cancel'}
          </button>
          {integrationType && (
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
                    Test Connection
                  </>
                )}
              </button>
              <button
                onClick={handleSave}
                disabled={connectionStatus !== 'success'}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Check className="w-5 h-5" />
                Save Integration
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
