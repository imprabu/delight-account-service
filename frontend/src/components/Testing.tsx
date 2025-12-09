import { useState } from 'react';
import { Send, Bot, User, RotateCcw, Target, Database, Brain, Sparkles, ChevronDown, ChevronUp } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface TestingProps {
  onNavigate: (screen: string) => void;
}

export function Testing({ onNavigate }: TestingProps) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      role: 'bot',
      content: "Hi! I'm DelightChat. How can I help you today?",
      timestamp: '10:30 AM',
    },
  ]);
  const [input, setInput] = useState('');
  const [showDebug, setShowDebug] = useState(true);
  const [debugInfo, setDebugInfo] = useState<any>(null);

  const quickTests = [
    { query: 'I need help with my order #12345', intent: 'Order Status Inquiry' },
    { query: 'How much does the enterprise plan cost?', intent: 'Product Information' },
    { query: 'I\'m getting an error when I try to login', intent: 'Technical Support' },
    { query: 'Can I speak to a human agent?', intent: 'Request Human Support' },
  ];

  const handleSend = () => {
    if (!input.trim()) return;

    const userMessage = {
      id: messages.length + 1,
      role: 'user',
      content: input,
      timestamp: new Date().toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }),
    };

    setMessages([...messages, userMessage]);
    
    // Simulate LLM processing with debug info
    const mockDebug = {
      intentDetection: {
        detectedIntent: 'Technical Support Request',
        confidence: 0.94,
        alternativeIntents: [
          { name: 'Product Information', confidence: 0.23 },
          { name: 'General Conversation', confidence: 0.12 },
        ],
      },
      ragRetrieval: {
        query: input,
        searchTime: '287ms',
        results: [
          { content: 'Troubleshooting login issues: First, clear your browser cache...', relevance: 0.92, source: 'troubleshooting-guide.pdf' },
          { content: 'Common error messages and solutions...', relevance: 0.87, source: 'faq-errors.md' },
          { content: 'Account authentication process...', relevance: 0.81, source: 'user-manual.pdf' },
        ],
      },
      llmGeneration: {
        model: 'gpt-4o',
        temperature: 0.7,
        promptTokens: 2341,
        completionTokens: 156,
        totalTokens: 2497,
        latency: '1.8s',
      },
      actionsExecuted: [],
    };

    setDebugInfo(mockDebug);
    setInput('');

    // Simulate bot response
    setTimeout(() => {
      const botMessage = {
        id: messages.length + 2,
        role: 'bot',
        content: "I understand you're having login issues. Let me help you troubleshoot this. First, try clearing your browser cache and cookies, then attempt to log in again. If the issue persists, try using a different browser or incognito mode. Would you like me to create a support ticket for our technical team?",
        timestamp: new Date().toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, botMessage]);
    }, 1000);
  };

  const handleQuickTest = (query: string) => {
    setInput(query);
  };

  const handleReset = () => {
    setMessages([
      {
        id: 1,
        role: 'bot',
        content: "Hi! I'm DelightChat. How can I help you today?",
        timestamp: '10:30 AM',
      },
    ]);
    setDebugInfo(null);
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">LLM Testing Lab</h1>
        <p className="text-gray-600 text-sm md:text-base">Test intent detection, RAG retrieval, and LLM response generation</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Chat Interface */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-gray-200 flex flex-col h-[600px]">
            {/* Chat Header */}
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-full flex items-center justify-center">
                  <Bot className="w-6 h-6 text-white" />
                </div>
                <div>
                  <div className="text-gray-900">DelightChat Testing Environment</div>
                  <div className="text-gray-500 text-sm flex items-center gap-2">
                    <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                    LLM Active
                  </div>
                </div>
              </div>
              <button
                onClick={handleReset}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                title="Reset conversation"
              >
                <RotateCcw className="w-5 h-5 text-gray-600" />
              </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div className={`flex gap-3 max-w-[80%] ${message.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                        message.role === 'bot'
                          ? 'bg-gradient-to-br from-blue-600 to-purple-600'
                          : 'bg-gray-700'
                      }`}
                    >
                      {message.role === 'bot' ? (
                        <Bot className="w-5 h-5 text-white" />
                      ) : (
                        <User className="w-5 h-5 text-white" />
                      )}
                    </div>
                    <div>
                      <div
                        className={`px-4 py-3 rounded-lg ${
                          message.role === 'bot'
                            ? 'bg-gray-50 border border-gray-200'
                            : 'bg-blue-600 text-white'
                        }`}
                      >
                        <p className={message.role === 'bot' ? 'text-gray-900' : 'text-white'}>
                          {message.content}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-gray-500 text-xs">{message.timestamp}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Input */}
            <div className="p-4 border-t border-gray-200">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                  placeholder="Type your test message..."
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                />
                <button
                  onClick={handleSend}
                  className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                >
                  <Send className="w-5 h-5" />
                  Send
                </button>
              </div>
            </div>
          </div>

          {/* Debug Panel */}
          {debugInfo && (
            <div className="bg-white rounded-xl border border-gray-200">
              <div 
                className="p-4 border-b border-gray-200 flex items-center justify-between cursor-pointer"
                onClick={() => setShowDebug(!showDebug)}
              >
                <h3 className="text-gray-900 flex items-center gap-2">
                  <Brain className="w-5 h-5 text-purple-600" />
                  LLM Debug Information
                </h3>
                {showDebug ? <ChevronUp className="w-5 h-5 text-gray-400" /> : <ChevronDown className="w-5 h-5 text-gray-400" />}
              </div>

              {showDebug && (
                <div className="p-6 space-y-6">
                  {/* Intent Detection */}
                  <div>
                    <div className="flex items-center gap-2 mb-3">
                      <Target className="w-4 h-4 text-blue-600" />
                      <h4 className="text-gray-900">Intent Detection</h4>
                    </div>
                    <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-gray-900">Detected Intent:</span>
                        <span className="font-medium text-blue-700">{debugInfo.intentDetection.detectedIntent}</span>
                      </div>
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-gray-900">Confidence:</span>
                        <div className="flex items-center gap-2">
                          <div className="w-32 h-2 bg-gray-200 rounded-full">
                            <div 
                              className="h-2 bg-green-500 rounded-full"
                              style={{ width: `${debugInfo.intentDetection.confidence * 100}%` }}
                            />
                          </div>
                          <span className="text-sm font-medium text-green-700">
                            {(debugInfo.intentDetection.confidence * 100).toFixed(1)}%
                          </span>
                        </div>
                      </div>
                      <div className="pt-3 border-t border-blue-300">
                        <div className="text-sm text-gray-700 mb-2">Alternative Intents:</div>
                        <div className="space-y-1">
                          {debugInfo.intentDetection.alternativeIntents.map((alt: any, idx: number) => (
                            <div key={idx} className="flex items-center justify-between text-sm">
                              <span className="text-gray-600">{alt.name}</span>
                              <span className="text-gray-500">{(alt.confidence * 100).toFixed(1)}%</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* RAG Retrieval */}
                  <div>
                    <div className="flex items-center gap-2 mb-3">
                      <Database className="w-4 h-4 text-purple-600" />
                      <h4 className="text-gray-900">RAG Vector Search</h4>
                    </div>
                    <div className="space-y-3">
                      <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg text-sm">
                        <div className="flex justify-between mb-1">
                          <span className="text-gray-700">Search Time:</span>
                          <span className="font-medium text-purple-700">{debugInfo.ragRetrieval.searchTime}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Results Found:</span>
                          <span className="font-medium text-purple-700">{debugInfo.ragRetrieval.results.length} chunks</span>
                        </div>
                      </div>
                      {debugInfo.ragRetrieval.results.map((result: any, idx: number) => (
                        <div key={idx} className="p-3 bg-gray-50 border border-gray-200 rounded-lg">
                          <div className="flex items-start justify-between mb-2">
                            <span className="text-xs text-gray-500">Result {idx + 1}</span>
                            <div className="flex items-center gap-1">
                              <span className="text-xs text-gray-600">Relevance:</span>
                              <span className="text-xs font-medium text-green-700">{(result.relevance * 100).toFixed(0)}%</span>
                            </div>
                          </div>
                          <p className="text-sm text-gray-700 mb-2">{result.content}</p>
                          <div className="text-xs text-gray-500">Source: {result.source}</div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* LLM Generation */}
                  <div>
                    <div className="flex items-center gap-2 mb-3">
                      <Sparkles className="w-4 h-4 text-orange-600" />
                      <h4 className="text-gray-900">LLM Generation</h4>
                    </div>
                    <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg">
                      <div className="grid grid-cols-2 gap-3 text-sm">
                        <div className="flex justify-between">
                          <span className="text-gray-700">Model:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.model}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Temperature:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.temperature}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Prompt Tokens:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.promptTokens.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Completion:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.completionTokens}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Total Tokens:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.totalTokens.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-700">Latency:</span>
                          <span className="font-medium text-orange-700">{debugInfo.llmGeneration.latency}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  {debugInfo.actionsExecuted.length > 0 && (
                    <div>
                      <div className="flex items-center gap-2 mb-3">
                        <Sparkles className="w-4 h-4 text-green-600" />
                        <h4 className="text-gray-900">Actions Executed</h4>
                      </div>
                      <div className="space-y-2">
                        {debugInfo.actionsExecuted.map((action: any, idx: number) => (
                          <div key={idx} className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm">
                            <div className="text-gray-900">{action.name}</div>
                            <div className="text-gray-600 text-xs">{action.result}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Testing Tools */}
        <div className="lg:col-span-1 space-y-6">
          {/* Quick Tests */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="text-gray-900 mb-4">Quick Test Queries</h2>
            <div className="space-y-2">
              {quickTests.map((test, index) => (
                <button
                  key={index}
                  onClick={() => handleQuickTest(test.query)}
                  className="w-full text-left p-3 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <div className="text-sm text-gray-900 mb-1">{test.query}</div>
                  <div className="text-xs text-blue-600 flex items-center gap-1">
                    <Target className="w-3 h-3" />
                    {test.intent}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Session Stats */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="text-gray-900 mb-4">Session Stats</h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600 text-sm">Messages</span>
                <span className="text-gray-900">{messages.length}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 text-sm">User Messages</span>
                <span className="text-gray-900">
                  {messages.filter((m) => m.role === 'user').length}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 text-sm">Bot Responses</span>
                <span className="text-gray-900">
                  {messages.filter((m) => m.role === 'bot').length}
                </span>
              </div>
              {debugInfo && (
                <>
                  <div className="pt-3 border-t border-gray-200">
                    <div className="flex justify-between mb-2">
                      <span className="text-gray-600 text-sm">Avg Intent Confidence</span>
                      <span className="text-green-700 font-medium">
                        {(debugInfo.intentDetection.confidence * 100).toFixed(0)}%
                      </span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span className="text-gray-600 text-sm">Vector Search Time</span>
                      <span className="text-purple-700 font-medium">{debugInfo.ragRetrieval.searchTime}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600 text-sm">LLM Latency</span>
                      <span className="text-orange-700 font-medium">{debugInfo.llmGeneration.latency}</span>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Test Scenarios */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="text-gray-900 mb-4">Test Scenarios</h2>
            <div className="space-y-2">
              <button className="w-full text-left px-4 py-3 border border-gray-300 hover:border-blue-400 rounded-lg transition-colors text-sm">
                <div className="text-gray-900 mb-1">Intent Detection</div>
                <div className="text-gray-600 text-xs">Test LLM intent classification</div>
              </button>
              <button className="w-full text-left px-4 py-3 border border-gray-300 hover:border-blue-400 rounded-lg transition-colors text-sm">
                <div className="text-gray-900 mb-1">RAG Retrieval</div>
                <div className="text-gray-600 text-xs">Test vector search accuracy</div>
              </button>
              <button className="w-full text-left px-4 py-3 border border-gray-300 hover:border-blue-400 rounded-lg transition-colors text-sm">
                <div className="text-gray-900 mb-1">Action Execution</div>
                <div className="text-gray-600 text-xs">Test MCP/API calls</div>
              </button>
              <button className="w-full text-left px-4 py-3 border border-gray-300 hover:border-blue-400 rounded-lg transition-colors text-sm">
                <div className="text-gray-900 mb-1">End-to-End Flow</div>
                <div className="text-gray-600 text-xs">Complete conversation test</div>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}