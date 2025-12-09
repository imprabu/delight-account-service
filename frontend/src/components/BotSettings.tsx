import { useState } from 'react';
import { Save, Bot, Brain, Sparkles, Key, Eye, EyeOff, ExternalLink, CheckCircle, AlertCircle, CreditCard, Zap } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface BotSettingsProps {
  onNavigate: (screen: string) => void;
}

export function BotSettings({ onNavigate }: BotSettingsProps) {
  const [botName, setBotName] = useState('DelightChat Assistant');
  const [aiMode, setAiMode] = useState<'managed' | 'own'>('managed');
  const [selectedProvider, setSelectedProvider] = useState<string>('openai');
  const [selectedManagedModel, setSelectedManagedModel] = useState<string>('gpt-4o');
  const [showApiKey, setShowApiKey] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [isConnected, setIsConnected] = useState(false);

  const managedModels = [
    {
      id: 'gpt-4o',
      provider: 'OpenAI',
      name: 'GPT-4o',
      description: 'Latest and most capable - Best for complex questions',
      costPer1kMessages: '$0.15',
      icon: '🚀',
      recommended: true,
    },
    {
      id: 'gpt-4-turbo',
      provider: 'OpenAI',
      name: 'GPT-4 Turbo',
      description: 'Fast and powerful - Good balance',
      costPer1kMessages: '$0.30',
      icon: '⚡',
      recommended: false,
    },
    {
      id: 'gpt-3.5-turbo',
      provider: 'OpenAI',
      name: 'GPT-3.5',
      description: 'Budget-friendly - Simple questions',
      costPer1kMessages: '$0.02',
      icon: '💨',
      recommended: false,
    },
    {
      id: 'claude-3-opus',
      provider: 'Anthropic',
      name: 'Claude 3 Opus',
      description: 'Most capable Claude - Complex reasoning',
      costPer1kMessages: '$0.45',
      icon: '🔮',
      recommended: false,
    },
    {
      id: 'claude-3-sonnet',
      provider: 'Anthropic',
      name: 'Claude 3 Sonnet',
      description: 'Balanced Claude - Good for most cases',
      costPer1kMessages: '$0.18',
      icon: '✨',
      recommended: false,
    },
  ];

  const ownProviders = [
    {
      id: 'openai',
      name: 'OpenAI',
      logo: '🤖',
      description: 'GPT-4 and GPT-3.5 models',
      keyLabel: 'API Key',
      docsUrl: 'https://platform.openai.com/api-keys',
    },
    {
      id: 'anthropic',
      name: 'Anthropic',
      logo: '🔮',
      description: 'Claude AI models',
      keyLabel: 'API Key',
      docsUrl: 'https://console.anthropic.com/settings/keys',
    },
    {
      id: 'google',
      name: 'Google AI',
      logo: '🌟',
      description: 'Gemini models',
      keyLabel: 'API Key',
      docsUrl: 'https://makersuite.google.com/app/apikey',
    },
    {
      id: 'azure',
      name: 'Azure OpenAI',
      logo: '☁️',
      description: 'Enterprise OpenAI',
      keyLabel: 'API Key',
      docsUrl: 'https://portal.azure.com/',
    },
  ];

  const selectedProviderData = ownProviders.find(p => p.id === selectedProvider);
  const selectedManagedModelData = managedModels.find(m => m.id === selectedManagedModel);

  const handleTestConnection = () => {
    if (apiKey.trim()) {
      setIsConnected(true);
    }
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Assistant Settings</h1>
          <p className="text-gray-600 text-sm md:text-base">Configure your AI assistant's identity and intelligence</p>
        </div>
        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto">
          <Save className="w-5 h-5" />
          Save Changes
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
                  value={botName}
                  onChange={(e) => setBotName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="Enter bot name"
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-2">Welcome Message</label>
                <textarea
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
                  placeholder="Hi! I'm DelightChat. How can I help you today?"
                  defaultValue="Hi! I'm DelightChat. How can I help you today?"
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-2">Language</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
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
                  <input type="checkbox" className="sr-only peer" defaultChecked />
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
                onClick={() => setAiMode('managed')}
                className={`p-4 border-2 rounded-lg transition-all ${
                  aiMode === 'managed'
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <Zap className={`w-5 h-5 ${aiMode === 'managed' ? 'text-purple-600' : 'text-gray-400'}`} />
                  <span className="text-gray-900">Managed AI</span>
                  {aiMode === 'managed' && <CheckCircle className="w-4 h-4 text-purple-600 ml-auto" />}
                </div>
                <p className="text-sm text-gray-600 text-left">
                  We handle everything. Pay per use on top of your subscription.
                </p>
              </button>

              <button
                onClick={() => setAiMode('own')}
                className={`p-4 border-2 rounded-lg transition-all ${
                  aiMode === 'own'
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  <Key className={`w-5 h-5 ${aiMode === 'own' ? 'text-purple-600' : 'text-gray-400'}`} />
                  <span className="text-gray-900">Your Own AI</span>
                  {aiMode === 'own' && <CheckCircle className="w-4 h-4 text-purple-600 ml-auto" />}
                </div>
                <p className="text-sm text-gray-600 text-left">
                  Use your own AI account. You pay your provider directly.
                </p>
              </button>
            </div>

            {/* Managed AI */}
            {aiMode === 'managed' && (
              <div className="space-y-4">
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <CreditCard className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
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
                  <div className="grid grid-cols-1 gap-3">
                    {managedModels.map((model) => (
                      <button
                        key={model.id}
                        onClick={() => setSelectedManagedModel(model.id)}
                        className={`p-4 border-2 rounded-lg text-left transition-all relative ${
                          selectedManagedModel === model.id
                            ? 'border-purple-500 bg-purple-50'
                            : 'border-gray-200 hover:border-purple-300'
                        }`}
                      >
                        {model.recommended && (
                          <div className="absolute -top-2 -right-2 px-2 py-1 bg-orange-500 text-white text-xs rounded-full">
                            Recommended
                          </div>
                        )}
                        <div className="flex items-start justify-between">
                          <div className="flex items-start gap-3 flex-1">
                            <div className="text-2xl">{model.icon}</div>
                            <div className="flex-1">
                              <div className="flex items-center gap-2 mb-1">
                                <span className="text-gray-900">{model.name}</span>
                                <span className="text-xs px-2 py-0.5 bg-gray-200 text-gray-700 rounded">
                                  {model.provider}
                                </span>
                              </div>
                              <p className="text-sm text-gray-600 mb-2">{model.description}</p>
                              <div className="text-xs text-gray-600">
                                ~{model.costPer1kMessages} per 1,000 messages
                              </div>
                            </div>
                          </div>
                          {selectedManagedModel === model.id && (
                            <CheckCircle className="w-5 h-5 text-purple-600 flex-shrink-0 ml-2" />
                          )}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <CheckCircle className="w-4 h-4 text-green-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-green-900 mb-1">Estimated Cost</p>
                      <p className="text-xs text-green-700">
                        Typically $24-$180/month depending on your conversation volume (100-1000 conversations/day)
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Your Own AI */}
            {aiMode === 'own' && (
              <div className="space-y-4">
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <Key className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-sm text-gray-900 mb-1">Connect Your AI Account</p>
                      <p className="text-xs text-gray-700">
                        Already have an account with OpenAI, Anthropic, or Google? Connect it here and pay them directly.
                      </p>
                    </div>
                  </div>
                </div>

                <div>
                  <label className="block text-gray-700 mb-3">Select Your Provider</label>
                  <div className="grid grid-cols-1 gap-3">
                    {ownProviders.map((provider) => (
                      <button
                        key={provider.id}
                        onClick={() => {
                          setSelectedProvider(provider.id);
                          setIsConnected(false);
                          setApiKey('');
                        }}
                        className={`p-4 border-2 rounded-lg text-left transition-all ${
                          selectedProvider === provider.id
                            ? 'border-purple-500 bg-purple-50'
                            : 'border-gray-200 hover:border-purple-300'
                        }`}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex items-start gap-3 flex-1">
                            <div className="text-2xl">{provider.logo}</div>
                            <div className="flex-1">
                              <div className="text-gray-900 mb-1">{provider.name}</div>
                              <p className="text-sm text-gray-600">{provider.description}</p>
                            </div>
                          </div>
                          {selectedProvider === provider.id && (
                            <CheckCircle className="w-5 h-5 text-purple-600 flex-shrink-0 ml-2" />
                          )}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                {/* API Key Input */}
                {selectedProviderData && (
                  <div className="pt-4 border-t border-gray-200">
                    <div className="mb-4">
                      <div className="flex items-center justify-between mb-2">
                        <label className="text-gray-700">{selectedProviderData.keyLabel}</label>
                        <a
                          href={selectedProviderData.docsUrl}
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
                            setIsConnected(false);
                          }}
                          className="w-full px-4 py-2 pr-24 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none font-mono text-sm"
                          placeholder="Paste your API key here"
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

                    {selectedProvider === 'azure' && (
                      <div className="space-y-3 mb-4">
                        <div>
                          <label className="block text-gray-700 mb-2 text-sm">Azure Endpoint URL</label>
                          <input
                            type="text"
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 outline-none text-sm"
                            placeholder="https://your-resource.openai.azure.com/"
                          />
                        </div>
                        <div>
                          <label className="block text-gray-700 mb-2 text-sm">Deployment Name</label>
                          <input
                            type="text"
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 outline-none text-sm"
                            placeholder="my-deployment-name"
                          />
                        </div>
                      </div>
                    )}

                    <div className="flex gap-3">
                      <button
                        onClick={handleTestConnection}
                        disabled={!apiKey.trim()}
                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Test Connection
                      </button>
                      <button
                        disabled={!apiKey.trim()}
                        className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                      >
                        <Key className="w-4 h-4" />
                        Save Key
                      </button>
                    </div>

                    {isConnected && (
                      <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                        <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                        <div>
                          <div className="text-green-900 mb-1">Connected Successfully!</div>
                          <div className="text-sm text-green-700">
                            Your {selectedProviderData.name} account is connected and ready to use.
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}
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
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
              defaultValue={`You are a friendly and helpful customer support assistant. Your goals are to:

1. Understand what the customer needs
2. Search our knowledge base for relevant information
3. Provide clear, helpful answers
4. Be professional but warm and approachable
5. If you don't know something, admit it and offer to connect them with a person
6. Always be patient and empathetic, especially with frustrated customers

Remember: You can search our knowledge base for accurate information. Always try to help!`}
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
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Show Source Documents</div>
                  <div className="text-gray-600 text-sm">Display which articles were used in the answer</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Remember Conversation</div>
                  <div className="text-gray-600 text-sm">Keep context from previous messages</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">How Many Messages to Remember</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                  <option value="5">Last 5 messages</option>
                  <option value="10">Last 10 messages</option>
                  <option value="20">Last 20 messages (Recommended)</option>
                  <option value="50">Last 50 messages</option>
                  <option value="all">Entire conversation</option>
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
              {aiMode === 'managed' ? (
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{selectedManagedModelData?.icon}</span>
                  <div>
                    <div className="text-sm text-gray-900">{selectedManagedModelData?.name}</div>
                    <div className="text-xs text-green-600 flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" />
                      Managed by us
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{selectedProviderData?.logo}</span>
                  <div>
                    <div className="text-sm text-gray-900">{selectedProviderData?.name}</div>
                    <div className="text-xs text-gray-500">
                      {isConnected ? (
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
                    <div className="text-gray-900 mb-1">{botName}</div>
                    <div className="text-gray-700 text-sm">Hi! I'm DelightChat. How can I help you today?</div>
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

            {(aiMode === 'managed' || isConnected) && (
              <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                <div className="text-xs text-green-900 mb-1 flex items-center gap-1">
                  <Brain className="w-3 h-3" />
                  <span>AI Active</span>
                </div>
                <div className="text-xs text-green-700">
                  {aiMode === 'managed' 
                    ? `Using ${selectedManagedModelData?.name}`
                    : `Using your ${selectedProviderData?.name} account`
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