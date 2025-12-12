import { useState, useEffect } from 'react';
import { Save, Bot, Brain, Sparkles, Key, Eye, EyeOff, ExternalLink, CheckCircle, AlertCircle, Zap, Loader2 } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';
import { apiGet, apiPut, apiPost } from '../utils/api';

interface BotSettingsProps {
  onNavigate: (screen: string) => void;
}

interface AiModel {
  id: number;
  modelCode: string;
  modelName: string;
  description: string;
  icon: string;
  isRecommended: boolean;
  status: string;
  isAvailable: boolean;
  isDeprecated: boolean;
  deprecationNotice?: string;
}

interface BotConfig {
  id?: number;
  botName: string;
  welcomeMessage: string;
  language: string;
  autoDetectLanguage: boolean;
  aiMode: 'managed' | 'own';
  managedModelId?: number;
  managedModel?: AiModel;
  personalityInstructions: string;
  showTypingIndicator: boolean;
  showSourceDocuments: boolean;
  rememberConversation: boolean;
  conversationMemoryLimit: number;
  hasApiKey: boolean;
}

export function BotSettings({ onNavigate }: BotSettingsProps) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  
  const [botConfig, setBotConfig] = useState<BotConfig>({
    botName: 'DelightChat Assistant',
    welcomeMessage: 'Hi! I\'m DelightChat. How can I help you today?',
    language: 'en',
    autoDetectLanguage: true,
    aiMode: 'managed',
    personalityInstructions: '',
    showTypingIndicator: true,
    showSourceDocuments: true,
    rememberConversation: true,
    conversationMemoryLimit: 20,
    hasApiKey: false,
  });
  
  const [models, setModels] = useState<AiModel[]>([]);
  const [selectedManagedModelId, setSelectedManagedModelId] = useState<number | null>(null);
  const [showApiKey, setShowApiKey] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [testingConnection, setTestingConnection] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<{ success: boolean; message: string } | null>(null);
  const [savingKey, setSavingKey] = useState(false);

  // Load bot config and models on mount
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      // Load bot config and models in parallel
      const [configResponse, modelsResponse] = await Promise.all([
        apiGet<BotConfig>('/rag/bot').catch(() => null), // 404 is OK - no config exists yet
        apiGet<AiModel[]>('/rag/models'),
      ]);

      if (modelsResponse) {
        setModels(modelsResponse);
      }

      if (configResponse) {
        setBotConfig(configResponse);
        
        // Set selected model ID from config, but validate it exists in models list
        const configModelId = configResponse.managedModelId;
        if (configModelId && modelsResponse) {
          // Verify the model exists in the available models
          const modelExists = modelsResponse.some(m => m.id === configModelId && m.isAvailable);
          if (modelExists) {
            setSelectedManagedModelId(configModelId);
          } else {
            // Model from config doesn't exist or isn't available - use recommended or first
            console.warn(`Model ID ${configModelId} from config not found in available models, selecting recommended/default`);
            const recommended = modelsResponse.find(m => m.isRecommended && m.isAvailable);
            setSelectedManagedModelId(recommended?.id || modelsResponse.find(m => m.isAvailable)?.id || null);
          }
        } else if (!configModelId && modelsResponse && modelsResponse.length > 0) {
          // No model selected in config - select recommended or first available
          const recommended = modelsResponse.find(m => m.isRecommended && m.isAvailable);
          setSelectedManagedModelId(recommended?.id || modelsResponse.find(m => m.isAvailable)?.id || null);
        } else {
          setSelectedManagedModelId(null);
        }
      } else if (modelsResponse && modelsResponse.length > 0) {
        // No config exists yet - select recommended or first available
        const recommended = modelsResponse.find(m => m.isRecommended && m.isAvailable);
        setSelectedManagedModelId(recommended?.id || modelsResponse.find(m => m.isAvailable)?.id || null);
      }
    } catch (err: any) {
      console.error('Error loading data:', err);
      setError(err.message || 'Failed to load bot settings');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSuccessMessage(null);
    
    try {
      const requestBody = {
        botName: botConfig.botName,
        welcomeMessage: botConfig.welcomeMessage,
        language: botConfig.language,
        autoDetectLanguage: botConfig.autoDetectLanguage,
        aiMode: botConfig.aiMode,
        managedModelId: selectedManagedModelId,
        personalityInstructions: botConfig.personalityInstructions,
        showTypingIndicator: botConfig.showTypingIndicator,
        showSourceDocuments: botConfig.showSourceDocuments,
        rememberConversation: botConfig.rememberConversation,
        conversationMemoryLimit: botConfig.conversationMemoryLimit,
      };

      const response = await apiPut<BotConfig>('/rag/bot', requestBody);
      setBotConfig(response);
      setSuccessMessage('Bot settings saved successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      console.error('Error saving bot config:', err);
      setError(err.message || 'Failed to save bot settings');
    } finally {
      setSaving(false);
    }
  };

  const handleTestConnection = async () => {
    if (!apiKey.trim()) {
      setConnectionStatus({ success: false, message: 'Please enter an API key' });
      return;
    }

    setTestingConnection(true);
    setConnectionStatus(null);
    
    try {
      const response = await apiPost<{ success: boolean; message: string }>('/rag/bot/test-connection', {
        apiKey: apiKey,
      });
      
      setConnectionStatus({
        success: response.success,
        message: response.message,
      });
    } catch (err: any) {
      console.error('Error testing connection:', err);
      setConnectionStatus({
        success: false,
        message: err.message || 'Failed to test connection',
      });
    } finally {
      setTestingConnection(false);
    }
  };

  const handleSaveKey = async () => {
    if (!apiKey.trim()) {
      setError('Please enter an API key');
      return;
    }

    setSavingKey(true);
    setError(null);
    setSuccessMessage(null);
    
    try {
      await apiPost<{ success: boolean; message: string }>('/rag/bot/api-key', {
        apiKey: apiKey,
      });
      
      setSuccessMessage('API key saved successfully!');
      setApiKey(''); // Clear the input for security
      setShowApiKey(false);
      
      // Reload config to update hasApiKey status
      await loadData();
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      console.error('Error saving API key:', err);
      setError(err.message || 'Failed to save API key');
    } finally {
      setSavingKey(false);
    }
  };

  const selectedManagedModel = models.find(m => m.id === selectedManagedModelId);

  if (loading) {
    return (
      <div className="p-4 md:p-6 lg:p-8">
        <BackToSettings onNavigate={onNavigate} />
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      
      {/* Error/Success Messages */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <div className="text-red-900 mb-1">Error</div>
            <div className="text-sm text-red-700">{error}</div>
          </div>
        </div>
      )}
      
      {successMessage && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
          <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <div className="text-green-900 mb-1">Success</div>
            <div className="text-sm text-green-700">{successMessage}</div>
          </div>
        </div>
      )}

      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Assistant Settings</h1>
          <p className="text-gray-600 text-sm md:text-base">Configure your AI assistant's identity and intelligence</p>
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {saving ? (
            <>
              <Loader2 className="w-5 h-5 animate-spin" />
              Saving...
            </>
          ) : (
            <>
              <Save className="w-5 h-5" />
              Save Changes
            </>
          )}
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Settings */}
        <div className="lg:col-span-2 space-y-6">
          {/* Basic Info */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-6 flex items-center gap-2">
              <Bot className="w-5 h-5 text-blue-600" />
              Bot Identity
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2">Bot Name</label>
                <input
                  type="text"
                  value={botConfig.botName}
                  onChange={(e) => setBotConfig({ ...botConfig, botName: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="Enter bot name"
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-2">Welcome Message</label>
                <textarea
                  rows={3}
                  value={botConfig.welcomeMessage}
                  onChange={(e) => setBotConfig({ ...botConfig, welcomeMessage: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
                  placeholder="Hi! I'm DelightChat. How can I help you today?"
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-2">Language</label>
                <select
                  value={botConfig.language}
                  onChange={(e) => setBotConfig({ ...botConfig, language: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                  <option value="en">English</option>
                  <option value="es">Spanish</option>
                  <option value="fr">French</option>
                  <option value="de">German</option>
                  <option value="ja">Japanese</option>
                  <option value="zh">Chinese</option>
                </select>
              </div>
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Auto-detect Customer Language</div>
                  <div className="text-gray-600 text-sm">Respond in the language your customer uses</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={botConfig.autoDetectLanguage}
                    onChange={(e) => setBotConfig({ ...botConfig, autoDetectLanguage: e.target.checked })}
                  />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>
            </div>
          </div>

          {/* AI Configuration */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <div className="mb-6">
              <h2 className="text-gray-900 mb-2 flex items-center gap-2">
                <Brain className="w-5 h-5 text-purple-600" />
                AI Configuration
              </h2>
              <p className="text-sm text-gray-600">
                Choose how to power your AI assistant
              </p>
            </div>

            {/* Mode Toggle */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
              <button
                onClick={() => setBotConfig({ ...botConfig, aiMode: 'managed' })}
                className={`p-4 border-2 rounded-lg transition-all ${
                  botConfig.aiMode === 'managed'
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <Zap className={`w-5 h-5 ${botConfig.aiMode === 'managed' ? 'text-purple-600' : 'text-gray-400'}`} />
                  <span className="text-gray-900">Managed AI</span>
                  {botConfig.aiMode === 'managed' && <CheckCircle className="w-4 h-4 text-purple-600 ml-auto" />}
                </div>
                <p className="text-sm text-gray-600 text-left">
                  We handle everything. Pay per use on top of your subscription.
                </p>
              </button>

              <button
                onClick={() => setBotConfig({ ...botConfig, aiMode: 'own' })}
                className={`p-4 border-2 rounded-lg transition-all ${
                  botConfig.aiMode === 'own'
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <Key className={`w-5 h-5 ${botConfig.aiMode === 'own' ? 'text-purple-600' : 'text-gray-400'}`} />
                  <span className="text-gray-900">Your Own AI</span>
                  {botConfig.aiMode === 'own' && <CheckCircle className="w-4 h-4 text-purple-600 ml-auto" />}
                </div>
                <p className="text-sm text-gray-600 text-left">
                  Use your own OpenAI API key. You pay OpenAI directly.
                </p>
              </button>
            </div>

            {/* Managed AI */}
            {botConfig.aiMode === 'managed' && (
              <div className="space-y-4">
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <CheckCircle className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-gray-900 mb-1">Simple Pay-As-You-Go</p>
                      <p className="text-xs text-gray-700">
                        Choose any AI model and pay only for what you use. Costs are added to your monthly bill. We handle all the setup and management.
                      </p>
                    </div>
                  </div>
                </div>

                <div>
                  <label className="block text-gray-700 mb-3">Choose AI Model</label>
                  {models.length === 0 ? (
                    <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg text-center text-gray-600">
                      No models available
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 gap-3">
                      {models
                        .filter(m => m.isAvailable)
                        .map((model) => (
                          <button
                            key={model.id}
                            onClick={() => setSelectedManagedModelId(model.id)}
                            className={`p-4 border-2 rounded-lg text-left transition-all relative ${
                              selectedManagedModelId === model.id
                                ? 'border-purple-500 bg-purple-50'
                                : 'border-gray-200 hover:border-purple-300'
                            } ${!model.isAvailable ? 'opacity-50 cursor-not-allowed' : ''}`}
                            disabled={!model.isAvailable}
                          >
                            {model.isRecommended && (
                              <div className="absolute -top-2 -right-2 px-2 py-1 bg-orange-500 text-white text-xs rounded-full">
                                Recommended
                              </div>
                            )}
                            {model.isDeprecated && (
                              <div className="absolute -top-2 -left-2 px-2 py-1 bg-yellow-500 text-white text-xs rounded-full">
                                Deprecated
                              </div>
                            )}
                            <div className="flex items-start justify-between">
                              <div className="flex items-start gap-3 flex-1">
                                <div className="text-2xl">{model.icon || '🤖'}</div>
                                <div className="flex-1">
                                  <div className="flex items-center gap-2 mb-1">
                                    <span className="text-gray-900">{model.modelName}</span>
                                    <span className="text-xs px-2 py-0.5 bg-gray-200 text-gray-700 rounded">
                                      OpenAI
                                    </span>
                                  </div>
                                  <p className="text-sm text-gray-600 mb-2">{model.description}</p>
                                  {model.isDeprecated && model.deprecationNotice && (
                                    <p className="text-xs text-yellow-700 mb-2">{model.deprecationNotice}</p>
                                  )}
                                </div>
                              </div>
                              {selectedManagedModelId === model.id && (
                                <CheckCircle className="w-5 h-5 text-purple-600 flex-shrink-0 ml-2" />
                              )}
                            </div>
                          </button>
                        ))}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Your Own AI - OpenAI Only */}
            {botConfig.aiMode === 'own' && (
              <div className="space-y-4">
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <Key className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-gray-900 mb-1">Connect Your OpenAI Account</p>
                      <p className="text-xs text-gray-700">
                        Use your own OpenAI API key. You pay OpenAI directly.
                      </p>
                    </div>
                  </div>
                </div>

                {botConfig.hasApiKey && (
                  <div className="p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <div className="text-green-900 mb-1">API Key Saved</div>
                      <div className="text-sm text-green-700">
                        Your OpenAI API key is saved and encrypted. You can update it below.
                      </div>
                    </div>
                  </div>
                )}

                <div className="pt-4 border-t border-gray-200">
                  <div className="mb-4">
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-gray-700">OpenAI API Key</label>
                      <a
                        href="https://platform.openai.com/api-keys"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm text-blue-600 hover:underline flex items-center gap-1"
                      >
                        Get Your Key
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                    <div className="relative">
                      <input
                        type={showApiKey ? 'text' : 'password'}
                        value={apiKey}
                        onChange={(e) => {
                          setApiKey(e.target.value);
                          setConnectionStatus(null);
                        }}
                        className="w-full px-4 py-2 pr-24 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none font-mono text-sm"
                        placeholder={botConfig.hasApiKey ? "Enter new API key to update" : "Paste your API key here"}
                      />
                      <button
                        onClick={() => setShowApiKey(!showApiKey)}
                        className="absolute right-2 top-1/2 -translate-y-1/2 p-2 hover:bg-gray-100 rounded"
                      >
                        {showApiKey ? (
                          <EyeOff className="w-4 h-4 text-gray-600" />
                        ) : (
                          <Eye className="w-4 h-4 text-gray-600" />
                        )}
                      </button>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      Your API key is encrypted and secure. We never share it with anyone.
                    </p>
                  </div>

                  <div className="flex gap-3">
                    <button
                      onClick={handleTestConnection}
                      disabled={!apiKey.trim() || testingConnection}
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                      {testingConnection ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin" />
                          Testing...
                        </>
                      ) : (
                        'Test Connection'
                      )}
                    </button>
                    <button
                      onClick={handleSaveKey}
                      disabled={!apiKey.trim() || savingKey}
                      className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                      {savingKey ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin" />
                          Saving...
                        </>
                      ) : (
                        <>
                          <Key className="w-4 h-4" />
                          Save Key
                        </>
                      )}
                    </button>
                  </div>

                  {connectionStatus && (
                    <div className={`mt-4 p-4 border rounded-lg flex items-start gap-3 ${
                      connectionStatus.success
                        ? 'bg-green-50 border-green-200'
                        : 'bg-red-50 border-red-200'
                    }`}>
                      {connectionStatus.success ? (
                        <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                      ) : (
                        <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                      )}
                      <div>
                        <div className={`mb-1 ${connectionStatus.success ? 'text-green-900' : 'text-red-900'}`}>
                          {connectionStatus.success ? 'Connection Successful!' : 'Connection Failed'}
                        </div>
                        <div className={`text-sm ${connectionStatus.success ? 'text-green-700' : 'text-red-700'}`}>
                          {connectionStatus.message}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Bot Personality */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-6 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-orange-600" />
              Bot Personality & Instructions
            </h2>
            <p className="text-sm text-gray-600 mb-4">
              Tell your AI assistant how to behave and what tone to use when talking to customers
            </p>
            <textarea
              rows={10}
              value={botConfig.personalityInstructions}
              onChange={(e) => setBotConfig({ ...botConfig, personalityInstructions: e.target.value })}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
              placeholder="You are a friendly and helpful customer support assistant..."
            />
            <p className="text-xs text-gray-500 mt-2">
              💡 Tip: Be specific about tone, style, and how to handle different situations
            </p>
          </div>

          {/* Advanced Options */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-6">Advanced Options</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Show Typing Indicator</div>
                  <div className="text-gray-600 text-sm">Display "typing..." while bot thinks</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={botConfig.showTypingIndicator}
                    onChange={(e) => setBotConfig({ ...botConfig, showTypingIndicator: e.target.checked })}
                  />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Show Source Documents</div>
                  <div className="text-gray-600 text-sm">Display which articles were used in the answer</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={botConfig.showSourceDocuments}
                    onChange={(e) => setBotConfig({ ...botConfig, showSourceDocuments: e.target.checked })}
                  />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Remember Conversation</div>
                  <div className="text-gray-600 text-sm">Keep context from previous messages</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={botConfig.rememberConversation}
                    onChange={(e) => setBotConfig({ ...botConfig, rememberConversation: e.target.checked })}
                  />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">How Many Messages to Remember</label>
                <select
                  value={botConfig.conversationMemoryLimit}
                  onChange={(e) => setBotConfig({ ...botConfig, conversationMemoryLimit: parseInt(e.target.value) })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                  <option value={5}>Last 5 messages</option>
                  <option value={10}>Last 10 messages</option>
                  <option value={20}>Last 20 messages (Recommended)</option>
                  <option value={50}>Last 50 messages</option>
                  <option value={100}>Last 100 messages</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Preview */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6 sticky top-8">
            <h2 className="text-gray-900 mb-4">Preview</h2>
            
            {/* Connection Status */}
            <div className="mb-4 p-3 bg-gray-50 rounded-lg border border-gray-200">
              <div className="text-xs text-gray-600 mb-2">AI Provider</div>
              {botConfig.aiMode === 'managed' ? (
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{selectedManagedModel?.icon || '🤖'}</span>
                  <div>
                    <div className="text-sm text-gray-900">{selectedManagedModel?.modelName || 'No model selected'}</div>
                    <div className="text-xs text-green-600 flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" />
                      Managed by us
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-2xl">🤖</span>
                  <div>
                    <div className="text-sm text-gray-900">OpenAI</div>
                    <div className="text-xs text-gray-500">
                      {botConfig.hasApiKey ? (
                        <span className="text-green-600 flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" />
                          Connected
                        </span>
                      ) : (
                        <span>Not connected</span>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Chat Preview */}
            <div className="space-y-4">
              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 bg-gradient-to-br from-blue-600 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <Bot className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <div className="text-gray-900 mb-1">{botConfig.botName}</div>
                    <div className="text-gray-700 text-sm">{botConfig.welcomeMessage}</div>
                  </div>
                </div>
              </div>

              <div className="bg-blue-600 text-white rounded-lg p-4 ml-8">
                <div className="text-sm">What are your business hours?</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 bg-gradient-to-br from-blue-600 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <Bot className="w-5 h-5 text-white" />
                  </div>
                  <div className="text-gray-700 text-sm">
                    We're open Monday through Friday from 9 AM to 6 PM EST. Our team is ready to help during those hours!
                  </div>
                </div>
              </div>
            </div>

            {(botConfig.aiMode === 'managed' || botConfig.hasApiKey) && (
              <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                <div className="text-xs text-green-900 mb-1 flex items-center gap-1">
                  <Brain className="w-3 h-3" />
                  <span>AI Active</span>
                </div>
                <div className="text-xs text-green-700">
                  {botConfig.aiMode === 'managed' 
                    ? `Using ${selectedManagedModel?.modelName || 'No model'}`
                    : 'Using your OpenAI account'
                  }
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
