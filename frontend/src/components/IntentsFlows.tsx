import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Brain, Database, Target, Sparkles, Code, Link2, Zap, Settings as SettingsIcon, ChevronRight, HelpCircle, Save, X, CheckCircle, MessageSquare, User, FileText } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';
import { 
  getIntentIntegrationMappings, 
  createIntentIntegrationMapping, 
  deleteIntentIntegrationMapping,
  IntentIntegrationMapping,
  McpResource
} from '../utils/integrationsApi';
import { apiGet } from '../utils/api';

interface IntentsFlowsProps {
  onNavigate: (screen: string) => void;
}

export function IntentsFlows({ onNavigate }: IntentsFlowsProps) {
  const [selectedIntent, setSelectedIntent] = useState<number | null>(1);
  const [showEditor, setShowEditor] = useState(false);
  const [editingIntent, setEditingIntent] = useState<any>(null);

  const intents = [
    {
      id: 1,
      name: 'Technical Support Request',
      semanticDescription: 'User is experiencing a technical problem, bug, error, or needs help troubleshooting an issue with the product. They may describe symptoms, error messages, or ask how to fix something that isn\'t working.',
      responseType: 'ai_generated',
      useKnowledgeBase: true,
      knowledgeBaseTags: ['troubleshooting', 'errors', 'technical-docs'],
      actions: [
        { 
          type: 'mcp', 
          tool: 'create_support_ticket',
          description: 'Create a support ticket when the user reports a technical issue that needs tracking or escalation. Use this when the problem cannot be immediately resolved or requires follow-up.',
          config: { priority: 'high', category: 'technical' }
        },
        { 
          type: 'integration', 
          service: 'slack',
          action: 'notify_channel',
          description: 'Notify the tech support channel in Slack when the issue is critical, affects multiple users, or requires immediate team attention. Use when urgency keywords are detected or system-wide problems are mentioned.',
          config: { channel: '#tech-support' }
        }
      ],
      fallbackAction: 'escalate_to_human',
      status: 'Active',
      priority: 'high',
    },
    {
      id: 2,
      name: 'Product Information Query',
      semanticDescription: 'User wants to learn about product features, capabilities, pricing, plans, or how something works. They are in discovery or research mode and need detailed information.',
      responseType: 'ai_generated',
      useKnowledgeBase: true,
      knowledgeBaseTags: ['product-features', 'pricing', 'documentation'],
      actions: [
        { 
          type: 'integration', 
          service: 'salesforce',
          action: 'log_lead',
          description: 'Log a lead in Salesforce when the user shows interest in pricing, asks about plans, or requests a quote. This helps track potential customers in the sales pipeline.',
          config: { source: 'chatbot', status: 'new' }
        }
      ],
      fallbackAction: 'provide_contact_form',
      status: 'Active',
      priority: 'medium',
    },
    {
      id: 3,
      name: 'Order Status Inquiry',
      semanticDescription: 'User wants to check their order status, track a shipment, get delivery updates, or know when something will arrive. They may provide an order number or tracking information.',
      responseType: 'action_results',
      useKnowledgeBase: false,
      knowledgeBaseTags: [],
      actions: [
        { 
          type: 'api', 
          endpoint: '/api/orders/{order_id}',
          method: 'GET',
          description: 'Fetch order details when the user provides an order number or asks about their order status. Extract order_id from the conversation context.',
          config: { timeout: 5000 }
        },
        { 
          type: 'mcp', 
          tool: 'get_tracking_info',
          description: 'Retrieve shipping tracking information when the order has been shipped and the user wants delivery updates or tracking details.',
          config: {}
        }
      ],
      fallbackAction: 'ask_for_order_number',
      status: 'Active',
      priority: 'high',
    },
    {
      id: 4,
      name: 'Pricing Question',
      semanticDescription: 'User is asking about plans, pricing, costs, or wants to see pricing information.',
      responseType: 'quick_reply',
      useKnowledgeBase: false,
      knowledgeBaseTags: [],
      quickReply: 'We offer three plans:\n\n✨ Starter: $29/month\n💼 Professional: $79/month\n🚀 Enterprise: Custom pricing\n\nWould you like to see detailed feature comparisons?',
      actions: [
        { 
          type: 'integration', 
          service: 'salesforce',
          action: 'log_lead',
          description: 'Create a lead in Salesforce for any pricing inquiry to track potential customers and enable follow-up by the sales team.',
          config: { source: 'pricing_inquiry' }
        }
      ],
      fallbackAction: 'send_pricing_guide',
      status: 'Active',
      priority: 'high',
    },
    {
      id: 5,
      name: 'Urgent Issue',
      semanticDescription: 'User indicates urgency, emergency, or critical problem that requires immediate human attention. Keywords like "urgent", "asap", "critical", "emergency".',
      responseType: 'human_handoff',
      useKnowledgeBase: false,
      knowledgeBaseTags: [],
      actions: [
        { 
          type: 'mcp', 
          tool: 'create_support_ticket',
          description: 'Immediately create a critical priority support ticket for urgent issues that require immediate attention and tracking.',
          config: { priority: 'critical', category: 'urgent', assignee: 'support-lead' }
        },
        { 
          type: 'integration', 
          service: 'slack',
          action: 'alert_team',
          description: 'Send immediate alerts to the support team via Slack when an urgent or critical issue is detected, ensuring rapid response.',
          config: { users: ['@support-lead', '@manager'], message: '🚨 Urgent issue needs immediate attention' }
        },
        { 
          type: 'api', 
          endpoint: '/api/notifications/sms',
          method: 'POST',
          description: 'Send SMS notification to on-call support staff for critical issues that require immediate human intervention outside business hours.',
          config: { to: '+1234567890', message: 'Critical support case created' }
        }
      ],
      fallbackAction: 'show_emergency_contact',
      status: 'Active',
      priority: 'critical',
    },
    {
      id: 6,
      name: 'Request Demo',
      semanticDescription: 'User wants to see a product demo, schedule a demo call, or get a walkthrough of how the product works. They are interested in seeing it in action.',
      responseType: 'ai_generated',
      useKnowledgeBase: true,
      knowledgeBaseTags: ['demo', 'product-tour', 'features'],
      actions: [
        { 
          type: 'mcp', 
          tool: 'create_calendar_event',
          description: 'Schedule a calendar event when the user provides a specific date and time for the demo, or when they agree to a suggested demo time.',
          config: { type: 'demo', duration: 30, calendar: 'sales-team' }
        },
        { 
          type: 'integration', 
          service: 'salesforce',
          action: 'create_lead',
          description: 'Create a hot lead in Salesforce for demo requests, as these indicate strong buying intent and should be prioritized by the sales team.',
          config: { source: 'demo_request', status: 'hot', campaign: 'inbound-demo' }
        },
        { 
          type: 'integration', 
          service: 'hubspot',
          action: 'create_deal',
          description: 'Create a deal in HubSpot when a demo is scheduled, moving the prospect into the sales pipeline at the demo stage.',
          config: { pipeline: 'sales', stage: 'demo_scheduled', amount: 0 }
        }
      ],
      fallbackAction: 'send_demo_video',
      status: 'Active',
      priority: 'high',
    },
    {
      id: 7,
      name: 'Billing Question',
      semanticDescription: 'User has questions about their bill, invoice, payment, subscription, or wants to update payment method. May mention charges, billing cycle, or payment issues.',
      responseType: 'action_results',
      useKnowledgeBase: false,
      knowledgeBaseTags: [],
      actions: [
        { 
          type: 'api', 
          endpoint: '/api/billing/customer/{customer_id}',
          method: 'GET',
          description: 'Retrieve current customer billing information including plan details, payment method, and next billing date when user asks about their account or subscription.',
          config: { include: ['current_plan', 'payment_method', 'next_billing_date'] }
        },
        { 
          type: 'api', 
          endpoint: '/api/billing/invoices/{customer_id}',
          method: 'GET',
          description: 'Fetch recent invoice history when the user asks about past bills, payment history, or needs to download invoices.',
          config: { limit: 5, status: 'all' }
        },
        { 
          type: 'mcp', 
          tool: 'update_customer_profile',
          description: 'Update customer payment information when the user explicitly requests to change their payment method or update billing details.',
          config: { fields: ['payment_method'] }
        }
      ],
      fallbackAction: 'send_billing_contact',
      status: 'Active',
      priority: 'high',
    },
  ];

  const selectedIntentData = intents.find((i) => i.id === selectedIntent);

  const handleEditIntent = (intent: any) => {
    setEditingIntent(intent);
    setShowEditor(true);
  };

  const handleCreateIntent = () => {
    setEditingIntent({
      name: '',
      semanticDescription: '',
      responseType: 'ai_generated',
      useKnowledgeBase: false,
      knowledgeBaseTags: [],
      actions: [],
      fallbackAction: 'escalate_to_human',
      status: 'Draft',
      priority: 'medium',
    });
    setShowEditor(true);
  };

  const getResponseTypeInfo = (type: string) => {
    switch (type) {
      case 'ai_generated':
        return {
          icon: Sparkles,
          color: 'blue',
          label: 'AI Generated',
          description: 'LLM generates a custom, contextual response',
        };
      case 'quick_reply':
        return {
          icon: MessageSquare,
          color: 'green',
          label: 'Quick Reply',
          description: 'Send a pre-written template response',
        };
      case 'action_results':
        return {
          icon: Zap,
          color: 'orange',
          label: 'Action Results',
          description: 'Display results from executed actions',
        };
      case 'human_handoff':
        return {
          icon: User,
          color: 'purple',
          label: 'Human Handoff',
          description: 'Route conversation to a human agent',
        };
      default:
        return {
          icon: Brain,
          color: 'gray',
          label: 'Unknown',
          description: '',
        };
    }
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Intents & Flows</h1>
          <p className="text-gray-600 text-sm md:text-base">Define what your bot understands and how it responds</p>
        </div>
        <button 
          onClick={handleCreateIntent}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto"
        >
          <Plus className="w-5 h-5" />
          New Intent
        </button>
      </div>

      {/* How It Works */}
      <div className="mb-6 bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-xl p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <Brain className="w-6 h-6 text-white" />
          </div>
          <div className="flex-1">
            <h3 className="text-gray-900 mb-2">How DelightChat Processes Messages</h3>
            <p className="text-gray-700 text-sm mb-4">
              The LLM analyzes every message to understand what the customer wants (intent detection). 
              Then it intelligently chooses which actions to execute based on the conversation context and action descriptions. 
              Finally, it generates a response using the action results and your chosen strategy.
            </p>
            <div className="flex items-center gap-2 text-xs flex-wrap">
              <div className="flex items-center gap-1 px-3 py-2 bg-white rounded-lg border border-blue-200">
                <Brain className="w-4 h-4 text-blue-600" />
                <span className="text-gray-700">1. Understand Intent</span>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-400" />
              <div className="flex items-center gap-1 px-3 py-2 bg-white rounded-lg border border-orange-200">
                <Zap className="w-4 h-4 text-orange-600" />
                <span className="text-gray-700">2. Execute Actions</span>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-400" />
              <div className="flex items-center gap-1 px-3 py-2 bg-white rounded-lg border border-purple-200">
                <Sparkles className="w-4 h-4 text-purple-600" />
                <span className="text-gray-700">3. Generate Response</span>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-400" />
              <div className="flex items-center gap-1 px-3 py-2 bg-white rounded-lg border border-green-200">
                <Target className="w-4 h-4 text-green-600" />
                <span className="text-gray-700">4. Send to Customer</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Intents List */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-4 border-b border-gray-200">
              <h2 className="text-gray-900">Intents ({intents.length})</h2>
            </div>
            <div className="divide-y divide-gray-200 max-h-[700px] overflow-y-auto">
              {intents.map((intent) => {
                const typeInfo = getResponseTypeInfo(intent.responseType);
                const TypeIcon = typeInfo.icon;
                
                return (
                  <button
                    key={intent.id}
                    onClick={() => setSelectedIntent(intent.id)}
                    className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
                      selectedIntent === intent.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3 mb-2">
                      <div className={`w-8 h-8 bg-${typeInfo.color}-50 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5`}>
                        <TypeIcon className={`w-4 h-4 text-${typeInfo.color}-600`} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-gray-900 mb-1">{intent.name}</div>
                        <p className="text-gray-600 text-xs line-clamp-2">{intent.semanticDescription}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-500 ml-11 flex-wrap">
                      <span className={`px-2 py-0.5 rounded bg-${typeInfo.color}-50 text-${typeInfo.color}-700`}>
                        {typeInfo.label}
                      </span>
                      {intent.actions.length > 0 && (
                        <>
                          <span>•</span>
                          <span>{intent.actions.length} action{intent.actions.length > 1 ? 's' : ''}</span>
                        </>
                      )}
                      {intent.useKnowledgeBase && (
                        <>
                          <span>•</span>
                          <span>KB enabled</span>
                        </>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Intent Details */}
        <div className="lg:col-span-2 space-y-6">
          {selectedIntentData && (
            <>
              {/* Semantic Description */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-gray-900 flex items-center gap-2">
                    <Brain className="w-5 h-5 text-blue-600" />
                    Intent Definition
                  </h2>
                  <button 
                    onClick={() => handleEditIntent(selectedIntentData)}
                    className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2"
                  >
                    <Edit className="w-4 h-4" />
                    Edit
                  </button>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-gray-700 mb-2 text-sm">Intent Name</label>
                    <div className="px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-900">
                      {selectedIntentData.name}
                    </div>
                  </div>
                  <div>
                    <label className="block text-gray-700 mb-2 text-sm">
                      How to Recognize This Intent
                      <span className="text-gray-500 text-xs ml-2">(LLM uses this to detect customer's need)</span>
                    </label>
                    <div className="px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-900 text-sm whitespace-pre-wrap">
                      {selectedIntentData.semanticDescription}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-gray-700 mb-2 text-sm">Priority</label>
                      <div className={`px-4 py-2 border rounded-lg text-sm ${
                        selectedIntentData.priority === 'critical' ? 'bg-red-50 border-red-200 text-red-700' :
                        selectedIntentData.priority === 'high' ? 'bg-orange-50 border-orange-200 text-orange-700' :
                        selectedIntentData.priority === 'medium' ? 'bg-yellow-50 border-yellow-200 text-yellow-700' :
                        'bg-gray-50 border-gray-200 text-gray-700'
                      }`}>
                        {selectedIntentData.priority.charAt(0).toUpperCase() + selectedIntentData.priority.slice(1)} Priority
                      </div>
                    </div>
                    <div>
                      <label className="block text-gray-700 mb-2 text-sm">Status</label>
                      <div className="px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-sm">
                        {selectedIntentData.status}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Actions Configuration */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="text-gray-900 mb-4 flex items-center gap-2">
                  <Zap className="w-5 h-5 text-orange-600" />
                  Available Actions
                </h2>
                <p className="text-sm text-gray-600 mb-4">
                  The LLM will intelligently choose which actions to execute based on the conversation context. Each action has a description that helps the LLM decide when to use it.
                </p>

                {selectedIntentData.actions.length === 0 ? (
                  <div className="text-center py-8 border-2 border-dashed border-gray-300 rounded-lg">
                    <Zap className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                    <p className="text-sm text-gray-600">No actions configured</p>
                    <p className="text-xs text-gray-500 mt-1">Actions are optional - the LLM can respond without them</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {selectedIntentData.actions.map((action: any, idx: number) => (
                      <div key={idx} className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors">
                        <div className="flex items-start gap-3">
                          <div className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                            action.type === 'mcp' ? 'bg-purple-50' :
                            action.type === 'api' ? 'bg-blue-50' :
                            'bg-green-50'
                          }`}>
                            {action.type === 'mcp' && <Code className="w-5 h-5 text-purple-600" />}
                            {action.type === 'api' && <Link2 className="w-5 h-5 text-blue-600" />}
                            {action.type === 'integration' && <Zap className="w-5 h-5 text-green-600" />}
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <span className={`text-xs px-2 py-0.5 rounded font-mono ${
                                action.type === 'mcp' ? 'bg-purple-100 text-purple-700' :
                                action.type === 'api' ? 'bg-blue-100 text-blue-700' :
                                'bg-green-100 text-green-700'
                              }`}>
                                {action.type.toUpperCase()}
                              </span>
                              <span className="text-xs px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                                LLM-Driven
                              </span>
                            </div>
                            <div className="text-sm font-medium text-gray-900 mb-2">
                              {action.type === 'mcp' && `MCP Tool: ${action.tool}`}
                              {action.type === 'api' && `${action.method || 'GET'} ${action.endpoint}`}
                              {action.type === 'integration' && `${action.service} - ${action.action}`}
                            </div>
                            {action.description && (
                              <div className="text-xs text-gray-600 bg-gray-50 px-3 py-2 rounded-lg mb-2 border-l-2 border-blue-400">
                                <span className="font-medium text-gray-700">When to use: </span>
                                {action.description}
                              </div>
                            )}
                            {Object.keys(action.config || {}).length > 0 && (
                              <div className="text-xs text-gray-600 font-mono bg-gray-50 px-2 py-1 rounded mt-2">
                                {JSON.stringify(action.config, null, 2)}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Response Type */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="text-gray-900 mb-4 flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-purple-600" />
                  How to Respond
                </h2>

                {(() => {
                  const typeInfo = getResponseTypeInfo(selectedIntentData.responseType);
                  const TypeIcon = typeInfo.icon;
                  
                  return (
                    <>
                      {/* Selected Response Type */}
                      <div className={`p-4 border-2 border-${typeInfo.color}-500 bg-${typeInfo.color}-50 rounded-lg mb-4`}>
                        <div className="flex items-start gap-3">
                          <div className={`w-10 h-10 bg-${typeInfo.color}-100 rounded-lg flex items-center justify-center flex-shrink-0`}>
                            <TypeIcon className={`w-5 h-5 text-${typeInfo.color}-600`} />
                          </div>
                          <div className="flex-1">
                            <div className="text-gray-900 mb-1">{typeInfo.label}</div>
                            <p className="text-sm text-gray-700">{typeInfo.description}</p>
                          </div>
                        </div>
                      </div>

                      {/* Response Configuration */}
                      {selectedIntentData.responseType === 'ai_generated' && (
                        <div className="space-y-3">
                          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                            <div className="flex items-center gap-2 mb-3">
                              <Sparkles className="w-4 h-4 text-blue-600" />
                              <span className="text-sm text-gray-900">AI Response Configuration</span>
                            </div>
                            <p className="text-sm text-gray-700 mb-3">
                              The LLM will generate a natural, contextual response for this intent.
                            </p>
                            <div className="flex items-center justify-between text-sm">
                              <span className="text-gray-700">Use Knowledge Base:</span>
                              <span className={`px-2 py-1 rounded text-xs ${
                                selectedIntentData.useKnowledgeBase 
                                  ? 'bg-green-100 text-green-700' 
                                  : 'bg-gray-100 text-gray-700'
                              }`}>
                                {selectedIntentData.useKnowledgeBase ? 'Yes - Search for relevant content' : 'No - Use general knowledge only'}
                              </span>
                            </div>
                            {selectedIntentData.useKnowledgeBase && selectedIntentData.knowledgeBaseTags.length > 0 && (
                              <div className="flex items-center justify-between text-sm mt-2 pt-2 border-t border-blue-300">
                                <span className="text-gray-700">Search Tags:</span>
                                <div className="flex gap-1 flex-wrap">
                                  {selectedIntentData.knowledgeBaseTags.map((tag: string, idx: number) => (
                                    <span key={idx} className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
                                      {tag}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      )}

                      {selectedIntentData.responseType === 'quick_reply' && (
                        <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                          <div className="flex items-center gap-2 mb-3">
                            <MessageSquare className="w-4 h-4 text-green-600" />
                            <span className="text-sm text-gray-900">Quick Reply Template</span>
                          </div>
                          <div className="px-3 py-2 bg-white border border-green-300 rounded text-sm text-gray-900 whitespace-pre-wrap">
                            {selectedIntentData.quickReply || 'No template configured'}
                          </div>
                        </div>
                      )}

                      {selectedIntentData.responseType === 'action_results' && (
                        <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg">
                          <div className="flex items-center gap-2 mb-2">
                            <Zap className="w-4 h-4 text-orange-600" />
                            <span className="text-sm text-gray-900">Action Results Response</span>
                          </div>
                          <p className="text-sm text-gray-700 mb-2">
                            The bot will display information from the executed actions (e.g., order status, account details).
                            The LLM formats the results naturally.
                          </p>
                          <div className="text-xs text-gray-600">
                            ⚡ {selectedIntentData.actions.length} action{selectedIntentData.actions.length > 1 ? 's' : ''} configured to provide data
                          </div>
                        </div>
                      )}

                      {selectedIntentData.responseType === 'human_handoff' && (
                        <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                          <div className="flex items-center gap-2 mb-2">
                            <User className="w-4 h-4 text-purple-600" />
                            <span className="text-sm text-gray-900">Human Agent Handoff</span>
                          </div>
                          <p className="text-sm text-gray-700">
                            This conversation will be marked as "Needs Help" and routed to your team's inbox for a human agent to respond.
                            The bot will inform the customer that a team member will assist them shortly.
                          </p>
                        </div>
                      )}
                    </>
                  );
                })()}
              </div>

              {/* Fallback Strategy */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="text-gray-900 mb-4">Fallback Strategy</h2>
                <p className="text-sm text-gray-600 mb-4">
                  What happens if the bot can't respond properly or actions fail?
                </p>
                <div className="px-4 py-3 bg-orange-50 border border-orange-200 rounded-lg flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <HelpCircle className="w-4 h-4 text-orange-600" />
                    <span className="text-sm text-gray-900">If something goes wrong:</span>
                  </div>
                  <span className="text-sm text-orange-700 font-medium">{selectedIntentData.fallbackAction.replace(/_/g, ' ')}</span>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Intent Editor Modal */}
      {showEditor && editingIntent && (
        <IntentEditor
          intent={editingIntent}
          onSave={(intentData) => {
            console.log('Saving intent:', intentData);
            setShowEditor(false);
            setEditingIntent(null);
          }}
          onClose={() => {
            setShowEditor(false);
            setEditingIntent(null);
          }}
        />
      )}
    </div>
  );
}

// Intent Editor Component
function IntentEditor({ intent, onSave, onClose }: any) {
  const [name, setName] = useState(intent?.name || '');
  const [semanticDescription, setSemanticDescription] = useState(intent?.semanticDescription || '');
  const [responseType, setResponseType] = useState(intent?.responseType || 'ai_generated');
  const [useKnowledgeBase, setUseKnowledgeBase] = useState(intent?.useKnowledgeBase || false);
  const [knowledgeBaseTags, setKnowledgeBaseTags] = useState<string[]>(intent?.knowledgeBaseTags || []);
  const [quickReply, setQuickReply] = useState(intent?.quickReply || '');
  const [actions, setActions] = useState<any[]>(intent?.actions || []);
  const [fallbackAction, setFallbackAction] = useState(intent?.fallbackAction || 'escalate_to_human');
  const [priority, setPriority] = useState(intent?.priority || 'medium');
  const [newTag, setNewTag] = useState('');
  const [showActionForm, setShowActionForm] = useState(false);

  const responseTypes = [
    {
      value: 'ai_generated',
      label: 'AI Generated Response',
      description: 'LLM creates a custom, natural response (optionally using knowledge base)',
      icon: Sparkles,
    },
    {
      value: 'quick_reply',
      label: 'Quick Reply',
      description: 'Send a pre-written template message',
      icon: MessageSquare,
    },
    {
      value: 'action_results',
      label: 'Action Results',
      description: 'Show data from executed actions (order status, account info, etc.)',
      icon: Zap,
    },
    {
      value: 'human_handoff',
      label: 'Human Handoff',
      description: 'Route to your team for human assistance',
      icon: User,
    },
  ];

  const addKnowledgeTag = () => {
    if (newTag.trim() && !knowledgeBaseTags.includes(newTag.trim())) {
      setKnowledgeBaseTags([...knowledgeBaseTags, newTag.trim()]);
      setNewTag('');
    }
  };

  const removeKnowledgeTag = (tag: string) => {
    setKnowledgeBaseTags(knowledgeBaseTags.filter(t => t !== tag));
  };

  const addAction = (actionData: any) => {
    setActions([...actions, actionData]);
    setShowActionForm(false);
  };

  const removeAction = (index: number) => {
    setActions(actions.filter((_, i) => i !== index));
  };

  const handleSave = () => {
    onSave({
      ...intent,
      name,
      semanticDescription,
      responseType,
      useKnowledgeBase,
      knowledgeBaseTags,
      quickReply,
      actions,
      fallbackAction,
      priority,
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto">
      <div className="bg-white rounded-xl shadow-xl max-w-4xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-4 md:px-6 py-4 flex items-center justify-between z-10">
          <h2 className="text-gray-900">{intent?.id ? 'Edit Intent' : 'Create New Intent'}</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 md:p-6 space-y-6">
          {/* Basic Info */}
          <div>
            <label className="block text-gray-700 mb-2 text-sm">Intent Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="e.g., Technical Support Request"
            />
          </div>

          <div>
            <label className="block text-gray-700 mb-2 text-sm">
              Semantic Description
              <span className="text-gray-500 text-xs ml-2">(Explain when this intent applies - the LLM uses this)</span>
            </label>
            <textarea
              value={semanticDescription}
              onChange={(e) => setSemanticDescription(e.target.value)}
              rows={4}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Describe what the user is trying to do, what keywords they might use, and when this intent should be detected..."
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-700 mb-2 text-sm">Priority</label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
                <option value="critical">Critical</option>
              </select>
            </div>
            <div>
              <label className="block text-gray-700 mb-2 text-sm">Fallback Action</label>
              <select
                value={fallbackAction}
                onChange={(e) => setFallbackAction(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="escalate_to_human">Escalate to human</option>
                <option value="ask_clarifying_question">Ask clarifying question</option>
                <option value="provide_contact_form">Provide contact form</option>
                <option value="send_error_message">Send error message</option>
              </select>
            </div>
          </div>

          {/* Response Type Selection */}
          <div>
            <label className="block text-gray-700 mb-3 text-sm">Response Type</label>
            <div className="grid grid-cols-2 gap-3">
              {responseTypes.map((type) => {
                const Icon = type.icon;
                const isSelected = responseType === type.value;
                return (
                  <button
                    key={type.value}
                    type="button"
                    onClick={() => setResponseType(type.value)}
                    className={`p-4 border-2 rounded-lg text-left transition-all ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                        isSelected ? 'bg-blue-100' : 'bg-gray-100'
                      }`}>
                        <Icon className={`w-5 h-5 ${isSelected ? 'text-blue-600' : 'text-gray-600'}`} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className={`text-sm mb-1 ${isSelected ? 'text-blue-900' : 'text-gray-900'}`}>
                          {type.label}
                        </div>
                        <p className={`text-xs ${isSelected ? 'text-blue-700' : 'text-gray-600'}`}>
                          {type.description}
                        </p>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Response Type Specific Fields */}
          {responseType === 'ai_generated' && (
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg space-y-4">
              <div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={useKnowledgeBase}
                    onChange={(e) => setUseKnowledgeBase(e.target.checked)}
                    className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-900">Use Knowledge Base for context</span>
                </label>
              </div>

              {useKnowledgeBase && (
                <div>
                  <label className="block text-gray-700 mb-2 text-sm">Knowledge Base Tags (optional)</label>
                  <div className="flex gap-2 mb-2">
                    <input
                      type="text"
                      value={newTag}
                      onChange={(e) => setNewTag(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addKnowledgeTag())}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                      placeholder="Add tag and press Enter"
                    />
                    <button
                      type="button"
                      onClick={addKnowledgeTag}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm"
                    >
                      Add
                    </button>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {knowledgeBaseTags.map((tag) => (
                      <span
                        key={tag}
                        className="px-3 py-1 bg-blue-100 text-blue-700 rounded-lg flex items-center gap-2 text-sm"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => removeKnowledgeTag(tag)}
                          className="hover:text-blue-900"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {responseType === 'quick_reply' && (
            <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
              <label className="block text-gray-700 mb-2 text-sm">Quick Reply Template</label>
              <textarea
                value={quickReply}
                onChange={(e) => setQuickReply(e.target.value)}
                rows={4}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent text-sm"
                placeholder="Enter the pre-written message to send..."
              />
            </div>
          )}

          {/* Actions Configuration */}
          <div>
            <label className="block text-gray-700 mb-3 text-sm">Actions (optional)</label>
            <p className="text-xs text-gray-600 mb-3">
              Configure actions that the LLM can intelligently choose to execute based on conversation context. 
              Each action should have a clear description explaining when it should be used.
            </p>

            {actions.length > 0 && (
              <div className="space-y-2 mb-3">
                {actions.map((action, idx) => (
                  <div key={idx} className="p-3 bg-gray-50 border border-gray-200 rounded-lg">
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="flex-1">
                        <div className="text-sm font-medium text-gray-900">
                          {action.type === 'mcp' && `MCP: ${action.tool}`}
                          {action.type === 'api' && `API: ${action.endpoint}`}
                          {action.type === 'integration' && `${action.service} - ${action.action}`}
                        </div>
                        {action.description && (
                          <div className="text-xs text-gray-600 mt-1 italic">
                            "{action.description}"
                          </div>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => removeAction(idx)}
                        className="p-1 hover:bg-gray-200 rounded flex-shrink-0"
                      >
                        <X className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <button
              type="button"
              onClick={() => setShowActionForm(true)}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2 text-sm"
            >
              <Plus className="w-4 h-4" />
              Add Action
            </button>
          </div>
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-4 md:px-6 py-4 flex flex-col sm:flex-row sm:items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors w-full sm:w-auto"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full sm:w-auto"
          >
            <Save className="w-4 h-4" />
            Save Intent
          </button>
        </div>
      </div>

      {/* Action Form Modal */}
      {showActionForm && (
        <ActionForm
          onSave={addAction}
          onClose={() => setShowActionForm(false)}
        />
      )}
    </div>
  );
}

// Action Form Component
function ActionForm({ onSave, onClose }: { onSave: (action: any) => void; onClose: () => void }) {
  const [actionType, setActionType] = useState<'mcp' | 'api' | 'integration'>('mcp');
  const [availableMcpResources, setAvailableMcpResources] = useState<McpResource[]>([]);
  const [availableCustomApis, setAvailableCustomApis] = useState<any[]>([]);
  const [actionMappings, setActionMappings] = useState<IntentIntegrationMapping[]>([]);
  const [loadingMappings, setLoadingMappings] = useState(false);
  const [loadingResources, setLoadingResources] = useState(false);
  const [description, setDescription] = useState('');
  
  // MCP fields
  const [mcpTool, setMcpTool] = useState('');
  const [mcpConfig, setMcpConfig] = useState('{}');
  
  // API fields
  const [apiEndpoint, setApiEndpoint] = useState('');
  const [apiMethod, setApiMethod] = useState('GET');
  const [apiHeaders, setApiHeaders] = useState('{}');
  const [apiBody, setApiBody] = useState('{}');
  
  // Integration fields
  const [integrationService, setIntegrationService] = useState('');
  const [integrationAction, setIntegrationAction] = useState('');
  const [integrationConfig, setIntegrationConfig] = useState('{}');

  // Fetch MCP resources when MCP action type is selected
  useEffect(() => {
    if (actionType === 'mcp') {
      loadMcpResources();
    }
  }, [actionType]);

  const loadMcpResources = async () => {
    try {
      setLoadingResources(true);
      // Fetch all MCP integrations first
      const integrations = await apiGet<any[]>(`/rag/integrations?integrationType=mcp`);
      
      // Fetch resources from all MCP integrations
      const allResources: McpResource[] = [];
      for (const integration of integrations || []) {
        try {
          const resources = await getMcpResources(integration.id, 'tool');
          allResources.push(...resources);
        } catch (err) {
          console.error(`Error loading resources for integration ${integration.id}:`, err);
        }
      }
      
      setAvailableMcpResources(allResources);
    } catch (err: any) {
      console.error('Error loading MCP resources:', err);
      setAvailableMcpResources([]);
    } finally {
      setLoadingResources(false);
    }
  };

  // Fallback to hardcoded tools if no resources are available
  const mcpTools = availableMcpResources.length > 0
    ? availableMcpResources
        .filter(r => r.contentType === 'tool')
        .map(r => ({
          value: r.mcpIdentifier,
          label: r.name,
          description: r.description || '',
          llmDescription: r.useCases || r.description || ''
        }))
    : [
        { 
          value: 'create_support_ticket', 
          label: 'Create Support Ticket', 
          description: 'Create a new support ticket in your system',
          llmDescription: 'Create a support ticket when the user reports an issue that needs tracking, escalation, or follow-up. Use this for technical problems, bugs, or requests that cannot be immediately resolved.'
        },
        { 
          value: 'get_order_status', 
          label: 'Get Order Status', 
          description: 'Fetch order information by order ID',
          llmDescription: 'Retrieve order details when the user provides an order number or asks about their order status. Extract the order ID from the conversation.'
        },
        { 
          value: 'get_tracking_info', 
          label: 'Get Tracking Info', 
          description: 'Get shipping tracking details',
          llmDescription: 'Get shipping tracking information when the order has been shipped and the user wants delivery updates, tracking numbers, or estimated arrival times.'
        },
        { 
          value: 'update_customer_profile', 
          label: 'Update Customer Profile', 
          description: 'Update customer information',
          llmDescription: 'Update customer profile information when the user explicitly requests to change their details, such as email, phone number, address, or payment method.'
        },
        { 
          value: 'search_knowledge_base', 
          label: 'Search Knowledge Base', 
          description: 'Perform custom knowledge search',
          llmDescription: 'Search the knowledge base when the user asks questions that might be answered by documentation, FAQs, or help articles. Use this to find relevant information before responding.'
        },
        { 
          value: 'create_calendar_event', 
          label: 'Create Calendar Event', 
          description: 'Schedule appointments or meetings',
          llmDescription: 'Schedule a calendar event when the user provides a specific date and time for a meeting, demo, or appointment, or when they agree to a suggested time slot.'
        },
      ];

  const integrations = [
    { 
      service: 'slack', 
      actions: [
        { value: 'send_message', llmDescription: 'Send a direct message to a Slack user when you need to notify a specific team member about the conversation.' },
        { value: 'notify_channel', llmDescription: 'Post a message to a Slack channel when the issue requires team visibility, is critical, or needs collaborative attention.' },
        { value: 'create_thread', llmDescription: 'Create a threaded conversation in Slack when you want to start a discussion about a specific topic or issue.' },
        { value: 'alert_team', llmDescription: 'Send alerts to specific team members via Slack when urgent action is needed, critical issues arise, or escalation is required.' }
      ],
      description: 'Send notifications to Slack channels or users'
    },
    { 
      service: 'salesforce', 
      actions: [
        { value: 'create_lead', llmDescription: 'Create a new lead in Salesforce when the user shows buying intent, asks about pricing, requests a demo, or is a potential customer.' },
        { value: 'log_lead', llmDescription: 'Log lead activity in Salesforce when the user engages with pricing information, product features, or shows interest in your services.' },
        { value: 'update_contact', llmDescription: 'Update contact information in Salesforce when the user provides new details like email, phone, company, or preferences.' },
        { value: 'create_case', llmDescription: 'Create a support case in Salesforce when the user reports an issue, bug, or problem that needs to be tracked and resolved.' }
      ],
      description: 'Manage leads and cases in Salesforce'
    },
    { 
      service: 'hubspot', 
      actions: [
        { value: 'create_contact', llmDescription: 'Create a new contact in HubSpot when you identify a new prospect or when the user provides their contact information.' },
        { value: 'log_interaction', llmDescription: 'Log an interaction in HubSpot to track customer engagement, conversations, or touchpoints for sales and marketing purposes.' },
        { value: 'create_deal', llmDescription: 'Create a deal in HubSpot when the user shows strong buying intent, requests a quote, or moves to a sales stage like demo scheduled.' },
        { value: 'add_to_list', llmDescription: 'Add the contact to a HubSpot list when they match specific criteria, such as pricing interest, demo requests, or newsletter signups.' }
      ],
      description: 'Track contacts and deals in HubSpot'
    },
    { 
      service: 'zendesk', 
      actions: [
        { value: 'create_ticket', llmDescription: 'Create a Zendesk ticket when the user reports a support issue, bug, or problem that requires tracking and resolution.' },
        { value: 'update_ticket', llmDescription: 'Update an existing Zendesk ticket when new information is provided, status changes, or additional details are needed.' },
        { value: 'add_comment', llmDescription: 'Add a comment to a Zendesk ticket when you need to provide updates, ask for clarification, or add context to an existing issue.' }
      ],
      description: 'Manage support tickets in Zendesk'
    },
    { 
      service: 'sendgrid', 
      actions: [
        { value: 'send_email', llmDescription: 'Send an email via SendGrid when you need to deliver information, confirmations, or follow-ups that require email delivery.' },
        { value: 'add_to_list', llmDescription: 'Add the contact to a SendGrid email list when they subscribe, opt-in, or request to receive marketing communications.' },
        { value: 'trigger_campaign', llmDescription: 'Trigger a SendGrid email campaign when the user matches campaign criteria, such as completing a signup or showing interest in a product.' }
      ],
      description: 'Send emails and manage campaigns'
    },
  ];

  const handleSave = () => {
    let action: any = {
      description: description.trim() || undefined,
    };

    if (actionType === 'mcp') {
      action = {
        ...action,
        type: 'mcp',
        tool: mcpTool,
        config: JSON.parse(mcpConfig || '{}'),
      };
    } else if (actionType === 'api') {
      action = {
        ...action,
        type: 'api',
        endpoint: apiEndpoint,
        method: apiMethod,
        headers: JSON.parse(apiHeaders || '{}'),
        body: apiMethod !== 'GET' ? JSON.parse(apiBody || '{}') : undefined,
      };
    } else if (actionType === 'integration') {
      action = {
        ...action,
        type: 'integration',
        service: integrationService,
        action: integrationAction,
        config: JSON.parse(integrationConfig || '{}'),
      };
    }

    onSave(action);
  };

  const selectedIntegration = integrations.find(i => i.service === integrationService);

  return (
    <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-10">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md md:max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-4 md:px-6 py-4 flex items-center justify-between">
          <h3 className="text-gray-900">Add Action</h3>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 md:p-6 space-y-6">
          {/* Action Type Selection */}
          <div>
            <label className="block text-gray-700 mb-3 text-sm">Action Type</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                type="button"
                onClick={() => setActionType('mcp')}
                className={`p-4 border-2 rounded-lg text-center transition-all ${
                  actionType === 'mcp'
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <Code className={`w-6 h-6 mx-auto mb-2 ${actionType === 'mcp' ? 'text-purple-600' : 'text-gray-600'}`} />
                <div className={`text-sm ${actionType === 'mcp' ? 'text-purple-900' : 'text-gray-900'}`}>
                  MCP Tool
                </div>
                <p className={`text-xs mt-1 ${actionType === 'mcp' ? 'text-purple-700' : 'text-gray-600'}`}>
                  Pre-built tools
                </p>
              </button>

              <button
                type="button"
                onClick={() => setActionType('api')}
                className={`p-4 border-2 rounded-lg text-center transition-all ${
                  actionType === 'api'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <Link2 className={`w-6 h-6 mx-auto mb-2 ${actionType === 'api' ? 'text-blue-600' : 'text-gray-600'}`} />
                <div className={`text-sm ${actionType === 'api' ? 'text-blue-900' : 'text-gray-900'}`}>
                  Custom API
                </div>
                <p className={`text-xs mt-1 ${actionType === 'api' ? 'text-blue-700' : 'text-gray-600'}`}>
                  Your endpoints
                </p>
              </button>

              <button
                type="button"
                onClick={() => setActionType('integration')}
                className={`p-4 border-2 rounded-lg text-center transition-all ${
                  actionType === 'integration'
                    ? 'border-green-500 bg-green-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <Zap className={`w-6 h-6 mx-auto mb-2 ${actionType === 'integration' ? 'text-green-600' : 'text-gray-600'}`} />
                <div className={`text-sm ${actionType === 'integration' ? 'text-green-900' : 'text-gray-900'}`}>
                  Integration
                </div>
                <p className={`text-xs mt-1 ${actionType === 'integration' ? 'text-green-700' : 'text-gray-600'}`}>
                  3rd party apps
                </p>
              </button>
            </div>
          </div>

          {/* Action Description */}
          <div>
            <label className="block text-gray-700 mb-2 text-sm">
              Action Description
              <span className="text-gray-500 text-xs ml-2">(The LLM uses this to decide when to execute this action)</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              placeholder="Describe when this action should be executed. Be specific about the conditions, context, or user intent that should trigger this action. The LLM will use this description to intelligently decide when to call this action."
            />
            <p className="text-xs text-gray-500 mt-1">
              Example: "Create a support ticket when the user reports a technical issue that needs tracking or escalation."
            </p>
          </div>

          {/* MCP Configuration */}
          {actionType === 'mcp' && (
            <div className="space-y-4 p-4 bg-purple-50 border border-purple-200 rounded-lg">
              <div>
                <label className="block text-gray-700 mb-2 text-sm">Select MCP Tool</label>
                {loadingResources ? (
                  <div className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-sm text-gray-500">
                    Loading MCP resources...
                  </div>
                ) : (
                  <>
                    <select
                      value={mcpTool}
                      onChange={(e) => {
                        setMcpTool(e.target.value);
                        const selectedTool = mcpTools.find(t => t.value === e.target.value);
                        if (selectedTool?.llmDescription && !description) {
                          setDescription(selectedTool.llmDescription);
                        }
                      }}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    >
                      <option value="">Choose a tool...</option>
                      {mcpTools.map(tool => (
                        <option key={tool.value} value={tool.value}>
                          {tool.label}
                        </option>
                      ))}
                    </select>
                    {mcpTool && (
                      <p className="text-xs text-gray-600 mt-1">
                        {mcpTools.find(t => t.value === mcpTool)?.description}
                      </p>
                    )}
                    {availableMcpResources.length === 0 && (
                      <p className="text-xs text-amber-600 mt-1">
                        No MCP resources found. Sync resources in the Integrations page first.
                      </p>
                    )}
                  </>
                )}
              </div>

              <div>
                <label className="block text-gray-700 mb-2 text-sm">Configuration (JSON)</label>
                <textarea
                  value={mcpConfig}
                  onChange={(e) => setMcpConfig(e.target.value)}
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  placeholder='{"priority": "high", "category": "technical"}'
                />
                <p className="text-xs text-gray-600 mt-1">
                  Tool-specific parameters (optional)
                </p>
              </div>

              {/* Examples */}
              <div className="pt-3 border-t border-purple-200">
                <div className="text-xs text-gray-700 mb-2">Example Configurations:</div>
                <div className="space-y-2 text-xs">
                  <button
                    type="button"
                    onClick={() => {
                      setMcpConfig('{"priority": "high", "category": "technical"}');
                      if (!description) {
                        setDescription('Create a support ticket when the user reports a technical issue that needs tracking or escalation. Use this when the problem cannot be immediately resolved or requires follow-up.');
                      }
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-purple-200 rounded hover:bg-purple-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Support Ticket (High Priority)</div>
                    <code className="text-purple-700">{`{"priority": "high", "category": "technical"}`}</code>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setMcpConfig('{"assignee": "support-team", "tags": ["bug", "urgent"]}');
                      if (!description) {
                        setDescription('Create a support ticket when the user reports a technical issue that needs tracking or escalation. Use this when the problem cannot be immediately resolved or requires follow-up.');
                      }
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-purple-200 rounded hover:bg-purple-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Assign to Team with Tags</div>
                    <code className="text-purple-700">{`{"assignee": "support-team", "tags": ["bug"]}`}</code>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* API Configuration */}
          {actionType === 'api' && (
            <div className="space-y-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div>
                <label className="block text-gray-700 mb-2 text-sm">HTTP Method</label>
                <select
                  value={apiMethod}
                  onChange={(e) => setApiMethod(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="PATCH">PATCH</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>

              <div>
                <label className="block text-gray-700 mb-2 text-sm">API Endpoint</label>
                <input
                  type="text"
                  value={apiEndpoint}
                  onChange={(e) => setApiEndpoint(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="/api/orders/{order_id}"
                />
                <p className="text-xs text-gray-600 mt-1">
                  Use {'{variable}'} for dynamic values extracted from conversation
                </p>
              </div>

              <div>
                <label className="block text-gray-700 mb-2 text-sm">Headers (JSON)</label>
                <textarea
                  value={apiHeaders}
                  onChange={(e) => setApiHeaders(e.target.value)}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder='{"Authorization": "Bearer YOUR_TOKEN", "Content-Type": "application/json"}'
                />
              </div>

              {apiMethod !== 'GET' && (
                <div>
                  <label className="block text-gray-700 mb-2 text-sm">Request Body (JSON)</label>
                  <textarea
                    value={apiBody}
                    onChange={(e) => setApiBody(e.target.value)}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder='{"customer_id": "{user_id}", "message": "{user_message}"}'
                  />
                </div>
              )}

              {/* Examples */}
              <div className="pt-3 border-t border-blue-200">
                <div className="text-xs text-gray-700 mb-2">Example APIs:</div>
                <div className="space-y-2 text-xs">
                  <button
                    type="button"
                    onClick={() => {
                      setApiMethod('GET');
                      setApiEndpoint('/api/orders/{order_id}');
                      setApiHeaders('{"Authorization": "Bearer YOUR_API_KEY"}');
                      setDescription('Fetch order details when the user provides an order number or asks about their order status. Extract order_id from the conversation context.');
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-blue-200 rounded hover:bg-blue-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Get Order Status</div>
                    <code className="text-blue-700">{`GET /api/orders/{order_id}`}</code>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setApiMethod('POST');
                      setApiEndpoint('/api/support/tickets');
                      setApiHeaders('{"Authorization": "Bearer YOUR_API_KEY", "Content-Type": "application/json"}');
                      setApiBody('{"subject": "Support Request", "description": "{user_message}", "priority": "high"}');
                      setDescription('Create a support ticket when the user reports an issue that needs tracking, escalation, or follow-up. Use this for technical problems, bugs, or requests that cannot be immediately resolved.');
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-blue-200 rounded hover:bg-blue-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Create Support Ticket</div>
                    <code className="text-blue-700">POST /api/support/tickets</code>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Integration Configuration */}
          {actionType === 'integration' && (
            <div className="space-y-4 p-4 bg-green-50 border border-green-200 rounded-lg">
              <div>
                <label className="block text-gray-700 mb-2 text-sm">Service</label>
                <select
                  value={integrationService}
                  onChange={(e) => {
                    setIntegrationService(e.target.value);
                    setIntegrationAction('');
                  }}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                >
                  <option value="">Choose a service...</option>
                  {integrations.map(int => (
                    <option key={int.service} value={int.service}>
                      {int.service.charAt(0).toUpperCase() + int.service.slice(1)}
                    </option>
                  ))}
                </select>
                {selectedIntegration && (
                  <p className="text-xs text-gray-600 mt-1">
                    {selectedIntegration.description}
                  </p>
                )}
              </div>

              {integrationService && (
                <div>
                  <label className="block text-gray-700 mb-2 text-sm">Action</label>
                  <select
                    value={integrationAction}
                    onChange={(e) => {
                      setIntegrationAction(e.target.value);
                      const selectedAction = selectedIntegration?.actions.find((a: any) => 
                        (typeof a === 'string' ? a : a.value) === e.target.value
                      );
                      if (selectedAction && typeof selectedAction === 'object' && selectedAction.llmDescription && !description) {
                        setDescription(selectedAction.llmDescription);
                      }
                    }}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                  >
                    <option value="">Choose an action...</option>
                    {selectedIntegration?.actions.map((action: any) => {
                      const actionValue = typeof action === 'string' ? action : action.value;
                      const actionLabel = typeof action === 'string' 
                        ? action.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
                        : action.value.split('_').map((w: string) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
                      return (
                        <option key={actionValue} value={actionValue}>
                          {actionLabel}
                        </option>
                      );
                    })}
                  </select>
                </div>
              )}

              <div>
                <label className="block text-gray-700 mb-2 text-sm">Configuration (JSON)</label>
                <textarea
                  value={integrationConfig}
                  onChange={(e) => setIntegrationConfig(e.target.value)}
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:ring-2 focus:ring-green-500 focus:border-transparent"
                  placeholder='{"channel": "#support", "message": "New ticket created"}'
                />
              </div>

              {/* Examples */}
              {integrationService === 'slack' && (
                <div className="pt-3 border-t border-green-200">
                  <div className="text-xs text-gray-700 mb-2">Slack Examples:</div>
                  <div className="space-y-2 text-xs">
                  <button
                    type="button"
                    onClick={() => {
                      setIntegrationAction('notify_channel');
                      setIntegrationConfig('{"channel": "#tech-support", "message": "🚨 New critical issue reported"}');
                      setDescription('Post a message to the tech support Slack channel when the issue is critical, affects multiple users, or requires immediate team attention.');
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-green-200 rounded hover:bg-green-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Notify Support Channel</div>
                    <code className="text-green-700">{`{"channel": "#tech-support"}`}</code>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setIntegrationAction('alert_team');
                      setIntegrationConfig('{"users": ["@support-lead"], "priority": "urgent"}');
                      setDescription('Send alerts to specific team members via Slack when urgent action is needed, critical issues arise, or escalation is required.');
                    }}
                    className="block w-full text-left px-3 py-2 bg-white border border-green-200 rounded hover:bg-green-50 transition-colors"
                  >
                    <div className="text-gray-900 mb-1">Alert Specific User</div>
                    <code className="text-green-700">{`{"users": ["@support-lead"]}`}</code>
                  </button>
                  </div>
                </div>
              )}

              {integrationService === 'salesforce' && (
                <div className="pt-3 border-t border-green-200">
                  <div className="text-xs text-gray-700 mb-2">Salesforce Examples:</div>
                  <div className="space-y-2 text-xs">
                    <button
                      type="button"
                      onClick={() => {
                        setIntegrationAction('log_lead');
                        setIntegrationConfig('{"source": "chatbot", "status": "new", "campaign": "q4-2024"}');
                        setDescription('Log lead activity in Salesforce when the user engages with pricing information, product features, or shows interest in your services.');
                      }}
                      className="block w-full text-left px-3 py-2 bg-white border border-green-200 rounded hover:bg-green-50 transition-colors"
                    >
                      <div className="text-gray-900 mb-1">Create New Lead</div>
                      <code className="text-green-700">{`{"source": "chatbot", "status": "new"}`}</code>
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setIntegrationAction('create_case');
                        setIntegrationConfig('{"type": "Technical", "priority": "High", "origin": "Chat"}');
                        setDescription('Create a support case in Salesforce when the user reports an issue, bug, or problem that needs to be tracked and resolved.');
                      }}
                      className="block w-full text-left px-3 py-2 bg-white border border-green-200 rounded hover:bg-green-50 transition-colors"
                    >
                      <div className="text-gray-900 mb-1">Create Support Case</div>
                      <code className="text-green-700">{`{"type": "Technical", "priority": "High"}`}</code>
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-4 md:px-6 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <p className="text-xs text-gray-600">
            The LLM will intelligently choose which actions to execute based on the conversation context and action descriptions
          </p>
          <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors w-full sm:w-auto"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={
                (actionType === 'mcp' && !mcpTool) ||
                (actionType === 'api' && !apiEndpoint) ||
                (actionType === 'integration' && (!integrationService || !integrationAction)) ||
                loadingResources
              }
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 w-full sm:w-auto"
            >
              <Plus className="w-4 h-4" />
              Add Action
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
