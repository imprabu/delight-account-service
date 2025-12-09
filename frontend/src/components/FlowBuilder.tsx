import { useState } from 'react';
import { X, Plus, Trash2, Save, Brain, Database, Zap, Code, Link2, Settings as SettingsIcon, HelpCircle } from 'lucide-react';

interface FlowBuilderProps {
  intent: any;
  onSave: (intent: any) => void;
  onClose: () => void;
}

export function FlowBuilder({ intent, onSave, onClose }: FlowBuilderProps) {
  const [name, setName] = useState(intent?.name || '');
  const [semanticDescription, setSemanticDescription] = useState(intent?.semanticDescription || '');
  const [responseStrategy, setResponseStrategy] = useState(intent?.responseStrategy || 'llm_with_rag');
  const [knowledgeBaseTags, setKnowledgeBaseTags] = useState<string[]>(intent?.knowledgeBaseTags || []);
  const [actions, setActions] = useState<any[]>(intent?.actions || []);
  const [fallbackAction, setFallbackAction] = useState(intent?.fallbackAction || 'escalate_to_human');
  const [priority, setPriority] = useState(intent?.priority || 'medium');
  const [newTag, setNewTag] = useState('');
  const [showActionBuilder, setShowActionBuilder] = useState(false);
  const [editingAction, setEditingAction] = useState<any>(null);

  const strategies = [
    {
      value: 'llm_with_rag',
      label: 'LLM + RAG',
      description: 'LLM performs vector search on knowledge base, then generates contextual response using retrieved documents.',
      icon: Brain,
      usesKnowledge: true,
    },
    {
      value: 'knowledge_base',
      label: 'Knowledge Base Only',
      description: 'Direct RAG vector search to find relevant documents. Returns search results without additional LLM generation.',
      icon: Database,
      usesKnowledge: true,
    },
    {
      value: 'action_first',
      label: 'Action First',
      description: 'Execute configured actions first to gather data (API calls, MCPs), then LLM uses results to formulate response.',
      icon: Zap,
      usesKnowledge: false,
    },
    {
      value: 'llm_only',
      label: 'LLM Only',
      description: 'Pure LLM generation without knowledge base retrieval. Uses conversation context and general knowledge only.',
      icon: Brain,
      usesKnowledge: false,
    },
  ];

  const actionTypes = [
    { value: 'mcp', label: 'MCP Tool', description: 'Model Context Protocol tool', icon: Code },
    { value: 'api', label: 'API Call', description: 'REST API endpoint', icon: Link2 },
    { value: 'integration', label: 'Integration', description: 'Third-party service', icon: Zap },
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

  const addAction = () => {
    setEditingAction({
      type: 'mcp',
      trigger: 'always',
    });
    setShowActionBuilder(true);
  };

  const saveAction = () => {
    if (editingAction) {
      const actionIndex = actions.findIndex(a => a.id === editingAction.id);
      if (actionIndex >= 0) {
        const newActions = [...actions];
        newActions[actionIndex] = editingAction;
        setActions(newActions);
      } else {
        setActions([...actions, { ...editingAction, id: Date.now() }]);
      }
      setEditingAction(null);
      setShowActionBuilder(false);
    }
  };

  const deleteAction = (index: number) => {
    setActions(actions.filter((_, i) => i !== index));
  };

  const handleSave = () => {
    onSave({
      ...intent,
      name,
      semanticDescription,
      responseStrategy,
      knowledgeBaseTags,
      actions,
      fallbackAction,
      priority,
      status: intent?.status || 'Draft',
    });
  };

  const selectedStrategy = strategies.find(s => s.value === responseStrategy);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white rounded-xl max-w-4xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 md:p-6 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div className="flex-1 min-w-0">
            <h2 className="text-gray-900 text-lg md:text-xl mb-1">Intent Configuration</h2>
            <p className="text-gray-600 text-sm">Configure LLM-powered intent detection and response strategy</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6">
          <div className="space-y-6">
            {/* Basic Info */}
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2">Intent Name</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="e.g., Technical Support Request, Product Inquiry"
                />
              </div>

              <div>
                <label className="block text-gray-700 mb-2">
                  Semantic Description for LLM
                  <span className="text-gray-500 text-sm ml-2">(Be descriptive - this helps the LLM detect the intent)</span>
                </label>
                <textarea
                  value={semanticDescription}
                  onChange={(e) => setSemanticDescription(e.target.value)}
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
                  placeholder="Describe what this intent means in natural language. Include examples of what users might say, what they're trying to accomplish, and the context around this intent. The more detailed, the better the LLM can detect it."
                />
                <p className="text-xs text-gray-500 mt-1">
                  💡 Example: "User is experiencing a technical problem or bug. They might describe error messages, explain what's not working, or ask how to fix an issue. They need troubleshooting help."
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 mb-2">Priority</label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  >
                    <option value="high">High Priority</option>
                    <option value="medium">Medium Priority</option>
                    <option value="low">Low Priority</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Response Strategy */}
            <div>
              <label className="block text-gray-700 mb-3">Response Strategy</label>
              <div className="grid grid-cols-1 gap-3">
                {strategies.map((strategy) => {
                  const Icon = strategy.icon;
                  const isSelected = responseStrategy === strategy.value;
                  
                  return (
                    <button
                      key={strategy.value}
                      onClick={() => setResponseStrategy(strategy.value)}
                      className={`p-4 border-2 rounded-lg text-left transition-all ${
                        isSelected 
                          ? 'border-blue-500 bg-blue-50' 
                          : 'border-gray-200 hover:border-blue-300'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <div className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                          isSelected ? 'bg-blue-100' : 'bg-gray-100'
                        }`}>
                          <Icon className={`w-5 h-5 ${isSelected ? 'text-blue-600' : 'text-gray-600'}`} />
                        </div>
                        <div className="flex-1">
                          <div className="text-gray-900 mb-1">{strategy.label}</div>
                          <p className="text-sm text-gray-600">{strategy.description}</p>
                        </div>
                        {isSelected && (
                          <div className="w-5 h-5 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
                            <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                          </div>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Knowledge Base Tags */}
            {selectedStrategy?.usesKnowledge && (
              <div>
                <label className="block text-gray-700 mb-2">
                  <Database className="w-4 h-4 inline mr-1" />
                  Knowledge Base Tags
                </label>
                <p className="text-sm text-gray-600 mb-3">
                  Specify which knowledge base collections to search via vector similarity when this intent is detected
                </p>
                
                <div className="flex flex-wrap gap-2 mb-3">
                  {knowledgeBaseTags.map((tag, idx) => (
                    <div key={idx} className="px-3 py-1.5 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700 flex items-center gap-2">
                      <Database className="w-3 h-3" />
                      {tag}
                      <button
                        onClick={() => removeKnowledgeTag(tag)}
                        className="hover:bg-blue-200 rounded p-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && addKnowledgeTag()}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm"
                    placeholder="e.g., product-docs, troubleshooting, faq"
                  />
                  <button
                    onClick={addKnowledgeTag}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                  >
                    <Plus className="w-4 h-4" />
                    Add
                  </button>
                </div>
              </div>
            )}

            {/* Actions */}
            <div>
              <label className="block text-gray-700 mb-2">
                <Zap className="w-4 h-4 inline mr-1" />
                Configurable Actions
              </label>
              <p className="text-sm text-gray-600 mb-3">
                Configure actions to execute when this intent is detected (MCPs, API calls, integrations)
              </p>

              {actions.length === 0 ? (
                <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
                  <Zap className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                  <p className="text-sm text-gray-600 mb-3">No actions configured</p>
                  <button
                    onClick={addAction}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Add First Action
                  </button>
                </div>
              ) : (
                <div className="space-y-3">
                  {actions.map((action, idx) => (
                    <div key={idx} className="border border-gray-200 rounded-lg p-4 flex items-start gap-3">
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
                        <div className="flex items-center gap-2 mb-1">
                          <span className={`text-xs px-2 py-0.5 rounded ${
                            action.type === 'mcp' ? 'bg-purple-100 text-purple-700' :
                            action.type === 'api' ? 'bg-blue-100 text-blue-700' :
                            'bg-green-100 text-green-700'
                          }`}>
                            {action.type.toUpperCase()}
                          </span>
                          <span className="text-xs text-gray-500">Trigger: {action.trigger}</span>
                        </div>
                        <div className="text-sm text-gray-900">
                          {action.type === 'mcp' && `Tool: ${action.tool || 'Not configured'}`}
                          {action.type === 'api' && `${action.method || 'GET'} ${action.endpoint || 'Not configured'}`}
                          {action.type === 'integration' && `${action.service || 'Service'} - ${action.action || 'Action'}`}
                        </div>
                      </div>
                      <button
                        onClick={() => deleteAction(idx)}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <Trash2 className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                  ))}
                  <button
                    onClick={addAction}
                    className="w-full py-2 border-2 border-dashed border-gray-300 rounded-lg text-sm text-gray-600 hover:border-blue-400 hover:text-blue-600 transition-colors"
                  >
                    + Add Another Action
                  </button>
                </div>
              )}
            </div>

            {/* Fallback */}
            <div>
              <label className="block text-gray-700 mb-2">Fallback Strategy</label>
              <p className="text-sm text-gray-600 mb-3">
                What should happen if the LLM can't generate a good response or actions fail?
              </p>
              <select
                value={fallbackAction}
                onChange={(e) => setFallbackAction(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              >
                <option value="escalate_to_human">Escalate to Human Agent</option>
                <option value="ask_clarifying_question">Ask Clarifying Question</option>
                <option value="provide_contact_form">Provide Contact Form</option>
                <option value="suggest_alternatives">Suggest Alternative Resources</option>
                <option value="apologize_and_end">Apologize and End Conversation</option>
              </select>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 md:p-6 border-t border-gray-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 flex-shrink-0 bg-gray-50">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors w-full sm:w-auto order-2 sm:order-1"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full sm:w-auto order-1 sm:order-2"
          >
            <Save className="w-5 h-5" />
            Save Intent
          </button>
        </div>
      </div>

      {/* Action Builder Sub-Modal */}
      {showActionBuilder && editingAction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4 overflow-y-auto">
          <div className="bg-white rounded-xl max-w-2xl w-full my-8 p-4 md:p-6">
            <h3 className="text-gray-900 text-lg mb-4">Configure Action</h3>
            
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2 text-sm">Action Type</label>
                <div className="grid grid-cols-3 gap-3">
                  {actionTypes.map((type) => {
                    const Icon = type.icon;
                    return (
                      <button
                        key={type.value}
                        onClick={() => setEditingAction({ ...editingAction, type: type.value })}
                        className={`p-3 border-2 rounded-lg text-left ${
                          editingAction.type === type.value
                            ? 'border-blue-500 bg-blue-50'
                            : 'border-gray-200 hover:border-blue-300'
                        }`}
                      >
                        <Icon className="w-5 h-5 mb-1 text-gray-700" />
                        <div className="text-sm text-gray-900">{type.label}</div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {editingAction.type === 'mcp' && (
                <div>
                  <label className="block text-gray-700 mb-2 text-sm">MCP Tool Name</label>
                  <input
                    type="text"
                    value={editingAction.tool || ''}
                    onChange={(e) => setEditingAction({ ...editingAction, tool: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                    placeholder="e.g., create_support_ticket, get_user_info"
                  />
                </div>
              )}

              {editingAction.type === 'api' && (
                <>
                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block text-gray-700 mb-2 text-sm">Method</label>
                      <select
                        value={editingAction.method || 'GET'}
                        onChange={(e) => setEditingAction({ ...editingAction, method: e.target.value })}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      >
                        <option>GET</option>
                        <option>POST</option>
                        <option>PUT</option>
                        <option>DELETE</option>
                      </select>
                    </div>
                    <div className="col-span-2">
                      <label className="block text-gray-700 mb-2 text-sm">Endpoint</label>
                      <input
                        type="text"
                        value={editingAction.endpoint || ''}
                        onChange={(e) => setEditingAction({ ...editingAction, endpoint: e.target.value })}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                        placeholder="/api/orders/{order_id}"
                      />
                    </div>
                  </div>
                </>
              )}

              {editingAction.type === 'integration' && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-gray-700 mb-2 text-sm">Service</label>
                    <select
                      value={editingAction.service || ''}
                      onChange={(e) => setEditingAction({ ...editingAction, service: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="">Select...</option>
                      <option value="salesforce">Salesforce</option>
                      <option value="zendesk">Zendesk</option>
                      <option value="slack">Slack</option>
                      <option value="hubspot">HubSpot</option>
                      <option value="stripe">Stripe</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-gray-700 mb-2 text-sm">Action</label>
                    <input
                      type="text"
                      value={editingAction.action || ''}
                      onChange={(e) => setEditingAction({ ...editingAction, action: e.target.value })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      placeholder="e.g., create_lead, send_message"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-gray-700 mb-2 text-sm">Trigger Condition</label>
                <select
                  value={editingAction.trigger || 'always'}
                  onChange={(e) => setEditingAction({ ...editingAction, trigger: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="always">Always</option>
                  <option value="on_success">On Success Only</option>
                  <option value="on_failure">On Failure Only</option>
                  <option value="if_confident">If LLM is Confident</option>
                  <option value="custom">Custom Condition</option>
                </select>
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => {
                  setEditingAction(null);
                  setShowActionBuilder(false);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={saveAction}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Add Action
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
