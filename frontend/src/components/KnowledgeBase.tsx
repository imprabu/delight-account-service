import { useState, useEffect, useRef } from 'react';
import { Upload, FileText, Trash2, Plus, Database, Search, Check, Clock, X, AlertCircle, MoreVertical, Copy, ChevronDown, ChevronUp, Tag } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';
import { knowledgeAPI, Document, Stats, BotConfig, DocumentFilters, QueryResponse, SourceDocument } from '../utils/knowledgeApi';

interface KnowledgeBaseProps {
  onNavigate: (screen: string) => void;
}

// Helper function to format file size
function formatFileSize(bytes?: number): string {
  if (!bytes) return 'N/A';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Helper function to format date
function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

// Helper function to get status display
function getStatusDisplay(status: string): { text: string; color: string } {
  switch (status) {
    case 'COMPLETED':
      return { text: 'Ready', color: 'bg-green-50 text-green-700' };
    case 'PROCESSING':
      return { text: 'Processing', color: 'bg-yellow-50 text-yellow-700' };
    case 'PENDING':
      return { text: 'Pending', color: 'bg-gray-50 text-gray-700' };
    case 'FAILED':
      return { text: 'Failed', color: 'bg-red-50 text-red-700' };
    default:
      return { text: status, color: 'bg-gray-50 text-gray-700' };
  }
}

export function KnowledgeBase({ onNavigate }: KnowledgeBaseProps) {
  const [activeTab, setActiveTab] = useState<'documents' | 'settings'>('documents');
  
  // Documents state
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  
  // Stats
  const [stats, setStats] = useState<Stats | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  
  // Bot config
  const [botConfig, setBotConfig] = useState<BotConfig | null>(null);
  const [botConfigLoading, setBotConfigLoading] = useState(false);
  const [savingConfig, setSavingConfig] = useState(false);
  
  // Upload
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadSplitMethod, setUploadSplitMethod] = useState<string>('auto');
  
  // Test query
  const [testQuery, setTestQuery] = useState('');
  const [testQueryLoading, setTestQueryLoading] = useState(false);
  const [testQueryResult, setTestQueryResult] = useState<QueryResponse | null>(null);
  const [testQueryTags, setTestQueryTags] = useState<string[]>([]);
  const [testQueryTagMode, setTestQueryTagMode] = useState<'AND' | 'OR'>('OR');
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [showSourceDocuments, setShowSourceDocuments] = useState(true);
  const [tagInputValue, setTagInputValue] = useState('');
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  
  // Query history
  const [queryHistory, setQueryHistory] = useState<QueryResponse[]>([]);
  const [queryHistoryLoading, setQueryHistoryLoading] = useState(false);
  const [showQueryHistory, setShowQueryHistory] = useState(false);
  
  // Delete confirmation
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [permanentlyDeletingId, setPermanentlyDeletingId] = useState<number | null>(null);
  const [deleteMenuOpen, setDeleteMenuOpen] = useState<number | null>(null);
  const deleteMenuRef = useRef<HTMLDivElement>(null);
  
  // Tag management
  const [editingTagsForDoc, setEditingTagsForDoc] = useState<number | null>(null);
  const [docTagInput, setDocTagInput] = useState('');
  const [showDocTagSuggestions, setShowDocTagSuggestions] = useState(false);
  const [savingTags, setSavingTags] = useState<number | null>(null);

  // Close delete menu when clicking outside
  useEffect(() => {
    if (deleteMenuOpen === null) return;

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      
      // Don't close if clicking inside the menu container or on any button
      if (deleteMenuRef.current && deleteMenuRef.current.contains(target)) {
        console.log('✅ Click inside menu container, keeping it open');
        return;
      }
      
      // Also check if clicking on a button (in case the ref doesn't catch it)
      if (target.closest('button')) {
        console.log('✅ Click on button, keeping menu open');
        return;
      }
      
      // Close if clicking outside the menu
      console.log('❌ Click outside menu, closing it. Target:', target, 'Menu ref:', deleteMenuRef.current);
      setDeleteMenuOpen(null);
    };

    // Use a delay to allow button clicks to process first
    const timeoutId = setTimeout(() => {
      document.addEventListener('click', handleClickOutside, true);
    }, 0);
    
    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('click', handleClickOutside, true);
    };
  }, [deleteMenuOpen]);

  // Load documents
  const loadDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const filters: DocumentFilters = {
        page,
        size,
      };
      if (searchQuery.trim()) filters.search = searchQuery.trim();
      if (statusFilter) filters.status = statusFilter as any;
      if (typeFilter) filters.type = typeFilter as any;
      
      const response = await knowledgeAPI.getDocuments(filters);
      setDocuments(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents');
      console.error('Error loading documents:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load stats
  const loadStats = async () => {
    setStatsLoading(true);
    try {
      const statsData = await knowledgeAPI.getStats();
      setStats(statsData);
    } catch (err) {
      console.error('Error loading stats:', err);
    } finally {
      setStatsLoading(false);
    }
  };

  // Load bot config
  const loadBotConfig = async () => {
    setBotConfigLoading(true);
    try {
      const config = await knowledgeAPI.getBotConfig();
      setBotConfig(config);
    } catch (err) {
      console.error('Error loading bot config:', err);
    } finally {
      setBotConfigLoading(false);
    }
  };

  // Load data on mount and when filters change
  useEffect(() => {
    loadDocuments();
  }, [page, size, searchQuery, statusFilter, typeFilter]);

  useEffect(() => {
    loadStats();
  }, []);

  useEffect(() => {
    if (activeTab === 'settings') {
      loadBotConfig();
      // Query history will load on demand when user clicks to open the section
    }
  }, [activeTab]);

  useEffect(() => {
    loadAvailableTags();
  }, []);

  // Handle search with debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      if (page === 0) {
        loadDocuments();
      } else {
        setPage(0);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [searchQuery]);
  
  // Close tag editor when clicking outside
  useEffect(() => {
    if (editingTagsForDoc === null) return;
    
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      // Don't close if clicking inside the tag editor
      if (target.closest('.tag-editor-container')) {
        return;
      }
      // Close if clicking outside
      closeTagEditor();
    };
    
    // Use a small delay to allow button clicks to process first
    const timeoutId = setTimeout(() => {
      document.addEventListener('click', handleClickOutside);
    }, 100);
    
    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('click', handleClickOutside);
    };
  }, [editingTagsForDoc]);

  // Handle file selection
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError(null);
      // Auto-fill title if empty, using filename without extension
      if (!uploadTitle.trim()) {
        const fileName = file.name;
        const nameWithoutExt = fileName.replace(/\.[^/.]+$/, '');
        setUploadTitle(nameWithoutExt);
      }
    }
  };

  // Handle file upload
  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a file first');
      return;
    }

    if (!uploadTitle.trim()) {
      setError('Please enter a document title');
      return;
    }

    setUploading(true);
    setError(null);
    try {
      await knowledgeAPI.uploadDocument(selectedFile, uploadTitle, uploadSplitMethod);
      setUploadTitle('');
      setSelectedFile(null);
      setUploadSplitMethod('auto'); // Reset to default
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      await loadDocuments();
      await loadStats();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload document');
      console.error('Error uploading document:', err);
    } finally {
      setUploading(false);
    }
  };

  // Handle delete (soft delete)
  const handleDelete = async (id: number) => {
    console.log('🗑️ handleDelete called for document:', id);
    if (!confirm('Are you sure you want to delete this document? This will remove it from search results but can be restored later.')) {
      console.log('❌ Delete cancelled by user');
      return;
    }

    console.log('✅ Delete confirmed, proceeding...');
    setDeletingId(id);
    setDeleteMenuOpen(null);
    try {
      console.log('📞 Calling knowledgeAPI.deleteDocument for id:', id);
      await knowledgeAPI.deleteDocument(id);
      console.log('✅ Delete API call successful');
      await loadDocuments();
      await loadStats();
    } catch (err) {
      console.error('❌ Error deleting document:', err);
      setError(err instanceof Error ? err.message : 'Failed to delete document');
    } finally {
      setDeletingId(null);
    }
  };

  // Handle permanent delete
  const handlePermanentDelete = async (id: number) => {
    console.log('🗑️ handlePermanentDelete called for document:', id);
    if (!confirm('⚠️ WARNING: This will permanently delete this document and cannot be undone!\n\nThis will:\n- Remove all chunks from the vector database\n- Delete the physical file\n- Remove all associated tags\n- Permanently remove from the database\n\nAre you absolutely sure?')) {
      console.log('❌ Permanent delete cancelled by user');
      return;
    }

    console.log('✅ Permanent delete confirmed, proceeding...');
    setPermanentlyDeletingId(id);
    setDeleteMenuOpen(null);
    try {
      console.log('📞 Calling knowledgeAPI.permanentlyDeleteDocument for id:', id);
      await knowledgeAPI.permanentlyDeleteDocument(id);
      console.log('✅ Permanent delete API call successful');
      await loadDocuments();
      await loadStats();
    } catch (err) {
      console.error('❌ Error permanently deleting document:', err);
      setError(err instanceof Error ? err.message : 'Failed to permanently delete document');
    } finally {
      setPermanentlyDeletingId(null);
    }
  };

  // Handle bot config update
  const handleConfigUpdate = async (updates: Partial<BotConfig>) => {
    if (!botConfig) return;

    setSavingConfig(true);
    try {
      const updated = await knowledgeAPI.updateBotConfig(updates);
      setBotConfig(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update settings');
      console.error('Error updating bot config:', err);
    } finally {
      setSavingConfig(false);
    }
  };

  // Load available tags
  const loadAvailableTags = async () => {
    try {
      const tags = await knowledgeAPI.getAllTags();
      setAvailableTags(tags);
    } catch (err) {
      console.error('Error loading tags:', err);
      setAvailableTags([]);
    }
  };
  
  // Handle adding tag to document
  const handleAddTagToDocument = async (docId: number, tagName: string) => {
    if (!tagName.trim()) return;
    
    const normalizedTag = tagName.trim().toLowerCase();
    const doc = documents.find(d => d.id === docId);
    if (!doc) return;
    
    // Check if tag already exists
    if (doc.tags && doc.tags.includes(normalizedTag)) {
      setDocTagInput('');
      return;
    }
    
    setSavingTags(docId);
    try {
      // Add the tag via API
      await knowledgeAPI.addTags(docId, [normalizedTag]);
      
      // Update local state
      setDocuments(docs => 
        docs.map(d => 
          d.id === docId 
            ? { ...d, tags: [...(d.tags || []), normalizedTag] }
            : d
        )
      );
      
      // Refresh available tags if it's a new tag
      if (!availableTags.includes(normalizedTag)) {
        await loadAvailableTags();
      }
      
      setDocTagInput('');
      setShowDocTagSuggestions(false);
    } catch (err) {
      console.error('Error adding tag:', err);
      alert('Failed to add tag. Please try again.');
    } finally {
      setSavingTags(null);
    }
  };
  
  // Handle removing tag from document
  const handleRemoveTagFromDocument = async (docId: number, tagName: string) => {
    setSavingTags(docId);
    try {
      await knowledgeAPI.removeTag(docId, tagName);
      
      // Update local state
      setDocuments(docs => 
        docs.map(d => 
          d.id === docId 
            ? { ...d, tags: (d.tags || []).filter(t => t !== tagName) }
            : d
        )
      );
    } catch (err) {
      console.error('Error removing tag:', err);
      alert('Failed to remove tag. Please try again.');
    } finally {
      setSavingTags(null);
    }
  };
  
  // Open tag editor for document
  const openTagEditor = (docId: number) => {
    setEditingTagsForDoc(docId);
    setDocTagInput('');
    setShowDocTagSuggestions(false);
  };
  
  // Close tag editor
  const closeTagEditor = () => {
    setEditingTagsForDoc(null);
    setDocTagInput('');
    setShowDocTagSuggestions(false);
  };

  // Load query history
  const loadQueryHistory = async () => {
    setQueryHistoryLoading(true);
    try {
      const response = await knowledgeAPI.getQueryHistory(0, 20);
      // Backend returns queries ordered by createdAt DESC (most recent first)
      // Display them in the same order
      setQueryHistory(response.content);
    } catch (err) {
      console.error('Error loading query history:', err);
      setQueryHistory([]);
    } finally {
      setQueryHistoryLoading(false);
    }
  };

  // Handle test query
  const handleTestQuery = async () => {
    if (!testQuery.trim()) return;

    setTestQueryLoading(true);
    setTestQueryResult(null);
    try {
      const response = await knowledgeAPI.testQuery(
        testQuery.trim(), 
        false,
        testQueryTags.length > 0 ? testQueryTags : undefined,
        testQueryTags.length > 0 ? testQueryTagMode : undefined
      );
      setTestQueryResult(response);
      // Reload query history after successful query
      await loadQueryHistory();
    } catch (err) {
      const errorResponse: QueryResponse = {
        queryId: 0,
        status: 'FAILED',
        errorMessage: err instanceof Error ? err.message : 'Failed to test query',
      };
      setTestQueryResult(errorResponse);
      console.error('Error testing query:', err);
    } finally {
      setTestQueryLoading(false);
    }
  };

  // Clear test query
  const clearTestQuery = () => {
    setTestQuery('');
    setTestQueryResult(null);
    setTestQueryTags([]);
  };

  // Copy response to clipboard
  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      // Could show a toast notification here
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
    }
  };

  // Format processing time
  const formatProcessingTime = (ms?: number): string => {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  // Format similarity score
  const formatSimilarity = (similarity: number): string => {
    return `${(similarity * 100).toFixed(1)}%`;
  };

  // Get similarity color
  const getSimilarityColor = (similarity: number): string => {
    if (similarity >= 0.8) return 'text-green-600 bg-green-50';
    if (similarity >= 0.6) return 'text-yellow-600 bg-yellow-50';
    return 'text-red-600 bg-red-50';
  };

  // Get status badge color
  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-50 text-green-700';
      case 'PROCESSING':
        return 'bg-yellow-50 text-yellow-700';
      case 'FAILED':
      case 'RATE_LIMIT_EXCEEDED':
        return 'bg-red-50 text-red-700';
      default:
        return 'bg-gray-50 text-gray-700';
    }
  };


  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-gray-900 mb-2">Knowledge Base</h1>
          <p className="text-gray-600 text-sm md:text-base">Add documents that your AI assistant uses to answer questions</p>
        </div>
        <button 
          onClick={() => fileInputRef.current?.click()}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full md:w-auto"
        >
          <Plus className="w-5 h-5" />
          Add Documents
        </button>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-xl p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-600" />
            <span className="text-red-800">{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-red-600 hover:text-red-800">
            <X className="w-5 h-5" />
          </button>
        </div>
      )}

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
                <div className="text-gray-900 text-xl mb-1">
                  {statsLoading ? '...' : (stats?.totalDocuments || 0).toLocaleString()}
                </div>
                <div className="text-gray-600 text-xs">Total Documents</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">
                  {statsLoading ? '...' : (stats?.articlesCount || 0).toLocaleString()}
                </div>
                <div className="text-gray-600 text-xs">Articles</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">
                  {statsLoading ? '...' : (stats?.answerFoundPercentage || 0).toFixed(1)}%
                </div>
                <div className="text-gray-600 text-xs">Answer Found</div>
              </div>
              <div className="bg-white rounded-lg p-3 border border-purple-200">
                <div className="text-gray-900 text-xl mb-1">
                  {statsLoading ? '...' : ((stats?.averageSearchTimeMs || 0) / 1000).toFixed(1)}s
                </div>
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
            <div className="max-w-md mx-auto space-y-3">
              <input
                ref={fileInputRef}
                type="file"
                onChange={handleFileSelect}
                className="hidden"
                accept=".pdf,.txt,.md,.doc,.docx"
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                className="w-full px-6 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors disabled:opacity-50 disabled:cursor-not-allowed border border-gray-300"
              >
                {selectedFile ? `Selected: ${selectedFile.name}` : 'Choose File'}
              </button>
              {selectedFile && (
                <div className="text-sm text-gray-600">
                  File: {selectedFile.name} ({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)
                </div>
              )}
              <input
                type="text"
                placeholder="Document title..."
                value={uploadTitle}
                onChange={(e) => setUploadTitle(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
              <div>
                <label className="block text-sm text-gray-700 mb-1">Chunk Split Method</label>
                <select
                  value={uploadSplitMethod}
                  onChange={(e) => setUploadSplitMethod(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm"
                  title="How to split this document into chunks for better search"
                >
                  <option value="auto">Auto (Recommended)</option>
                  <option value="paragraph">Paragraph</option>
                  <option value="section">Section</option>
                  <option value="page">Page</option>
                </select>
                <p className="text-xs text-gray-500 mt-1">
                  {botConfig?.splitMethod && uploadSplitMethod === 'auto' 
                    ? `Will use account default: ${botConfig.splitMethod}` 
                    : 'Determines how this document is broken into searchable pieces'}
                </p>
              </div>
              <button
                onClick={handleUpload}
                disabled={uploading || !selectedFile || !uploadTitle.trim()}
                className="w-full px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {uploading ? 'Uploading...' : 'Upload Document'}
              </button>
            </div>
            <p className="text-gray-500 text-sm mt-4">Supports PDF, Word, Text, CSV, and more (Max 10MB per file)</p>
          </div>

          {/* Documents List */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-4 md:p-6 border-b border-gray-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <h2 className="text-gray-900">Your Documents ({totalElements})</h2>
              <div className="flex items-center gap-2 flex-wrap">
                <select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Status</option>
                  <option value="COMPLETED">Ready</option>
                  <option value="PROCESSING">Processing</option>
                  <option value="PENDING">Pending</option>
                  <option value="FAILED">Failed</option>
                </select>
                <select
                  value={typeFilter}
                  onChange={(e) => {
                    setTypeFilter(e.target.value);
                    setPage(0);
                  }}
                  className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Types</option>
                  <option value="PDF">PDF</option>
                  <option value="TEXT">Text</option>
                </select>
                <div className="flex items-center gap-2">
                  <Search className="w-4 h-4 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search documents..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-full sm:w-auto"
                  />
                </div>
              </div>
            </div>
            
            {loading ? (
              <div className="p-12 text-center text-gray-500">
                <Clock className="w-8 h-8 animate-spin mx-auto mb-2" />
                <p>Loading documents...</p>
              </div>
            ) : documents.length === 0 ? (
              <div className="p-12 text-center text-gray-500">
                <FileText className="w-12 h-12 mx-auto mb-2 text-gray-400" />
                <p>No documents found</p>
              </div>
            ) : (
              <>
                <div className="divide-y divide-gray-200">
                  {documents.map((doc) => {
                    const statusDisplay = getStatusDisplay(doc.embeddingStatus);
                    return (
                      <div key={doc.id} className="p-6 hover:bg-gray-50 transition-colors">
                        <div className="flex items-start gap-4">
                          <div className="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                            <FileText className="w-6 h-6 text-blue-600" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-start justify-between mb-2">
                              <div>
                                <div className="text-gray-900 mb-1">{doc.title}</div>
                                <div className="text-gray-500 text-sm flex items-center gap-3 flex-wrap">
                                  <span>{doc.type}</span>
                                  <span>•</span>
                                  <span>{formatFileSize(doc.fileSize)}</span>
                                  <span>•</span>
                                  <span>Added {formatDate(doc.createdAt)}</span>
                                  {doc.splitMethod && (
                                    <>
                                      <span>•</span>
                                      <span className="font-medium text-gray-700" title="Document-specific split method">
                                        Split: {doc.splitMethod.charAt(0).toUpperCase() + doc.splitMethod.slice(1)}
                                      </span>
                                    </>
                                  )}
                                  {!doc.splitMethod && botConfig && (
                                    <>
                                      <span>•</span>
                                      <span className="italic text-gray-400" title="Using account-level default">
                                        Split: Account Default ({botConfig.splitMethod || 'auto'})
                                      </span>
                                    </>
                                  )}
                                </div>
                              </div>
                              <div className="flex items-center gap-3">
                                <span className={`px-3 py-1 rounded-full text-sm flex items-center gap-1.5 ${statusDisplay.color}`}>
                                  {doc.embeddingStatus === 'COMPLETED' ? (
                                    <Check className="w-3 h-3" />
                                  ) : doc.embeddingStatus === 'PROCESSING' ? (
                                    <Clock className="w-3 h-3 animate-spin" />
                                  ) : null}
                                  {statusDisplay.text}
                                </span>
                                <div className="relative" ref={deleteMenuRef}>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.preventDefault();
                                      e.stopPropagation();
                                      const newState = deleteMenuOpen === doc.id ? null : doc.id;
                                      console.log('🔘 Menu toggle clicked for document:', doc.id, 'Current state:', deleteMenuOpen, 'New state:', newState);
                                      setDeleteMenuOpen(newState);
                                    }}
                                    disabled={deletingId === doc.id || permanentlyDeletingId === doc.id}
                                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
                                  >
                                    <MoreVertical className="w-5 h-5 text-gray-600" />
                                  </button>
                                  {deleteMenuOpen === doc.id && (
                                    <div 
                                      className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-50"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        console.log('📦 Menu container clicked, preventing close');
                                      }}
                                    >
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.preventDefault();
                                          e.stopPropagation();
                                          console.log('🔘 Soft delete button clicked for document:', doc.id);
                                          // Close menu first
                                          setDeleteMenuOpen(null);
                                          // Then handle delete
                                          handleDelete(doc.id);
                                        }}
                                        disabled={deletingId === doc.id || permanentlyDeletingId === doc.id}
                                        className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-t-lg disabled:opacity-50 disabled:cursor-not-allowed"
                                      >
                                        <div className="flex items-center gap-2">
                                          <Trash2 className="w-4 h-4" />
                                          <span>Delete (Soft)</span>
                                        </div>
                                        <div className="text-xs text-gray-500 mt-0.5">Can be restored</div>
                                      </button>
                                      <div className="border-t border-gray-200"></div>
                                      <button
                                        type="button"
                                        onClick={(e) => {
                                          e.preventDefault();
                                          e.stopPropagation();
                                          console.log('🔘 Permanent delete button clicked for document:', doc.id);
                                          // Close menu first
                                          setDeleteMenuOpen(null);
                                          // Then handle delete
                                          handlePermanentDelete(doc.id);
                                        }}
                                        disabled={deletingId === doc.id || permanentlyDeletingId === doc.id}
                                        className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-b-lg disabled:opacity-50 disabled:cursor-not-allowed"
                                      >
                                        <div className="flex items-center gap-2">
                                          <AlertCircle className="w-4 h-4" />
                                          <span>Permanently Delete</span>
                                        </div>
                                        <div className="text-xs text-red-500 mt-0.5">Cannot be undone</div>
                                      </button>
                                    </div>
                                  )}
                                </div>
                              </div>
                            </div>
                            
                            {doc.embeddingStatus === 'COMPLETED' && (
                              <>
                                {doc.tokenCount && (
                                  <div className="flex items-center gap-4 mb-3 text-sm">
                                    <div className="text-gray-600">
                                      ~{Math.round(doc.tokenCount / 400)} articles extracted
                                    </div>
                                  </div>
                                )}
                                
                                {/* Tags Section */}
                                <div className="mt-3">
                                  <div className="flex items-center gap-2 mb-2">
                                    <Tag className="w-4 h-4 text-gray-500" />
                                    <span className="text-sm font-medium text-gray-700">Tags:</span>
                                    {editingTagsForDoc !== doc.id && (
                                      <button
                                        type="button"
                                        onClick={() => openTagEditor(doc.id)}
                                        className="text-xs text-blue-600 hover:text-blue-700 ml-1"
                                        disabled={savingTags === doc.id}
                                      >
                                        {doc.tags && doc.tags.length > 0 ? 'Edit' : 'Add tags'}
                                      </button>
                                    )}
                                  </div>
                                  
                                  {editingTagsForDoc === doc.id ? (
                                    <div className="space-y-2 tag-editor-container">
                                      {/* Current Tags */}
                                      {doc.tags && doc.tags.length > 0 && (
                                        <div className="flex flex-wrap gap-2">
                                          {doc.tags.map((tag, idx) => (
                                            <span 
                                              key={idx} 
                                              className="px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs flex items-center gap-1"
                                            >
                                              {tag}
                                              <button
                                                type="button"
                                                onClick={() => handleRemoveTagFromDocument(doc.id, tag)}
                                                disabled={savingTags === doc.id}
                                                className="hover:text-blue-900 disabled:opacity-50"
                                              >
                                                <X className="w-3 h-3" />
                                              </button>
                                            </span>
                                          ))}
                                        </div>
                                      )}
                                      
                                      {/* Tag Input */}
                                      <div className="relative">
                                        <input
                                          type="text"
                                          placeholder="Type to add tag..."
                                          value={docTagInput}
                                          onChange={(e) => {
                                            setDocTagInput(e.target.value);
                                            setShowDocTagSuggestions(e.target.value.length > 0);
                                          }}
                                          onFocus={() => setShowDocTagSuggestions(docTagInput.length > 0)}
                                          onKeyDown={(e) => {
                                            if (e.key === 'Enter' && docTagInput.trim()) {
                                              e.preventDefault();
                                              handleAddTagToDocument(doc.id, docTagInput);
                                            } else if (e.key === 'Escape') {
                                              closeTagEditor();
                                            }
                                          }}
                                          disabled={savingTags === doc.id}
                                          className="w-full px-3 py-1.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none disabled:opacity-50"
                                        />
                                        {showDocTagSuggestions && (
                                          <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                                            {availableTags
                                              .filter(tag => 
                                                tag.toLowerCase().includes(docTagInput.toLowerCase()) &&
                                                !doc.tags?.includes(tag)
                                              )
                                              .slice(0, 5)
                                              .map((tag, idx) => (
                                                <button
                                                  key={idx}
                                                  type="button"
                                                  onClick={() => handleAddTagToDocument(doc.id, tag)}
                                                  className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm"
                                                >
                                                  {tag}
                                                </button>
                                              ))}
                                            {docTagInput.trim() && 
                                             !availableTags.some(tag => tag.toLowerCase() === docTagInput.trim().toLowerCase()) &&
                                             !doc.tags?.includes(docTagInput.trim().toLowerCase()) && (
                                              <button
                                                type="button"
                                                onClick={() => handleAddTagToDocument(doc.id, docTagInput)}
                                                className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm text-blue-600 font-medium"
                                              >
                                                + Create "{docTagInput.trim()}"
                                              </button>
                                            )}
                                          </div>
                                        )}
                                      </div>
                                      
                                      {/* Action Buttons */}
                                      <div className="flex items-center gap-2">
                                        <button
                                          type="button"
                                          onClick={closeTagEditor}
                                          disabled={savingTags === doc.id}
                                          className="text-sm text-gray-600 hover:text-gray-800 disabled:opacity-50"
                                        >
                                          Done
                                        </button>
                                        {savingTags === doc.id && (
                                          <span className="text-xs text-gray-500">Saving...</span>
                                        )}
                                      </div>
                                    </div>
                                  ) : (
                                    /* Display Tags (Read-only) */
                                    doc.tags && doc.tags.length > 0 ? (
                                      <div className="flex flex-wrap gap-2">
                                        {doc.tags.map((tag, idx) => (
                                          <span 
                                            key={idx} 
                                            className="px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs"
                                          >
                                            {tag}
                                          </span>
                                        ))}
                                      </div>
                                    ) : (
                                      <span className="text-xs text-gray-400 italic">No tags</span>
                                    )
                                  )}
                                </div>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
                
                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="p-4 md:p-6 border-t border-gray-200 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div className="text-sm text-gray-600">
                      Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of {totalElements}
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setPage(page - 1)}
                        disabled={page === 0 || loading}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Previous
                      </button>
                      <span className="text-sm text-gray-600">
                        Page {page + 1} of {totalPages}
                      </span>
                      <button
                        onClick={() => setPage(page + 1)}
                        disabled={page >= totalPages - 1 || loading}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Next
                      </button>
                      <select
                        value={size}
                        onChange={(e) => {
                          setSize(Number(e.target.value));
                          setPage(0);
                        }}
                        className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value={10}>10 per page</option>
                        <option value={20}>20 per page</option>
                        <option value={50}>50 per page</option>
                        <option value={100}>100 per page</option>
                      </select>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {activeTab === 'settings' && (
        <div className="space-y-6">
          {botConfigLoading ? (
            <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500">
              <Clock className="w-8 h-8 animate-spin mx-auto mb-2" />
              <p>Loading settings...</p>
            </div>
          ) : botConfig ? (
            <>
              {/* Search Settings */}
              <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
                <h2 className="text-gray-900 mb-4">Search Settings</h2>
                <p className="text-sm text-gray-600 mb-4">
                  Configure how your AI assistant searches through your knowledge base
                </p>
                <div className="space-y-4">
                  <div>
                    <label className="block text-gray-700 mb-2">Number of Results to Check</label>
                    <select
                      value={botConfig.maxResults || 5}
                      onChange={(e) => handleConfigUpdate({ maxResults: Number(e.target.value) })}
                      disabled={savingConfig}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none disabled:opacity-50"
                    >
                      <option value="3">3 results (Faster)</option>
                      <option value="5">5 results (Balanced)</option>
                      <option value="10">10 results (More thorough)</option>
                    </select>
                    <p className="text-xs text-gray-500 mt-1">More results provide better context but take slightly longer</p>
                  </div>
                  
                  <div>
                    <label className="block text-gray-700 mb-2">Answer Quality Threshold: {botConfig.answerQualityThreshold || 70}</label>
                    <div className="flex items-center gap-4">
                      <span className="text-gray-600 text-sm">Relaxed</span>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={botConfig.answerQualityThreshold || 70}
                        onChange={(e) => handleConfigUpdate({ answerQualityThreshold: Number(e.target.value) })}
                        disabled={savingConfig}
                        className="flex-1 disabled:opacity-50"
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
                      <input
                        type="checkbox"
                        className="sr-only peer"
                        checked={botConfig.showSourceDocuments || false}
                        onChange={(e) => handleConfigUpdate({ showSourceDocuments: e.target.checked })}
                        disabled={savingConfig}
                      />
                      <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform disabled:opacity-50"></div>
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
                    <select
                      value={botConfig.splitMethod || 'auto'}
                      onChange={(e) => handleConfigUpdate({ splitMethod: e.target.value })}
                      disabled={savingConfig}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none disabled:opacity-50"
                    >
                      <option value="auto">Automatic (Recommended)</option>
                      <option value="paragraph">By Paragraph</option>
                      <option value="section">By Section</option>
                      <option value="page">By Page</option>
                    </select>
                    <p className="text-xs text-gray-500 mt-1">Determines how documents are broken into searchable pieces</p>
                  </div>

                  <div>
                    <label className="block text-gray-700 mb-2">Typical Article Size</label>
                    <select
                      value={botConfig.articleSize || 'medium'}
                      onChange={(e) => handleConfigUpdate({ articleSize: e.target.value })}
                      disabled={savingConfig}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none disabled:opacity-50"
                    >
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
                      <input
                        type="checkbox"
                        className="sr-only peer"
                        checked={botConfig.autoUpdateDocuments || false}
                        onChange={(e) => handleConfigUpdate({ autoUpdateDocuments: e.target.checked })}
                        disabled={savingConfig}
                      />
                      <div className="w-12 h-6 bg-gray-300 rounded-full peer-checked:bg-blue-600 transition-colors peer-checked:after:translate-x-6 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform disabled:opacity-50"></div>
                    </label>
                  </div>
                </div>
              </div>

              {/* Test Search */}
              <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-gray-900 flex items-center gap-2">
                    <Search className="w-5 h-5 text-orange-600" />
                    Test Your Knowledge Base
                  </h2>
                  {testQueryResult && (
                    <button
                      onClick={clearTestQuery}
                      className="text-sm text-gray-600 hover:text-gray-900"
                    >
                      Clear
                    </button>
                  )}
                </div>
                <p className="text-sm text-gray-600 mb-4">
                  Try a sample question to see what information the bot would find
                </p>
                
                {/* Tag Selector */}
                <div className="mb-4">
                  <label className="block text-sm text-gray-700 mb-2">Filter by Tags (Optional)</label>
                  <div className="flex gap-2 mb-2">
                    <select
                      value={testQueryTagMode}
                      onChange={(e) => setTestQueryTagMode(e.target.value as 'AND' | 'OR')}
                      className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      <option value="OR">Any Tag (OR)</option>
                      <option value="AND">All Tags (AND)</option>
                    </select>
                  </div>
                  
                  {/* Tag Input with Autocomplete */}
                  <div className="relative mb-2">
                    <input
                      type="text"
                      placeholder="Type to search tags..."
                      value={tagInputValue}
                      onChange={(e) => {
                        setTagInputValue(e.target.value);
                        setShowTagSuggestions(e.target.value.length > 0);
                      }}
                      onFocus={() => setShowTagSuggestions(tagInputValue.length > 0)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && tagInputValue.trim()) {
                          e.preventDefault();
                          const tag = tagInputValue.trim().toLowerCase();
                          if (!testQueryTags.includes(tag)) {
                            setTestQueryTags([...testQueryTags, tag]);
                          }
                          setTagInputValue('');
                          setShowTagSuggestions(false);
                        }
                      }}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm"
                    />
                    {showTagSuggestions && (
                      <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                        {availableTags.length > 0 && availableTags
                          .filter(tag => 
                            tag.toLowerCase().includes(tagInputValue.toLowerCase()) &&
                            !testQueryTags.includes(tag)
                          )
                          .slice(0, 5)
                          .map((tag, idx) => (
                            <button
                              key={idx}
                              onClick={() => {
                                if (!testQueryTags.includes(tag)) {
                                  setTestQueryTags([...testQueryTags, tag]);
                                }
                                setTagInputValue('');
                                setShowTagSuggestions(false);
                              }}
                              className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm"
                            >
                              {tag}
                            </button>
                          ))}
                        {tagInputValue.trim() && 
                         !availableTags.some(tag => tag.toLowerCase() === tagInputValue.trim().toLowerCase()) &&
                         !testQueryTags.includes(tagInputValue.trim().toLowerCase()) && (
                          <button
                            onClick={() => {
                              const newTag = tagInputValue.trim().toLowerCase();
                              setTestQueryTags([...testQueryTags, newTag]);
                              setTagInputValue('');
                              setShowTagSuggestions(false);
                            }}
                            className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm text-blue-600 font-medium"
                          >
                            + Create "{tagInputValue.trim()}"
                          </button>
                        )}
                        {availableTags.length === 0 && tagInputValue.trim() && !testQueryTags.includes(tagInputValue.trim().toLowerCase()) && (
                          <button
                            onClick={() => {
                              const newTag = tagInputValue.trim().toLowerCase();
                              setTestQueryTags([...testQueryTags, newTag]);
                              setTagInputValue('');
                              setShowTagSuggestions(false);
                            }}
                            className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm text-blue-600 font-medium"
                          >
                            + Create "{tagInputValue.trim()}"
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                  
                  {/* Selected Tags */}
                  {testQueryTags.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {testQueryTags.map((tag, idx) => (
                        <span
                          key={idx}
                          className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm flex items-center gap-2"
                        >
                          {tag}
                          <button
                            onClick={() => setTestQueryTags(testQueryTags.filter((_, i) => i !== idx))}
                            className="hover:text-blue-900"
                            type="button"
                          >
                            <X className="w-3 h-3" />
                          </button>
                        </span>
                      ))}
                    </div>
                  )}
                  
                  {availableTags.length === 0 && (
                    <p className="text-xs text-gray-500 mt-1">No tags available. Tags will appear here once documents are tagged.</p>
                  )}
                  {testQueryTags.length > 0 && (
                    <p className="text-xs text-gray-500 mt-1">Tag filtering will be available when Weaviate schema is updated</p>
                  )}
                </div>

                <div className="flex gap-2 mb-4">
                  <input
                    type="text"
                    placeholder="Type a question..."
                    value={testQuery}
                    onChange={(e) => setTestQuery(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && !testQueryLoading && handleTestQuery()}
                    disabled={testQueryLoading}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none disabled:opacity-50"
                  />
                  <button
                    onClick={handleTestQuery}
                    disabled={testQueryLoading || !testQuery.trim()}
                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                  >
                    {testQueryLoading ? (
                      <>
                        <Clock className="w-4 h-4 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>
                        <Search className="w-4 h-4" />
                        Search
                      </>
                    )}
                  </button>
                </div>

                {testQueryResult && (
                  <div className="mt-4 space-y-4">
                    {/* Status Badge */}
                    <div className="flex items-center justify-between">
                      <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(testQueryResult.status)}`}>
                        {testQueryResult.status}
                      </span>
                      {testQueryResult.response && (
                        <button
                          onClick={() => copyToClipboard(testQueryResult.response || '')}
                          className="text-sm text-gray-600 hover:text-gray-900 flex items-center gap-1"
                        >
                          <Copy className="w-4 h-4" />
                          Copy
                        </button>
                      )}
                    </div>

                    {/* Error Message */}
                    {testQueryResult.errorMessage && (
                      <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                        <div className="flex items-start gap-2">
                          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                          <div>
                            <div className="text-red-900 font-medium mb-1">Error</div>
                            <div className="text-red-700 text-sm">{testQueryResult.errorMessage}</div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Response */}
                    {testQueryResult.response ? (
                      <div className="p-4 bg-gray-50 rounded-lg">
                        <div className="text-sm text-gray-600 mb-2 font-medium">Response:</div>
                        <div className="text-gray-900 whitespace-pre-wrap">{testQueryResult.response}</div>
                      </div>
                    ) : testQueryResult.status === 'COMPLETED' && !testQueryResult.errorMessage ? (
                      <div className="p-4 bg-yellow-50 rounded-lg border border-yellow-200">
                        <div className="text-sm text-yellow-800">
                          No response generated. This may indicate no relevant documents were found.
                        </div>
                      </div>
                    ) : null}

                    {/* Query Metrics */}
                    {(testQueryResult.processingTimeMs || testQueryResult.tokenCount || testQueryResult.costUsd) && (
                      <div className="p-4 bg-blue-50 rounded-lg">
                        <div className="text-sm text-gray-600 mb-2 font-medium">Query Metrics:</div>
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          {testQueryResult.processingTimeMs && (
                            <div>
                              <div className="text-gray-600">Processing Time</div>
                              <div className="text-gray-900 font-medium">{formatProcessingTime(testQueryResult.processingTimeMs)}</div>
                            </div>
                          )}
                          {testQueryResult.tokenCount && (
                            <div>
                              <div className="text-gray-600">Tokens</div>
                              <div className="text-gray-900 font-medium">{testQueryResult.tokenCount.toLocaleString()}</div>
                            </div>
                          )}
                          {testQueryResult.costUsd && (
                            <div>
                              <div className="text-gray-600">Cost</div>
                              <div className="text-gray-900 font-medium">${testQueryResult.costUsd.toFixed(4)}</div>
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Source Documents */}
                    {testQueryResult.sourceDocuments && testQueryResult.sourceDocuments.length > 0 && (
                      <div className="p-4 bg-gray-50 rounded-lg">
                        <button
                          onClick={() => setShowSourceDocuments(!showSourceDocuments)}
                          className="flex items-center justify-between w-full mb-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                        >
                          <span>Source Documents ({testQueryResult.sourceDocuments.length})</span>
                          {showSourceDocuments ? (
                            <ChevronUp className="w-4 h-4" />
                          ) : (
                            <ChevronDown className="w-4 h-4" />
                          )}
                        </button>
                        {showSourceDocuments && (
                          <div className="space-y-2 mt-2">
                            {testQueryResult.sourceDocuments.map((doc, idx) => (
                              <div
                                key={idx}
                                className="p-3 bg-white rounded-lg border border-gray-200 flex items-start justify-between"
                              >
                                <div className="flex-1">
                                  <div className="text-sm font-medium text-gray-900 mb-1">{doc.title}</div>
                                  {doc.chunkId && (
                                    <div className="text-xs text-gray-500">Chunk: {doc.chunkId}</div>
                                  )}
                                </div>
                                <div className={`px-2 py-1 rounded text-xs font-medium ${getSimilarityColor(doc.similarity)}`}>
                                  {formatSimilarity(doc.similarity)}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Query History */}
              <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
                <button
                  onClick={() => {
                    const willShow = !showQueryHistory;
                    setShowQueryHistory(willShow);
                    // Load on demand when opening the section
                    if (willShow && queryHistory.length === 0 && !queryHistoryLoading) {
                      loadQueryHistory();
                    }
                  }}
                  className="flex items-center justify-between w-full mb-4"
                >
                  <h2 className="text-gray-900 flex items-center gap-2">
                    <Clock className="w-5 h-5 text-gray-600" />
                    Query History
                  </h2>
                  {showQueryHistory ? (
                    <ChevronUp className="w-5 h-5 text-gray-600" />
                  ) : (
                    <ChevronDown className="w-5 h-5 text-gray-600" />
                  )}
                </button>
                {showQueryHistory && (
                  <div>
                    {queryHistoryLoading ? (
                      <div className="text-center py-8 text-gray-500">
                        <Clock className="w-8 h-8 animate-spin mx-auto mb-2" />
                        Loading history...
                      </div>
                    ) : queryHistory.length === 0 ? (
                      <div className="text-center py-8 text-gray-500">No query history yet</div>
                    ) : (
                      <div className="space-y-2">
                        {/* Display queries in DESC order (most recent first) - backend already returns them sorted */}
                        {queryHistory.map((query) => (
                          <button
                            key={query.queryId}
                            onClick={async () => {
                              // Re-run query from history
                              const queryText = query.query || '';
                              setTestQuery(queryText);
                              setTestQueryTags([]); // Reset tags for now
                              setTestQueryResult(null); // Clear previous result
                              
                              // Automatically execute the query if we have query text
                              if (queryText.trim()) {
                                setTestQueryLoading(true);
                                try {
                                  const response = await knowledgeAPI.testQuery(queryText.trim());
                                  setTestQueryResult(response);
                                  await loadQueryHistory();
                                } catch (err) {
                                  const errorResponse: QueryResponse = {
                                    queryId: 0,
                                    status: 'FAILED',
                                    errorMessage: err instanceof Error ? err.message : 'Failed to test query',
                                  };
                                  setTestQueryResult(errorResponse);
                                } finally {
                                  setTestQueryLoading(false);
                                }
                              } else {
                                // If no query text, just show the previous result
                                setTestQueryResult(query);
                              }
                            }}
                            className="w-full text-left p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                          >
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-sm font-medium text-gray-900 truncate flex-1" title={query.query || query.response || 'No query text'}>
                                {query.query || query.response?.substring(0, 60) || 'No query text'}
                                {query.query && query.query.length > 60 ? '...' : (query.response && query.response.length > 60 ? '...' : '')}
                              </span>
                              <span className={`px-2 py-1 rounded text-xs ml-2 flex-shrink-0 ${getStatusColor(query.status)}`}>
                                {query.status}
                              </span>
                            </div>
                            <div className="flex items-center justify-between text-xs text-gray-500">
                              <span>
                                {query.processingTimeMs ? formatProcessingTime(query.processingTimeMs) : 'N/A'}
                              </span>
                              {query.createdAt && (
                                <span>
                                  {new Date(query.createdAt).toLocaleString()}
                                </span>
                              )}
                            </div>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500">
              <AlertCircle className="w-8 h-8 mx-auto mb-2 text-red-500" />
              <p>Failed to load settings</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
