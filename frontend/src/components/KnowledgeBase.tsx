import { useState } from 'react';
import { Upload, FileText, Trash2, Plus, Database, Search, Check, Clock } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface KnowledgeBaseProps {
  onNavigate: (screen: string) => void;
}

export function KnowledgeBase({ onNavigate }: KnowledgeBaseProps) {
  const [activeTab, setActiveTab] = useState<'documents' | 'settings'>('documents');

  const documents = [
    { 
      id: 1, 
      name: 'Product Documentation', 
      type: 'PDF', 
      size: '2.4 MB', 
      status: 'Ready', 
      date: '2024-11-25',
      articles: 89,
      tags: ['product-features', 'documentation']
    },
    { 
      id: 2, 
      name: 'FAQ Database', 
      type: 'CSV', 
      size: '856 KB', 
      status: 'Ready', 
      date: '2024-11-24',
      articles: 34,
      tags: ['faq', 'support']
    },
    { 
      id: 3, 
      name: 'Support Articles', 
      type: 'JSON', 
      size: '1.2 MB', 
      status: 'Processing', 
      date: '2024-11-28',
      articles: 0,
      tags: ['troubleshooting', 'support']
    },
    { 
      id: 4, 
      name: 'API Documentation', 
      type: 'Markdown', 
      size: '3.8 MB', 
      status: 'Ready', 
      date: '2024-11-23',
      articles: 142,
      tags: ['api', 'technical-docs']
    },
  ];

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Knowledge Base</h1>
          <p className="text-gray-600 text-sm md:text-base">Add documents that your AI assistant uses to answer questions</p>
        </div>
        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto">
          <Plus className="w-5 h-5" />
          Add Documents
        </button>
      </div>

      {/* Info Banner */}
      <div className="mb-6 bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-xl p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 bg-purple-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <Database className="w-6 h-6 text-white" />
          </div>
          <div className="flex-1">
            <h3 className="text-gray-900 mb-2">How Knowledge Base Works</h3>
            <p className="text-gray-700 text-sm mb-3">
              Upload your help articles, FAQs, manuals, and guides. Your AI assistant will search through these documents 
              to find relevant information and provide accurate answers to your customers.
            </p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 md:gap-4">
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">247</div>
                <div className="text-gray-600 text-xs">Total Documents</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">8,421</div>
                <div className="text-gray-600 text-xs">Articles</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">87%</div>
                <div className="text-gray-600 text-xs">Answer Found</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">0.3s</div>
                <div className="text-gray-600 text-xs">Search Time</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200">
        <div className="flex gap-6">
          <button
            onClick={() => setActiveTab('documents')}
            className={`pb-3 px-1 border-b-2 transition-colors ${
              activeTab === 'documents'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            Documents
          </button>
          <button
            onClick={() => setActiveTab('settings')}
            className={`pb-3 px-1 border-b-2 transition-colors ${
              activeTab === 'settings'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            Settings
          </button>
        </div>
      </div>

      {activeTab === 'documents' && (
        <div className="space-y-6">
          {/* Upload Area */}
          <div className="bg-white rounded-xl border-2 border-dashed border-gray-300 p-6 md:p-12 text-center hover:border-blue-400 transition-colors">
            <div className="w-12 h-12 md:w-16 md:h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <Upload className="w-6 h-6 md:w-8 md:h-8 text-blue-600" />
            </div>
            <h3 className="text-gray-900 mb-2">Upload Documents</h3>
            <p className="text-gray-600 text-sm md:text-base mb-4">Add files that contain information to help answer customer questions</p>
            <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors w-full sm:w-auto">
              Choose Files
            </button>
            <p className="text-gray-500 text-sm mt-4">Supports PDF, Word, Text, CSV, and more (Max 10MB per file)</p>
          </div>

          {/* Documents List */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-4 md:p-6 border-b border-gray-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <h2 className="text-gray-900">Your Documents ({documents.length})</h2>
              <div className="flex items-center gap-2">
                <Search className="w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search documents..."
                  className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-full sm:w-auto"
                />
              </div>
            </div>
            <div className="divide-y divide-gray-200">
              {documents.map((doc) => (
                <div key={doc.id} className="p-6 hover:bg-gray-50 transition-colors">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                      <FileText className="w-6 h-6 text-blue-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <div className="text-gray-900 mb-1">{doc.name}</div>
                          <div className="text-gray-500 text-sm flex items-center gap-3">
                            <span>{doc.type}</span>
                            <span>•</span>
                            <span>{doc.size}</span>
                            <span>•</span>
                            <span>Added {doc.date}</span>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span
                            className={`px-3 py-1 rounded-full text-sm flex items-center gap-1.5 ${
                              doc.status === 'Ready'
                                ? 'bg-green-50 text-green-700'
                                : 'bg-yellow-50 text-yellow-700'
                            }`}
                          >
                            {doc.status === 'Ready' ? (
                              <Check className="w-3 h-3" />
                            ) : (
                              <Clock className="w-3 h-3 animate-spin" />
                            )}
                            {doc.status}
                          </span>
                          <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                            <Trash2 className="w-5 h-5 text-gray-600" />
                          </button>
                        </div>
                      </div>
                      
                      {doc.status === 'Ready' && (
                        <>
                          <div className="flex items-center gap-4 mb-3 text-sm">
                            <div className="text-gray-600">
                              {doc.articles} articles extracted
                            </div>
                          </div>
                          
                          <div className="flex flex-wrap gap-2">
                            {doc.tags.map((tag, idx) => (
                              <span key={idx} className="px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs">
                                {tag}
                              </span>
                            ))}
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'settings' && (
        <div className="space-y-6">
          {/* Search Settings */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-4">Search Settings</h2>
            <p className="text-sm text-gray-600 mb-4">
              Configure how your AI assistant searches through your knowledge base
            </p>
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2">Number of Results to Check</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                  <option value="3">3 results (Faster)</option>
                  <option value="5">5 results (Balanced)</option>
                  <option value="10">10 results (More thorough)</option>
                </select>
                <p className="text-xs text-gray-500 mt-1">More results provide better context but take slightly longer</p>
              </div>
              
              <div>
                <label className="block text-gray-700 mb-2">Answer Quality Threshold</label>
                <div className="flex items-center gap-4">
                  <span className="text-gray-600 text-sm">Relaxed</span>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    defaultValue={70}
                    className="flex-1"
                  />
                  <span className="text-gray-600 text-sm">Strict</span>
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  Higher threshold means bot will only answer when very confident
                </p>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Show Source Documents</div>
                  <div className="text-gray-600 text-sm">Display which documents were used to create the answer</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>
            </div>
          </div>

          {/* Document Processing */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-4">Document Processing</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2">How to Split Documents</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                  <option value="auto">Automatic (Recommended)</option>
                  <option value="paragraph">By Paragraph</option>
                  <option value="section">By Section</option>
                  <option value="page">By Page</option>
                </select>
                <p className="text-xs text-gray-500 mt-1">Determines how documents are broken into searchable pieces</p>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">Typical Article Size</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                  <option value="small">Short (1-2 paragraphs)</option>
                  <option value="medium">Medium (3-5 paragraphs)</option>
                  <option value="large">Long (Full articles)</option>
                </select>
                <p className="text-xs text-gray-500 mt-1">Helps optimize how content is stored and searched</p>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div>
                  <div className="text-gray-900 mb-1">Auto-update Documents</div>
                  <div className="text-gray-600 text-sm">Automatically check for updates in connected sources</div>
                </div>
                <label className="relative inline-block w-12 h-6">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform"></div>
                </label>
              </div>
            </div>
          </div>

          {/* Test Search */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
            <h2 className="text-gray-900 mb-4 flex items-center gap-2">
              <Search className="w-5 h-5 text-orange-600" />
              Test Your Knowledge Base
            </h2>
            <p className="text-sm text-gray-600 mb-4">
              Try a sample question to see what information the bot would find
            </p>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Type a question..."
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
              <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                Search
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
