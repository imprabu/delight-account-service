import { useState } from 'react';
import { Search, Filter, MessageSquare, Mail, Phone, Video, User, Clock, CheckCircle, AlertCircle, X, Send, Paperclip, MoreVertical, Tag, UserCheck, Archive, ExternalLink, Plus, List } from 'lucide-react';
import { ViewsPanel } from './ViewsPanel';
import { Pagination } from './Pagination';
import { CreateConversationViewModal } from './CreateConversationViewModal';
import { FilterGroup } from './AdvancedFilterPanel';
import { DemoBanner } from './DemoBanner';

interface InboxProps {
  onNavigateToContact?: (email: string) => void;
}

interface View {
  id: string;
  name: string;
  isDefault?: boolean;
  isPredefined?: boolean;
  filters: FilterGroup;
  icon?: any;
}

export function ConversationsEnhanced({ onNavigateToContact }: InboxProps) {
  // Mock logged-in user
  const currentUser = {
    id: 1,
    name: 'Alex Thompson',
    email: 'alex@company.com',
    avatar: '👨‍💼',
  };

  // Views Management
  const [views, setViews] = useState<View[]>([
    {
      id: 'needs-help',
      name: 'Needs Help',
      isPredefined: true,
      isDefault: true,
      icon: AlertCircle,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'status-needs-help',
          field: 'status',
          operator: 'equals',
          value: 'needs_help',
        }],
      },
    },
    {
      id: 'assigned',
      name: 'Assigned to Me',
      isPredefined: true,
      icon: UserCheck,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'assigned-me',
          field: 'assignedTo',
          operator: 'equals',
          value: currentUser.id.toString(),
        }],
      },
    },
    {
      id: 'unread',
      name: 'Unread',
      isPredefined: true,
      icon: MessageSquare,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'unread-true',
          field: 'unread',
          operator: 'equals',
          value: 'true',
        }],
      },
    },
    {
      id: 'all',
      name: 'All Conversations',
      isPredefined: true,
      icon: List,
      filters: { logic: 'AND', conditions: [] },
    },
    {
      id: 'resolved',
      name: 'Resolved',
      isPredefined: true,
      icon: CheckCircle,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'status-resolved',
          field: 'status',
          operator: 'equals',
          value: 'resolved',
        }],
      },
    },
  ]);

  const [activeViewId, setActiveViewId] = useState('needs-help');
  const activeView = views.find(v => v.id === activeViewId) || views[0];

  const [selectedConversation, setSelectedConversation] = useState<number | null>(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [replyText, setReplyText] = useState('');
  const [showContactLink, setShowContactLink] = useState(false);
  
  // Filter state - using advanced filters for extensibility
  const [showFilterPanel, setShowFilterPanel] = useState(false);
  const [currentFilters, setCurrentFilters] = useState<FilterGroup>({
    logic: 'AND',
    conditions: []
  });
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  
  // Create/Edit View Modal
  const [showCreateViewModal, setShowCreateViewModal] = useState(false);
  const [editingView, setEditingView] = useState<View | null>(null);

  const conversations = [
    {
      id: 1,
      customerName: 'John Davis',
      customerEmail: 'john.davis@email.com',
      avatar: '👨',
      channel: 'webchat',
      status: 'needs_help',
      subject: 'Cannot reset my password',
      lastMessage: "I've tried the reset link but it's not working. Can you help?",
      lastMessageTime: '2 min ago',
      unread: true,
      assignedTo: null,
      tags: ['account', 'urgent'],
      messageCount: 5,
      messages: [
        { id: 1, sender: 'customer', text: 'Hi, I need help resetting my password', time: '10:23 AM', avatar: '👨' },
        { id: 2, sender: 'bot', text: 'I can help you with that! I\'ll send a reset link to your email.', time: '10:23 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'I got the email but the link doesn\'t work', time: '10:25 AM', avatar: '👨' },
        { id: 4, sender: 'bot', text: 'I\'m sorry to hear that. Let me connect you with a team member who can help.', time: '10:25 AM', avatar: '🤖', confidence: 'low' },
        { id: 5, sender: 'customer', text: "I've tried the reset link but it's not working. Can you help?", time: '10:27 AM', avatar: '👨' },
      ]
    },
    {
      id: 2,
      customerName: 'Emma Wilson',
      customerEmail: 'emma.w@email.com',
      avatar: '👩',
      channel: 'email',
      status: 'needs_help',
      subject: 'Billing question about my subscription',
      lastMessage: 'Why was I charged twice this month?',
      lastMessageTime: '15 min ago',
      unread: true,
      assignedTo: currentUser.id,
      tags: ['billing'],
      messageCount: 3,
      messages: [
        { id: 1, sender: 'customer', text: 'I have a question about my bill', time: '9:45 AM', avatar: '👩' },
        { id: 2, sender: 'bot', text: 'I can help you with billing questions. What would you like to know?', time: '9:45 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Why was I charged twice this month?', time: '9:47 AM', avatar: '👩' },
      ]
    },
    {
      id: 3,
      customerName: 'Michael Brown',
      customerEmail: 'mbrown@email.com',
      avatar: '👨‍💼',
      channel: 'sms',
      status: 'needs_help',
      subject: 'Order hasn\'t arrived',
      lastMessage: 'My order #12345 still hasn\'t arrived. It\'s been 2 weeks.',
      lastMessageTime: '1 hour ago',
      unread: false,
      assignedTo: null,
      tags: ['shipping', 'urgent'],
      messageCount: 4,
      messages: [
        { id: 1, sender: 'customer', text: 'Where is my order?', time: '8:30 AM', avatar: '👨‍💼' },
        { id: 2, sender: 'bot', text: 'I can help you track your order. What\'s your order number?', time: '8:30 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Order #12345', time: '8:32 AM', avatar: '👨‍💼' },
        { id: 4, sender: 'customer', text: 'My order #12345 still hasn\'t arrived. It\'s been 2 weeks.', time: '8:33 AM', avatar: '👨‍💼' },
      ]
    },
    {
      id: 4,
      customerName: 'Lisa Anderson',
      customerEmail: 'lisa.a@email.com',
      avatar: '👩‍🦰',
      channel: 'whatsapp',
      status: 'resolved',
      subject: 'Product features question',
      lastMessage: 'Thanks so much! That helps a lot.',
      lastMessageTime: '2 hours ago',
      unread: false,
      assignedTo: currentUser.id,
      tags: ['product'],
      messageCount: 6,
      messages: [
        { id: 1, sender: 'customer', text: 'Can I export my data?', time: 'Yesterday 3:15 PM', avatar: '👩‍🦰' },
        { id: 2, sender: 'bot', text: 'Yes! You can export your data from Settings → Data Export.', time: 'Yesterday 3:15 PM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Perfect, found it. Thanks!', time: 'Yesterday 3:17 PM', avatar: '👩‍🦰' },
      ]
    },
    {
      id: 5,
      customerName: 'David Martinez',
      customerEmail: 'david.m@email.com',
      avatar: '👨‍🔧',
      channel: 'webchat',
      status: 'resolved',
      subject: 'Payment method update',
      lastMessage: 'All set, thanks!',
      lastMessageTime: '3 hours ago',
      unread: false,
      assignedTo: 2,
      tags: ['billing'],
      messageCount: 4,
      messages: []
    },
    {
      id: 6,
      customerName: 'Sarah Johnson',
      customerEmail: 'sarah.j@email.com',
      avatar: '👩‍💻',
      channel: 'email',
      status: 'needs_help',
      subject: 'API integration help',
      lastMessage: 'How do I get my API key?',
      lastMessageTime: '5 min ago',
      unread: true,
      assignedTo: null,
      tags: ['technical', 'api'],
      messageCount: 2,
      messages: []
    },
    {
      id: 7,
      customerName: 'Robert Chen',
      customerEmail: 'robert.c@email.com',
      avatar: '👨‍🎓',
      channel: 'sms',
      status: 'resolved',
      subject: 'Account upgrade',
      lastMessage: 'Great, upgraded successfully!',
      lastMessageTime: '1 day ago',
      unread: false,
      assignedTo: currentUser.id,
      tags: ['billing', 'upgrade'],
      messageCount: 8,
      messages: []
    },
    {
      id: 8,
      customerName: 'Jennifer Lee',
      customerEmail: 'jennifer.l@email.com',
      avatar: '👩‍🏫',
      channel: 'whatsapp',
      status: 'needs_help',
      subject: 'Feature request',
      lastMessage: 'Can you add dark mode?',
      lastMessageTime: '30 min ago',
      unread: false,
      assignedTo: null,
      tags: ['product', 'feature-request'],
      messageCount: 3,
      messages: []
    },
  ];

  const channels = {
    webchat: { icon: MessageSquare, label: 'Web Chat', color: 'blue' },
    email: { icon: Mail, label: 'Email', color: 'red' },
    sms: { icon: Phone, label: 'SMS', color: 'green' },
    whatsapp: { icon: MessageSquare, label: 'WhatsApp', color: 'green' },
    messenger: { icon: MessageSquare, label: 'Messenger', color: 'blue' },
  };

  const allTags = Array.from(new Set(conversations.flatMap(c => c.tags)));

  // Apply filters
  const applyViewFilters = (convs: any[]) => {
    return convs.filter(conv => {
      for (const condition of activeView.filters.conditions) {
        const { field, operator, value } = condition;
        
        if (field === 'status' && operator === 'equals') {
          if (conv.status !== value) return false;
        }
        if (field === 'assignedTo' && operator === 'equals') {
          if (conv.assignedTo?.toString() !== value) return false;
        }
        if (field === 'unread' && operator === 'equals') {
          if (conv.unread.toString() !== value) return false;
        }
      }
      return true;
    });
  };

  const applyBasicFilters = (convs: any[]) => {
    return convs.filter(conv => {
      // Channel filter
      if (currentFilters.conditions.some(c => c.field === 'channel' && c.value !== 'all' && conv.channel !== c.value)) return false;
      
      // Status filter
      if (currentFilters.conditions.some(c => c.field === 'status' && c.value !== 'all' && conv.status !== c.value)) return false;
      
      // Tag filter
      if (currentFilters.conditions.some(c => c.field === 'tags' && c.value !== 'all' && !conv.tags.includes(c.value))) return false;
      
      // Search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        return (
          conv.customerName.toLowerCase().includes(query) ||
          conv.subject.toLowerCase().includes(query) ||
          conv.lastMessage.toLowerCase().includes(query) ||
          conv.customerEmail.toLowerCase().includes(query)
        );
      }
      
      return true;
    });
  };

  const filteredConversations = applyBasicFilters(applyViewFilters(conversations));

  // Pagination
  const totalConversations = filteredConversations.length;
  const totalPages = Math.ceil(totalConversations / itemsPerPage);
  const paginatedConversations = filteredConversations.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const selectedConv = conversations.find(c => c.id === selectedConversation);

  const handleCreateView = () => {
    setEditingView(null);
    setShowCreateViewModal(true);
  };

  const handleEditView = (viewId: string) => {
    const view = views.find(v => v.id === viewId);
    if (view) {
      setEditingView(view);
      setShowCreateViewModal(true);
    }
  };

  const handleDeleteView = (viewId: string) => {
    setViews(views.filter(v => v.id !== viewId));
    if (activeViewId === viewId) {
      setActiveViewId(views[0].id);
    }
  };

  const handleDuplicateView = (viewId: string) => {
    const view = views.find(v => v.id === viewId);
    if (view) {
      const newView: View = {
        ...view,
        id: `view-${Date.now()}`,
        name: `${view.name} (Copy)`,
        isPredefined: false,
      };
      setViews([...views, newView]);
    }
  };

  const handleSaveView = (viewData: { name: string; description: string; filters: FilterGroup }) => {
    if (editingView) {
      // Update existing view
      setViews(views.map(v =>
        v.id === editingView.id
          ? { ...v, name: viewData.name, filters: viewData.filters }
          : v
      ));
    } else {
      // Create new view with configured filters
      const newView: View = {
        id: `view-${Date.now()}`,
        name: viewData.name,
        isPredefined: false,
        filters: viewData.filters,
      };
      setViews([...views, newView]);
      setActiveViewId(newView.id);
    }
    setShowCreateViewModal(false);
    setEditingView(null);
  };

  const handleSendReply = () => {
    if (!replyText.trim()) return;
    console.log('Sending reply:', replyText);
    setReplyText('');
  };

  const handleAssignToMe = () => {
    console.log('Assigning to me:', selectedConversation);
  };

  const handleResolve = () => {
    console.log('Resolving conversation:', selectedConversation);
  };

  const activeFilterCount = currentFilters.conditions.filter(c => c.value !== 'all').length;

  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="px-6 pt-2">
          <DemoBanner />
        </div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-gray-900 mb-1">Conversations</h1>
            <p className="text-sm text-gray-600">Manage customer conversations across all channels</p>
          </div>
        </div>

        {/* Controls Bar */}
        <div className="flex items-center gap-3">
          {/* Views Panel */}
          <ViewsPanel
            views={views}
            activeView={activeViewId}
            onViewChange={(viewId) => {
              setActiveViewId(viewId);
              setCurrentPage(1);
            }}
            onCreateView={handleCreateView}
            onEditView={handleEditView}
            onDeleteView={handleDeleteView}
            onDuplicateView={handleDuplicateView}
          />

          {/* Search */}
          <div className="flex-1 relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setCurrentPage(1);
              }}
              placeholder="Search conversations..."
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
            />
          </div>

          {/* Filter Toggle */}
          <button
            onClick={() => setShowFilterPanel(!showFilterPanel)}
            className={`px-4 py-2 border rounded-lg transition-colors flex items-center gap-2 ${
              activeFilterCount > 0 || showFilterPanel
                ? 'border-blue-500 bg-blue-50 text-blue-700'
                : 'border-gray-300 hover:bg-gray-50'
            }`}
          >
            <Filter className="w-4 h-4" />
            Filters
            {activeFilterCount > 0 && (
              <span className="px-2 py-0.5 bg-blue-600 text-white rounded-full text-xs">
                {activeFilterCount}
              </span>
            )}
          </button>
        </div>

        {/* Filter Panel */}
        {showFilterPanel && (
          <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <div className="grid grid-cols-3 gap-4">
              {/* Channel Filter */}
              <div>
                <label className="block text-xs text-gray-600 mb-2">Channel</label>
                <select
                  value={currentFilters.conditions.find(c => c.field === 'channel')?.value || 'all'}
                  onChange={(e) => {
                    const newConditions = currentFilters.conditions.filter(c => c.field !== 'channel');
                    if (e.target.value !== 'all') {
                      newConditions.push({ id: 'channel', field: 'channel', operator: 'equals', value: e.target.value });
                    }
                    setCurrentFilters({ ...currentFilters, conditions: newConditions });
                    setCurrentPage(1);
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                >
                  <option value="all">All Channels</option>
                  <option value="webchat">Web Chat</option>
                  <option value="email">Email</option>
                  <option value="sms">SMS</option>
                  <option value="whatsapp">WhatsApp</option>
                </select>
              </div>

              {/* Status Filter */}
              <div>
                <label className="block text-xs text-gray-600 mb-2">Status</label>
                <select
                  value={currentFilters.conditions.find(c => c.field === 'status')?.value || 'all'}
                  onChange={(e) => {
                    const newConditions = currentFilters.conditions.filter(c => c.field !== 'status');
                    if (e.target.value !== 'all') {
                      newConditions.push({ id: 'status', field: 'status', operator: 'equals', value: e.target.value });
                    }
                    setCurrentFilters({ ...currentFilters, conditions: newConditions });
                    setCurrentPage(1);
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                >
                  <option value="all">All Status</option>
                  <option value="needs_help">Needs Help</option>
                  <option value="resolved">Resolved</option>
                </select>
              </div>

              {/* Tag Filter */}
              <div>
                <label className="block text-xs text-gray-600 mb-2">Tags</label>
                <select
                  value={currentFilters.conditions.find(c => c.field === 'tags')?.value || 'all'}
                  onChange={(e) => {
                    const newConditions = currentFilters.conditions.filter(c => c.field !== 'tags');
                    if (e.target.value !== 'all') {
                      newConditions.push({ id: 'tag', field: 'tags', operator: 'contains', value: e.target.value });
                    }
                    setCurrentFilters({ ...currentFilters, conditions: newConditions });
                    setCurrentPage(1);
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                >
                  <option value="all">All Tags</option>
                  {allTags.map(tag => (
                    <option key={tag} value={tag}>{tag}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Reset Filters */}
            {activeFilterCount > 0 && (
              <div className="mt-3 flex justify-end">
                <button
                  onClick={() => {
                    setCurrentFilters({ logic: 'AND', conditions: [] });
                    setCurrentPage(1);
                  }}
                  className="text-sm text-blue-600 hover:underline"
                >
                  Reset Filters
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Main Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Conversations List */}
        <div className="w-96 bg-white border-r border-gray-200 flex flex-col">
          {/* List Header */}
          <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <div className="text-sm text-gray-600">
              {filteredConversations.length} conversation{filteredConversations.length !== 1 ? 's' : ''}
            </div>
          </div>

          {/* Conversation Items */}
          <div className="flex-1 overflow-y-auto">
            {paginatedConversations.length === 0 ? (
              <div className="p-8 text-center">
                <MessageSquare className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                <p className="text-gray-600">No conversations found</p>
                <p className="text-sm text-gray-500 mt-1">Try adjusting your filters</p>
              </div>
            ) : (
              paginatedConversations.map((conv) => {
                const ChannelIcon = channels[conv.channel as keyof typeof channels]?.icon || MessageSquare;
                const channelColor = channels[conv.channel as keyof typeof channels]?.color || 'gray';
                
                return (
                  <div
                    key={conv.id}
                    onClick={() => setSelectedConversation(conv.id)}
                    className={`p-4 border-b border-gray-200 cursor-pointer transition-colors hover:bg-gray-50 ${
                      selectedConversation === conv.id ? 'bg-blue-50 border-l-4 border-l-blue-600' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="text-2xl flex-shrink-0">{conv.avatar}</div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1">
                          <div className="text-sm text-gray-900 truncate">{conv.customerName}</div>
                          <div className="text-xs text-gray-500 flex-shrink-0 ml-2">{conv.lastMessageTime}</div>
                        </div>
                        <div className="text-sm text-gray-900 mb-1 truncate">{conv.subject}</div>
                        <div className="text-xs text-gray-600 mb-2 truncate">{conv.lastMessage}</div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className={`px-2 py-0.5 rounded text-xs flex items-center gap-1 bg-${channelColor}-100 text-${channelColor}-700`}>
                            <ChannelIcon className="w-3 h-3" />
                            {channels[conv.channel as keyof typeof channels]?.label}
                          </span>
                          {conv.unread && (
                            <span className="px-2 py-0.5 rounded text-xs bg-blue-100 text-blue-700">
                              Unread
                            </span>
                          )}
                          {conv.status === 'needs_help' && (
                            <span className="px-2 py-0.5 rounded text-xs bg-orange-100 text-orange-700">
                              Needs Help
                            </span>
                          )}
                          {conv.tags.slice(0, 2).map(tag => (
                            <span key={tag} className="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700">
                              {tag}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="border-t border-gray-200">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={totalConversations}
                itemsPerPage={itemsPerPage}
                onPageChange={setCurrentPage}
                onItemsPerPageChange={(value) => {
                  setItemsPerPage(value);
                  setCurrentPage(1);
                }}
              />
            </div>
          )}
        </div>

        {/* Conversation Detail */}
        {selectedConv ? (
          <div className="flex-1 flex flex-col bg-white">
            {/* Conversation Header */}
            <div className="px-6 py-4 border-b border-gray-200">
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div className="text-3xl">{selectedConv.avatar}</div>
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <h2 className="text-gray-900">{selectedConv.customerName}</h2>
                      <button
                        onClick={() => onNavigateToContact?.(selectedConv.customerEmail)}
                        className="p-1 hover:bg-gray-100 rounded transition-colors"
                        title="View contact details"
                      >
                        <ExternalLink className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                    <div className="text-sm text-gray-600">{selectedConv.subject}</div>
                    <div className="flex items-center gap-2 mt-2">
                      {selectedConv.tags.map(tag => (
                        <span key={tag} className="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {!selectedConv.assignedTo && (
                    <button
                      onClick={handleAssignToMe}
                      className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2"
                    >
                      <UserCheck className="w-4 h-4" />
                      Assign to Me
                    </button>
                  )}
                  {selectedConv.status !== 'resolved' && (
                    <button
                      onClick={handleResolve}
                      className="px-3 py-1.5 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm flex items-center gap-2"
                    >
                      <CheckCircle className="w-4 h-4" />
                      Resolve
                    </button>
                  )}
                  <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                    <MoreVertical className="w-4 h-4 text-gray-600" />
                  </button>
                </div>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4">
              {selectedConv.messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex gap-3 ${msg.sender === 'customer' ? '' : 'flex-row-reverse'}`}
                >
                  <div className="text-2xl flex-shrink-0">{msg.avatar}</div>
                  <div className={`flex-1 max-w-lg ${msg.sender === 'customer' ? '' : 'flex flex-col items-end'}`}>
                    <div
                      className={`px-4 py-3 rounded-lg ${
                        msg.sender === 'customer'
                          ? 'bg-gray-100'
                          : msg.sender === 'bot'
                          ? 'bg-blue-50 border border-blue-200'
                          : 'bg-blue-600 text-white'
                      }`}
                    >
                      <p className="text-sm">{msg.text}</p>
                    </div>
                    <div className="text-xs text-gray-500 mt-1">{msg.time}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Reply Box */}
            <div className="border-t border-gray-200 p-4">
              <div className="flex gap-3">
                <div className="flex-1">
                  <textarea
                    value={replyText}
                    onChange={(e) => setReplyText(e.target.value)}
                    placeholder="Type your reply..."
                    rows={3}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  />
                </div>
              </div>
              <div className="flex items-center justify-between mt-3">
                <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                  <Paperclip className="w-5 h-5 text-gray-600" />
                </button>
                <button
                  onClick={handleSendReply}
                  disabled={!replyText.trim()}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Send className="w-4 h-4" />
                  Send Reply
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <MessageSquare className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-gray-600">Select a conversation to view details</p>
            </div>
          </div>
        )}
      </div>

      {/* Create/Edit View Modal */}
      <CreateConversationViewModal
        isOpen={showCreateViewModal}
        onClose={() => {
          setShowCreateViewModal(false);
          setEditingView(null);
        }}
        onSave={handleSaveView}
        editingView={editingView ? { name: editingView.name, description: '', filters: editingView.filters } : null}
        availableChannels={Object.keys(channels)}
        availableTags={allTags}
      />
    </div>
  );
}