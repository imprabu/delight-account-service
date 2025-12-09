import { useState } from 'react';
import { Search, Plus, Building2, User, Mail, Phone, Calendar, MapPin, ExternalLink, MessageSquare, Filter, MoreVertical, Edit, Trash2, Star, Tag, Clock, TrendingUp, DollarSign, Users, ChevronRight, X, Download, Upload, Link2, Merge, Send, Video, StickyNote, AlertTriangle, CheckCircle2, ArrowRight, Settings, Eye, EyeOff, ChevronDown, ChevronLeft as ChevronLeftIcon, ChevronRight as ChevronRightIcon } from 'lucide-react';
import { ContactsSettings } from './ContactsSettings';
import { AccountsSettings } from './AccountsSettings';
import { LLMNoteAssistant } from './LLMNoteAssistant';

type ViewMode = 'contacts' | 'accounts';
type ContactView = 'list' | 'detail' | 'allConversations' | 'settings';
type ModalType = 'newContact' | 'editContact' | 'deleteContact' | 'newAccount' | 'editAccount' | 'deleteAccount' | 'addNote' | 'editNote' | 'addTag' | 'linkAccount' | 'mergeContacts' | 'exportData' | 'importData' | 'sendEmail' | 'scheduleCall' | 'bulkAction' | 'editAdditionalInfo' | 'contactSettings' | null;

export function Contacts() {
  const [viewMode, setViewMode] = useState<ViewMode>('contacts');
  const [contactView, setContactView] = useState<ContactView>('list');
  const [selectedContact, setSelectedContact] = useState<any>(null);
  const [selectedAccount, setSelectedAccount] = useState<any>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const [selectedContacts, setSelectedContacts] = useState<number[]>([]);
  const [showFilterPanel, setShowFilterPanel] = useState(false);
  const [filters, setFilters] = useState({
    tags: [] as string[],
    status: 'all',
    company: 'all',
    dateRange: 'all',
  });
  const [editingNote, setEditingNote] = useState<any>(null);
  const [editingContact, setEditingContact] = useState<any>(null);
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // Field visibility and LLM settings
  const [visibleFields, setVisibleFields] = useState({
    email: true,
    phone: true,
    company: true,
    role: true,
    linkedin: true,
    timezone: true,
    preferredContact: true,
    tags: true,
    notes: true,
  });

  const [enableLLMNotes, setEnableLLMNotes] = useState(true);

  const contacts = [
    {
      id: 1,
      name: 'Sarah Johnson',
      email: 'sarah.johnson@techcorp.com',
      phone: '+1 (555) 123-4567',
      company: 'TechCorp Industries',
      accountId: 1,
      role: 'Product Manager',
      avatar: null,
      tags: ['VIP', 'Enterprise'],
      lastContact: '2 hours ago',
      conversationCount: 12,
      status: 'active',
      createdAt: 'Jan 15, 2024',
      notes: 'Key decision maker for enterprise features. Interested in API integrations.',
      customFields: {
        linkedin: 'linkedin.com/in/sarahjohnson',
        timezone: 'PST (UTC-8)',
        preferredContact: 'Email',
      },
    },
    {
      id: 2,
      name: 'Michael Chen',
      email: 'mchen@startup.io',
      phone: '+1 (555) 234-5678',
      company: 'Startup.io',
      accountId: 2,
      role: 'CTO',
      avatar: null,
      tags: ['Trial', 'Technical'],
      lastContact: '1 day ago',
      conversationCount: 5,
      status: 'active',
      createdAt: 'Feb 20, 2024',
      notes: 'Evaluating our platform for team of 50. Needs custom integration.',
      customFields: {
        linkedin: 'linkedin.com/in/michaelchen',
        timezone: 'EST (UTC-5)',
        preferredContact: 'Phone',
      },
    },
    {
      id: 3,
      name: 'Emily Rodriguez',
      email: 'e.rodriguez@globalinc.com',
      phone: '+1 (555) 345-6789',
      company: 'Global Inc',
      accountId: 3,
      role: 'Operations Director',
      avatar: null,
      tags: ['Enterprise', 'Support'],
      lastContact: '3 days ago',
      conversationCount: 28,
      status: 'active',
      createdAt: 'Dec 5, 2023',
      notes: 'Long-term customer. Occasional technical issues with integrations.',
      customFields: {
        linkedin: 'linkedin.com/in/emilyrodriguez',
        timezone: 'CST (UTC-6)',
        preferredContact: 'Email',
      },
    },
    {
      id: 4,
      name: 'David Kim',
      email: 'david@designstudio.com',
      phone: '+1 (555) 456-7890',
      company: 'Design Studio',
      accountId: 4,
      role: 'Founder',
      avatar: null,
      tags: ['Small Business'],
      lastContact: '1 week ago',
      conversationCount: 3,
      status: 'inactive',
      createdAt: 'Mar 1, 2024',
      notes: 'Small agency, price sensitive.',
      customFields: {
        linkedin: 'linkedin.com/in/davidkim',
        timezone: 'PST (UTC-8)',
        preferredContact: 'Chat',
      },
    },
    {
      id: 5,
      name: 'Jennifer Lee',
      email: 'jlee@enterprise.com',
      phone: '+1 (555) 567-8901',
      company: 'Enterprise Solutions',
      accountId: 5,
      role: 'VP of Sales',
      avatar: null,
      tags: ['VIP', 'Enterprise'],
      lastContact: '4 hours ago',
      conversationCount: 45,
      status: 'active',
      createdAt: 'Nov 10, 2023',
      notes: 'High-value customer. Regular check-ins needed.',
      customFields: {
        linkedin: 'linkedin.com/in/jenniferlee',
        timezone: 'EST (UTC-5)',
        preferredContact: 'Video',
      },
    },
    {
      id: 6,
      name: 'Robert Martinez',
      email: 'rmartinez@consulting.com',
      phone: '+1 (555) 678-9012',
      company: 'Martinez Consulting',
      accountId: 6,
      role: 'Consultant',
      avatar: null,
      tags: ['Trial'],
      lastContact: '2 weeks ago',
      conversationCount: 2,
      status: 'inactive',
      createdAt: 'Mar 25, 2024',
      notes: 'Trial expired. Follow up needed.',
      customFields: {
        linkedin: 'linkedin.com/in/robertmartinez',
        timezone: 'MST (UTC-7)',
        preferredContact: 'Email',
      },
    },
  ];

  const accounts = [
    {
      id: 1,
      name: 'TechCorp Industries',
      domain: 'techcorp.com',
      industry: 'Technology',
      size: '500-1000 employees',
      location: 'San Francisco, CA',
      status: 'active',
      plan: 'Enterprise',
      mrr: 2500,
      contacts: 8,
      conversations: 45,
      createdAt: 'Jan 15, 2024',
      tags: ['VIP', 'Enterprise'],
      notes: 'Major enterprise client. Annual contract up for renewal in Q4.',
      customFields: {
        website: 'https://techcorp.com',
        accountOwner: 'John Smith',
        contractStartDate: 'Jan 1, 2024',
        contractEndDate: 'Dec 31, 2024',
        annualValue: 30000,
      },
    },
    {
      id: 2,
      name: 'Startup.io',
      domain: 'startup.io',
      industry: 'SaaS',
      size: '50-100 employees',
      location: 'New York, NY',
      status: 'trial',
      plan: 'Professional (Trial)',
      mrr: 0,
      contacts: 3,
      conversations: 8,
      createdAt: 'Feb 20, 2024',
      tags: ['Trial', 'Hot Lead'],
      notes: 'High potential. Trial ends in 2 weeks. CTO very engaged.',
      customFields: {
        website: 'https://startup.io',
        accountOwner: 'Jane Doe',
        trialStartDate: 'Nov 15, 2024',
        trialEndDate: 'Dec 15, 2024',
        annualValue: 0,
      },
    },
    {
      id: 3,
      name: 'Global Inc',
      domain: 'globalinc.com',
      industry: 'Manufacturing',
      size: '1000+ employees',
      location: 'Chicago, IL',
      status: 'active',
      plan: 'Enterprise',
      mrr: 5000,
      contacts: 15,
      conversations: 120,
      createdAt: 'Dec 5, 2023',
      tags: ['Enterprise', 'Long-term'],
      notes: 'Largest customer. Excellent relationship. Expansion opportunity.',
      customFields: {
        website: 'https://globalinc.com',
        accountOwner: 'John Smith',
        contractStartDate: 'Jun 1, 2024',
        contractEndDate: 'Jun 30, 2025',
        annualValue: 60000,
      },
    },
    {
      id: 4,
      name: 'Design Studio',
      domain: 'designstudio.com',
      industry: 'Design',
      size: '10-50 employees',
      location: 'Austin, TX',
      status: 'active',
      plan: 'Starter',
      mrr: 29,
      contacts: 2,
      conversations: 5,
      createdAt: 'Mar 1, 2024',
      tags: ['Small Business'],
      notes: 'Small agency. Basic usage.',
      customFields: {
        website: 'https://designstudio.com',
        accountOwner: 'Jane Doe',
        contractStartDate: 'Mar 1, 2024',
        contractEndDate: 'Mar 1, 2025',
        annualValue: 348,
      },
    },
  ];

  const allConversations = [
    {
      id: 1,
      subject: 'API Integration Question',
      preview: 'Hi, I\'m trying to integrate the webhook...',
      date: '2 hours ago',
      status: 'resolved',
      channel: 'email',
    },
    {
      id: 2,
      subject: 'Feature Request: Custom Fields',
      preview: 'Would love to see support for custom fields...',
      date: '1 day ago',
      status: 'open',
      channel: 'chat',
    },
    {
      id: 3,
      subject: 'Billing Question',
      preview: 'Quick question about our invoice...',
      date: '3 days ago',
      status: 'resolved',
      channel: 'email',
    },
    {
      id: 4,
      subject: 'Setup Help Needed',
      preview: 'Can you help me set up the integration?',
      date: '5 days ago',
      status: 'resolved',
      channel: 'chat',
    },
    {
      id: 5,
      subject: 'Product Demo Request',
      preview: 'I\'d like to schedule a demo...',
      date: '1 week ago',
      status: 'resolved',
      channel: 'email',
    },
  ];

  const availableTags = ['VIP', 'Enterprise', 'Trial', 'Hot Lead', 'Technical', 'Support', 'Small Business', 'Long-term'];

  const handleContactClick = (contact: any) => {
    setSelectedContact(contact);
    setContactView('detail');
  };

  const handleAccountClick = (account: any) => {
    setSelectedAccount(account);
  };

  const handleBackToList = () => {
    setContactView('list');
    setSelectedContact(null);
    setSelectedAccount(null);
  };

  // Apply filters
  const applyFilters = (items: any[]) => {
    return items.filter(item => {
      // Tag filter
      if (filters.tags.length > 0) {
        const hasTag = filters.tags.some(tag => item.tags.includes(tag));
        if (!hasTag) return false;
      }
      
      // Status filter
      if (filters.status !== 'all' && item.status !== filters.status) {
        return false;
      }
      
      // Company filter for contacts
      if (viewMode === 'contacts' && filters.company !== 'all') {
        if (item.company !== filters.company) return false;
      }
      
      return true;
    });
  };

  const filteredContacts = applyFilters(
    contacts.filter(contact =>
      contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.company.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  const filteredAccounts = applyFilters(
    accounts.filter(account =>
      account.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      account.domain.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  // Pagination
  const paginateItems = (items: any[]) => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return items.slice(startIndex, endIndex);
  };

  const totalPages = viewMode === 'contacts' 
    ? Math.ceil(filteredContacts.length / itemsPerPage)
    : Math.ceil(filteredAccounts.length / itemsPerPage);

  const paginatedContacts = paginateItems(filteredContacts);
  const paginatedAccounts = paginateItems(filteredAccounts);

  const resetFilters = () => {
    setFilters({
      tags: [],
      status: 'all',
      company: 'all',
      dateRange: 'all',
    });
  };

  const activeFilterCount = 
    filters.tags.length + 
    (filters.status !== 'all' ? 1 : 0) + 
    (filters.company !== 'all' ? 1 : 0) +
    (filters.dateRange !== 'all' ? 1 : 0);

  return (
    <div className="p-4 md:p-6 lg:p-8">
      {/* Settings View */}
      {contactView === 'settings' && (
        <div>
          <button
            onClick={handleBackToList}
            className="mb-4 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <ChevronRight className="w-4 h-4 rotate-180" />
            Back to {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}
          </button>

          {viewMode === 'contacts' ? (
            <ContactsSettings
              visibleFields={visibleFields}
              setVisibleFields={setVisibleFields}
              onClose={handleBackToList}
            />
          ) : (
            <AccountsSettings
              visibleFields={visibleFields}
              setVisibleFields={setVisibleFields}
              onClose={handleBackToList}
            />
          )}
        </div>
      )}

      {/* All Conversations View */}
      {contactView === 'allConversations' && selectedContact && (
        <div>
          <button
            onClick={() => setContactView('detail')}
            className="mb-4 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <ChevronRight className="w-4 h-4 rotate-180" />
            Back to {selectedContact.name}
          </button>

          <AllConversationsView contact={selectedContact} conversations={allConversations} />
        </div>
      )}

      {/* Main List/Detail Views */}
      {contactView !== 'settings' && contactView !== 'allConversations' && (
        <>
          {/* Header */}
          <div className="mb-6 md:mb-8 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
            <div>
              <h1 className="text-gray-900 mb-2">Contacts & Accounts</h1>
              <p className="text-gray-600 text-sm md:text-base">Manage customer relationships and view conversation history</p>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {selectedContacts.length > 0 && (
                <div className="flex items-center gap-2 w-full sm:w-auto">
                  <span className="text-sm text-gray-600">{selectedContacts.length} selected</span>
                  <button
                    onClick={() => setActiveModal('bulkAction')}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2 text-sm"
                  >
                    Bulk Actions
                  </button>
                </div>
              )}
              <button
                onClick={() => setActiveModal('exportData')}
                className="px-3 md:px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
              >
                <Download className="w-4 h-4" />
                <span className="hidden md:inline">Export</span>
              </button>
              <button
                onClick={() => setActiveModal('importData')}
                className="px-3 md:px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
              >
                <Upload className="w-4 h-4" />
                <span className="hidden md:inline">Import</span>
              </button>
              <button
                onClick={() => setContactView('settings')}
                className="px-3 md:px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
              >
                <Settings className="w-4 h-4" />
                <span className="hidden sm:inline">Settings</span>
              </button>
              <button
                onClick={() => setActiveModal(viewMode === 'contacts' ? 'newContact' : 'newAccount')}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <Plus className="w-5 h-5" />
                <span className="hidden sm:inline">New {viewMode === 'contacts' ? 'Contact' : 'Account'}</span>
                <span className="sm:hidden">New</span>
              </button>
            </div>
          </div>

          {/* View Toggle & Search */}
          <div className="mb-6 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
            <div className="flex bg-gray-100 rounded-lg p-1">
              <button
                onClick={() => {
                  setViewMode('contacts');
                  handleBackToList();
                  setCurrentPage(1);
                }}
                className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                  viewMode === 'contacts'
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <User className="w-4 h-4" />
                Contacts
              </button>
              <button
                onClick={() => {
                  setViewMode('accounts');
                  handleBackToList();
                  setCurrentPage(1);
                }}
                className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                  viewMode === 'accounts'
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <Building2 className="w-4 h-4" />
                Accounts
              </button>
            </div>

            <div className="flex-1 relative">
              <Search className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={`Search ${viewMode}...`}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <button 
              onClick={() => setShowFilterPanel(!showFilterPanel)}
              className={`px-4 py-2 border rounded-lg transition-colors flex items-center justify-center gap-2 ${
                showFilterPanel || activeFilterCount > 0
                  ? 'border-blue-500 bg-blue-50 text-blue-700'
                  : 'border-gray-300 hover:bg-gray-50'
              }`}
            >
              <Filter className="w-4 h-4" />
              <span className="hidden sm:inline">Filter</span>
              {activeFilterCount > 0 && (
                <span className="px-2 py-0.5 bg-blue-600 text-white rounded-full text-xs">
                  {activeFilterCount}
                </span>
              )}
            </button>
          </div>

          {/* Filter Panel */}
          {showFilterPanel && (
            <div className="mb-6 bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Filters</h3>
                <button
                  onClick={resetFilters}
                  className="text-sm text-blue-600 hover:underline"
                >
                  Reset All
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* Tags Filter */}
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Tags</label>
                  <div className="border border-gray-300 rounded-lg p-3 max-h-40 overflow-y-auto">
                    {availableTags.map(tag => (
                      <label key={tag} className="flex items-center gap-2 py-1 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={filters.tags.includes(tag)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setFilters({ ...filters, tags: [...filters.tags, tag] });
                            } else {
                              setFilters({ ...filters, tags: filters.tags.filter(t => t !== tag) });
                            }
                          }}
                          className="w-4 h-4"
                        />
                        <span className="text-sm text-gray-700">{tag}</span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Status Filter */}
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Status</label>
                  <select
                    value={filters.status}
                    onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="all">All Statuses</option>
                    <option value="active">Active</option>
                    <option value="inactive">Inactive</option>
                    {viewMode === 'accounts' && <option value="trial">Trial</option>}
                  </select>
                </div>

                {/* Company Filter (Contacts only) */}
                {viewMode === 'contacts' && (
                  <div>
                    <label className="block text-sm text-gray-700 mb-2">Company</label>
                    <select
                      value={filters.company}
                      onChange={(e) => setFilters({ ...filters, company: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="all">All Companies</option>
                      {Array.from(new Set(contacts.map(c => c.company))).map(company => (
                        <option key={company} value={company}>{company}</option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Date Range Filter */}
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Date Range</label>
                  <select
                    value={filters.dateRange}
                    onChange={(e) => setFilters({ ...filters, dateRange: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="all">All Time</option>
                    <option value="today">Today</option>
                    <option value="week">This Week</option>
                    <option value="month">This Month</option>
                    <option value="quarter">This Quarter</option>
                    <option value="year">This Year</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* Contacts View */}
          {viewMode === 'contacts' && contactView === 'list' && (
            <>
              {/* Mobile Card View */}
              <div className="lg:hidden space-y-3">
                {paginatedContacts.map((contact) => (
                  <div 
                    key={contact.id}
                    className="bg-white rounded-xl border border-gray-200 p-4"
                    onClick={() => handleContactClick(contact)}
                  >
                    <div className="flex items-start gap-3 mb-3">
                      <input
                        type="checkbox"
                        checked={selectedContacts.includes(contact.id)}
                        onChange={(e) => {
                          e.stopPropagation();
                          if (e.target.checked) {
                            setSelectedContacts([...selectedContacts, contact.id]);
                          } else {
                            setSelectedContacts(selectedContacts.filter(id => id !== contact.id));
                          }
                        }}
                        className="w-4 h-4 mt-1"
                      />
                      <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 flex-shrink-0">
                        {contact.name.split(' ').map(n => n[0]).join('')}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-gray-900">{contact.name}</div>
                        <div className="text-sm text-gray-600">{contact.email}</div>
                        <div className="text-sm text-gray-600 mt-1">{contact.company} • {contact.role}</div>
                      </div>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedContact(contact);
                        }}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <MoreVertical className="w-4 h-4 text-gray-600" />
                      </button>
                    </div>
                    <div className="flex gap-2 flex-wrap mb-2">
                      {contact.tags.map((tag, idx) => (
                        <span
                          key={idx}
                          className={`px-2 py-0.5 rounded text-xs ${
                            tag === 'VIP' ? 'bg-purple-100 text-purple-700' :
                            tag === 'Enterprise' ? 'bg-blue-100 text-blue-700' :
                            tag === 'Trial' ? 'bg-yellow-100 text-yellow-700' :
                            tag === 'Technical' ? 'bg-green-100 text-green-700' :
                            'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                    <div className="flex items-center justify-between text-sm text-gray-600 pt-3 border-t border-gray-100">
                      <span className="flex items-center gap-1">
                        <MessageSquare className="w-4 h-4" />
                        {contact.conversationCount}
                      </span>
                      <span className="text-xs">{contact.lastContact}</span>
                      <span className={`px-2 py-1 rounded text-xs ${
                        contact.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                      }`}>
                        {contact.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop Table View */}
              <div className="hidden lg:block bg-white rounded-xl border border-gray-200 overflow-hidden">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">
                        <input
                          type="checkbox"
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedContacts(paginatedContacts.map(c => c.id));
                            } else {
                              setSelectedContacts([]);
                            }
                          }}
                          checked={selectedContacts.length === paginatedContacts.length && paginatedContacts.length > 0}
                          className="w-4 h-4"
                        />
                      </th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Contact</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Company</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Role</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Tags</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Conversations</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Last Contact</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Status</th>
                      <th className="px-6 py-3"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {paginatedContacts.map((contact) => (
                      <tr
                        key={contact.id}
                        className="hover:bg-gray-50 cursor-pointer transition-colors"
                      >
                        <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="checkbox"
                            checked={selectedContacts.includes(contact.id)}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedContacts([...selectedContacts, contact.id]);
                              } else {
                                setSelectedContacts(selectedContacts.filter(id => id !== contact.id));
                              }
                            }}
                            className="w-4 h-4"
                          />
                        </td>
                        <td className="px-6 py-4" onClick={() => handleContactClick(contact)}>
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-blue-700">
                              {contact.name.split(' ').map(n => n[0]).join('')}
                            </div>
                            <div>
                              <div className="text-gray-900">{contact.name}</div>
                              <div className="text-sm text-gray-600">{contact.email}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-900" onClick={() => handleContactClick(contact)}>{contact.company}</td>
                        <td className="px-6 py-4 text-gray-600 text-sm" onClick={() => handleContactClick(contact)}>{contact.role}</td>
                        <td className="px-6 py-4" onClick={() => handleContactClick(contact)}>
                          <div className="flex gap-1 flex-wrap">
                            {contact.tags.map((tag, idx) => (
                              <span
                                key={idx}
                                className={`px-2 py-0.5 rounded text-xs ${
                                  tag === 'VIP' ? 'bg-purple-100 text-purple-700' :
                                  tag === 'Enterprise' ? 'bg-blue-100 text-blue-700' :
                                  tag === 'Trial' ? 'bg-yellow-100 text-yellow-700' :
                                  tag === 'Technical' ? 'bg-green-100 text-green-700' :
                                  'bg-gray-100 text-gray-700'
                                }`}
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-900" onClick={() => handleContactClick(contact)}>{contact.conversationCount}</td>
                        <td className="px-6 py-4 text-sm text-gray-600" onClick={() => handleContactClick(contact)}>{contact.lastContact}</td>
                        <td className="px-6 py-4" onClick={() => handleContactClick(contact)}>
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            contact.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                          }`}>
                            {contact.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                            }}
                            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                          >
                            <MoreVertical className="w-4 h-4 text-gray-600" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
                totalItems={filteredContacts.length}
                itemsPerPage={itemsPerPage}
              />
            </>
          )}

          {/* Contact Detail View */}
          {viewMode === 'contacts' && contactView === 'detail' && selectedContact && (
            <ContactDetailView
              contact={selectedContact}
              accounts={accounts}
              visibleFields={visibleFields}
              onBack={handleBackToList}
              onEdit={(field) => {
                setEditingContact(selectedContact);
                if (field === 'additionalInfo') {
                  setActiveModal('editAdditionalInfo');
                } else if (field === 'notes') {
                  setEditingNote({ text: selectedContact.notes });
                  setActiveModal('editNote');
                } else {
                  setActiveModal('editContact');
                }
              }}
              onViewAllConversations={() => setContactView('allConversations')}
              onNavigateToAccount={(accountId) => {
                const account = accounts.find(a => a.id === accountId);
                if (account) {
                  setViewMode('accounts');
                  handleAccountClick(account);
                }
              }}
            />
          )}

          {/* Accounts View */}
          {viewMode === 'accounts' && (
            <>
              <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Account</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Industry</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Plan</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">MRR</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Contacts</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Status</th>
                      <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">Tags</th>
                      <th className="px-6 py-3"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {paginatedAccounts.map((account) => (
                      <tr
                        key={account.id}
                        onClick={() => handleAccountClick(account)}
                        className="hover:bg-gray-50 cursor-pointer transition-colors"
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                              <Building2 className="w-5 h-5 text-purple-600" />
                            </div>
                            <div>
                              <div className="text-gray-900">{account.name}</div>
                              <div className="text-sm text-gray-600">{account.domain}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-900">{account.industry}</td>
                        <td className="px-6 py-4 text-gray-900">{account.plan}</td>
                        <td className="px-6 py-4 text-gray-900">${account.mrr.toLocaleString()}</td>
                        <td className="px-6 py-4 text-gray-900">{account.contacts}</td>
                        <td className="px-6 py-4">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            account.status === 'active' ? 'bg-green-100 text-green-700' :
                            account.status === 'trial' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-gray-100 text-gray-700'
                          }`}>
                            {account.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-1 flex-wrap">
                            {account.tags.map((tag, idx) => (
                              <span
                                key={idx}
                                className={`px-2 py-0.5 rounded text-xs ${
                                  tag === 'VIP' ? 'bg-purple-100 text-purple-700' :
                                  tag === 'Enterprise' ? 'bg-blue-100 text-blue-700' :
                                  tag === 'Hot Lead' ? 'bg-red-100 text-red-700' :
                                  'bg-gray-100 text-gray-700'
                                }`}
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                            }}
                            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                          >
                            <MoreVertical className="w-4 h-4 text-gray-600" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
                totalItems={filteredAccounts.length}
                itemsPerPage={itemsPerPage}
              />
            </>
          )}

          {/* Account Detail */}
          {viewMode === 'accounts' && selectedAccount && (
            <AccountDetailView
              account={selectedAccount}
              contacts={contacts}
              onClose={() => setSelectedAccount(null)}
              onNavigateToContact={(contact) => {
                setViewMode('contacts');
                handleContactClick(contact);
                setSelectedAccount(null);
              }}
              onEdit={() => {
                setActiveModal('editAccount');
              }}
            />
          )}
        </>
      )}

      {/* All Modals */}
      {activeModal === 'newContact' && (
        <ContactFormModal
          title="New Contact"
          onSave={(data: any) => {
            console.log('Creating contact:', data);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'editContact' && editingContact && (
        <ContactFormModal
          title="Edit Contact"
          contact={editingContact}
          onSave={(data: any) => {
            console.log('Updating contact:', data);
            setActiveModal(null);
            setEditingContact(null);
          }}
          onClose={() => {
            setActiveModal(null);
            setEditingContact(null);
          }}
        />
      )}

      {activeModal === 'editAdditionalInfo' && editingContact && (
        <EditAdditionalInfoModal
          contact={editingContact}
          onSave={(data: any) => {
            console.log('Updating additional info:', data);
            setActiveModal(null);
            setEditingContact(null);
          }}
          onClose={() => {
            setActiveModal(null);
            setEditingContact(null);
          }}
        />
      )}

      {activeModal === 'editNote' && editingNote && (
        <LLMNoteAssistant
          initialNote={editingNote.text}
          enableLLM={enableLLMNotes}
          onSave={(note: any) => {
            console.log('Updating note:', note);
            setActiveModal(null);
            setEditingNote(null);
          }}
          onClose={() => {
            setActiveModal(null);
            setEditingNote(null);
          }}
        />
      )}

      {activeModal === 'deleteContact' && selectedContact && (
        <DeleteConfirmModal
          title="Delete Contact"
          message={`Are you sure you want to delete ${selectedContact.name}? This action cannot be undone.`}
          onConfirm={() => {
            console.log('Deleting contact:', selectedContact.id);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'newAccount' && (
        <AccountFormModal
          title="New Account"
          onSave={(data: any) => {
            console.log('Creating account:', data);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'editAccount' && selectedAccount && (
        <AccountFormModal
          title="Edit Account"
          account={selectedAccount}
          onSave={(data: any) => {
            console.log('Updating account:', data);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'exportData' && (
        <ExportDataModal
          viewMode={viewMode}
          onExport={(format: string, fields: string[]) => {
            console.log('Exporting:', format, fields);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'importData' && (
        <ImportDataModal
          viewMode={viewMode}
          onImport={(file: File, mapping: any) => {
            console.log('Importing:', file, mapping);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'bulkAction' && (
        <BulkActionModal
          selectedCount={selectedContacts.length}
          onAction={(action: string) => {
            console.log('Bulk action:', action);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  );
}

// Component for Pagination
function Pagination({ currentPage, totalPages, onPageChange, totalItems, itemsPerPage }: any) {
  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, totalItems);

  if (totalPages <= 1) return null;

  return (
    <div className="mt-4 flex items-center justify-between">
      <div className="text-sm text-gray-600">
        Showing {startItem} to {endItem} of {totalItems} results
      </div>
      <div className="flex items-center gap-2">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
        >
          <ChevronLeftIcon className="w-4 h-4" />
          Previous
        </button>
        
        <div className="flex items-center gap-1">
          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
            let pageNum;
            if (totalPages <= 5) {
              pageNum = i + 1;
            } else if (currentPage <= 3) {
              pageNum = i + 1;
            } else if (currentPage >= totalPages - 2) {
              pageNum = totalPages - 4 + i;
            } else {
              pageNum = currentPage - 2 + i;
            }

            return (
              <button
                key={pageNum}
                onClick={() => onPageChange(pageNum)}
                className={`px-3 py-2 rounded-lg transition-colors ${
                  currentPage === pageNum
                    ? 'bg-blue-600 text-white'
                    : 'border border-gray-300 hover:bg-gray-50'
                }`}
              >
                {pageNum}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
        >
          Next
          <ChevronRightIcon className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}

// Contact Detail View Component
function ContactDetailView({ contact, accounts, visibleFields, onBack, onEdit, onViewAllConversations, onNavigateToAccount }: any) {
  const recentConversations = [
    {
      id: 1,
      subject: 'API Integration Question',
      preview: 'Hi, I\'m trying to integrate the webhook...',
      date: '2 hours ago',
      status: 'resolved',
    },
    {
      id: 2,
      subject: 'Feature Request: Custom Fields',
      preview: 'Would love to see support for custom fields...',
      date: '1 day ago',
      status: 'open',
    },
    {
      id: 3,
      subject: 'Billing Question',
      preview: 'Quick question about our invoice...',
      date: '3 days ago',
      status: 'resolved',
    },
  ];

  return (
    <div>
      <button
        onClick={onBack}
        className="mb-4 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
      >
        <ChevronRight className="w-4 h-4 rotate-180" />
        Back to Contacts
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Contact Info */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-start justify-between mb-6">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 text-xl">
                {contact.name.split(' ').map((n: string) => n[0]).join('')}
              </div>
              <button 
                onClick={() => onEdit('contact')}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <Edit className="w-4 h-4 text-gray-600" />
              </button>
            </div>

            <h2 className="text-gray-900 mb-1">{contact.name}</h2>
            {visibleFields.role && <p className="text-sm text-gray-600 mb-4">{contact.role}</p>}

            {visibleFields.tags && (
              <div className="flex flex-wrap gap-1 mb-6">
                {contact.tags.map((tag: string, idx: number) => (
                  <span
                    key={idx}
                    className={`px-2 py-0.5 rounded text-xs ${
                      tag === 'VIP' ? 'bg-purple-100 text-purple-700' :
                      tag === 'Enterprise' ? 'bg-blue-100 text-blue-700' :
                      tag === 'Trial' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}

            <div className="space-y-3">
              {visibleFields.email && (
                <div className="flex items-center gap-3 text-sm">
                  <Mail className="w-4 h-4 text-gray-400" />
                  <a href={`mailto:${contact.email}`} className="text-blue-600 hover:underline">
                    {contact.email}
                  </a>
                </div>
              )}
              {visibleFields.phone && (
                <div className="flex items-center gap-3 text-sm">
                  <Phone className="w-4 h-4 text-gray-400" />
                  <span className="text-gray-900">{contact.phone}</span>
                </div>
              )}
              {visibleFields.company && (
                <div className="flex items-center gap-3 text-sm">
                  <Building2 className="w-4 h-4 text-gray-400" />
                  <button
                    onClick={() => onNavigateToAccount(contact.accountId)}
                    className="text-blue-600 hover:underline"
                  >
                    {contact.company}
                  </button>
                </div>
              )}
              <div className="flex items-center gap-3 text-sm">
                <Calendar className="w-4 h-4 text-gray-400" />
                <span className="text-gray-600">Created {contact.createdAt}</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <Clock className="w-4 h-4 text-gray-400" />
                <span className="text-gray-600">Last contact: {contact.lastContact}</span>
              </div>
            </div>
          </div>

          {/* Additional Info */}
          {(visibleFields.linkedin || visibleFields.timezone || visibleFields.preferredContact) && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Additional Info</h3>
                <button 
                  onClick={() => onEdit('additionalInfo')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <div className="space-y-3 text-sm">
                {visibleFields.linkedin && (
                  <div>
                    <div className="text-gray-600 mb-1">LinkedIn</div>
                    <a href={`https://${contact.customFields.linkedin}`} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline flex items-center gap-1">
                      {contact.customFields.linkedin}
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}
                {visibleFields.timezone && (
                  <div>
                    <div className="text-gray-600 mb-1">Timezone</div>
                    <div className="text-gray-900">{contact.customFields.timezone}</div>
                  </div>
                )}
                {visibleFields.preferredContact && (
                  <div>
                    <div className="text-gray-600 mb-1">Preferred Contact</div>
                    <div className="text-gray-900">{contact.customFields.preferredContact}</div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Notes */}
          {visibleFields.notes && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Notes</h3>
                <button 
                  onClick={() => onEdit('notes')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <p className="text-sm text-gray-700">{contact.notes}</p>
            </div>
          )}
        </div>

        {/* Conversations & Activity */}
        <div className="lg:col-span-2 space-y-6">
          {/* Stats */}
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <MessageSquare className="w-5 h-5 text-blue-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">{contact.conversationCount}</div>
              <div className="text-sm text-gray-600">Conversations</div>
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <Clock className="w-5 h-5 text-green-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">2.5h</div>
              <div className="text-sm text-gray-600">Avg Response Time</div>
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <TrendingUp className="w-5 h-5 text-purple-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">94%</div>
              <div className="text-sm text-gray-600">Satisfaction</div>
            </div>
          </div>

          {/* Recent Conversations */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-6 border-b border-gray-200 flex items-center justify-between">
              <h3 className="text-gray-900">Recent Conversations</h3>
              <button 
                onClick={onViewAllConversations}
                className="text-sm text-blue-600 hover:underline"
              >
                View All
              </button>
            </div>
            <div className="divide-y divide-gray-200">
              {recentConversations.map((conv) => (
                <div key={conv.id} className="p-6 hover:bg-gray-50 cursor-pointer transition-colors">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="text-gray-900 mb-1">{conv.subject}</div>
                      <p className="text-sm text-gray-600">{conv.preview}</p>
                    </div>
                    <span className={`ml-4 px-2 py-1 rounded-full text-xs flex-shrink-0 ${
                      conv.status === 'resolved' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>
                      {conv.status}
                    </span>
                  </div>
                  <div className="text-xs text-gray-500">{conv.date}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Activity Timeline */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="text-gray-900 mb-4">Activity Timeline</h3>
            <div className="space-y-4">
              <div className="flex gap-3">
                <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <MessageSquare className="w-4 h-4 text-blue-600" />
                </div>
                <div className="flex-1">
                  <div className="text-sm text-gray-900 mb-1">Started conversation about API integration</div>
                  <div className="text-xs text-gray-500">2 hours ago</div>
                </div>
              </div>
              <div className="flex gap-3">
                <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <CheckCircle2 className="w-4 h-4 text-green-600" />
                </div>
                <div className="flex-1">
                  <div className="text-sm text-gray-900 mb-1">Issue resolved: Webhook configuration</div>
                  <div className="text-xs text-gray-500">1 day ago</div>
                </div>
              </div>
              <div className="flex gap-3">
                <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <Star className="w-4 h-4 text-purple-600" />
                </div>
                <div className="flex-1">
                  <div className="text-sm text-gray-900 mb-1">Tagged as VIP customer</div>
                  <div className="text-xs text-gray-500">3 days ago</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Account Detail View Component
function AccountDetailView({ account, contacts, onClose, onNavigateToContact, onEdit }: any) {
  return (
    <div className="mt-6 bg-white rounded-xl border border-gray-200 p-6">
      <div className="flex items-start justify-between mb-6">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-purple-100 rounded-lg flex items-center justify-center">
            <Building2 className="w-8 h-8 text-purple-600" />
          </div>
          <div>
            <h2 className="text-gray-900 mb-1">{account.name}</h2>
            <p className="text-sm text-gray-600">{account.industry} • {account.size}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onEdit}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <Edit className="w-5 h-5 text-gray-600" />
          </button>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-600" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="p-4 bg-blue-50 rounded-lg">
          <DollarSign className="w-5 h-5 text-blue-600 mb-2" />
          <div className="text-2xl text-gray-900 mb-1">${account.mrr}</div>
          <div className="text-sm text-gray-600">MRR</div>
        </div>
        <div className="p-4 bg-green-50 rounded-lg">
          <Users className="w-5 h-5 text-green-600 mb-2" />
          <div className="text-2xl text-gray-900 mb-1">{account.contacts}</div>
          <div className="text-sm text-gray-600">Contacts</div>
        </div>
        <div className="p-4 bg-purple-50 rounded-lg">
          <MessageSquare className="w-5 h-5 text-purple-600 mb-2" />
          <div className="text-2xl text-gray-900 mb-1">{account.conversations}</div>
          <div className="text-sm text-gray-600">Conversations</div>
        </div>
        <div className="p-4 bg-orange-50 rounded-lg">
          <Calendar className="w-5 h-5 text-orange-600 mb-2" />
          <div className="text-2xl text-gray-900 mb-1">{account.customFields.contractEndDate}</div>
          <div className="text-sm text-gray-600">Contract End Date</div>
          <div className="text-xs text-gray-500 mt-1">Annual Value: ${account.customFields.annualValue.toLocaleString()}</div>
        </div>
      </div>

      <div className="text-sm text-gray-700 mb-6 p-4 bg-gray-50 rounded-lg">
        <strong>Notes:</strong> {account.notes}
      </div>

      <div className="border-t border-gray-200 pt-4">
        <h3 className="text-gray-900 mb-3">Contacts at {account.name}</h3>
        <div className="space-y-2">
          {contacts
            .filter((c: any) => c.accountId === account.id)
            .map((contact: any) => (
              <button
                key={contact.id}
                onClick={() => onNavigateToContact(contact)}
                className="w-full flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 text-sm">
                  {contact.name.split(' ').map((n: string) => n[0]).join('')}
                </div>
                <div className="flex-1 text-left">
                  <div className="text-gray-900">{contact.name}</div>
                  <div className="text-sm text-gray-600">{contact.role}</div>
                </div>
                <ChevronRight className="w-5 h-5 text-gray-400" />
              </button>
            ))}
        </div>
      </div>
    </div>
  );
}

// All Conversations View
function AllConversationsView({ contact, conversations }: any) {
  const [filter, setFilter] = useState('all');

  const filteredConversations = filter === 'all' 
    ? conversations 
    : conversations.filter((c: any) => c.status === filter);

  return (
    <div className="bg-white rounded-xl border border-gray-200">
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-gray-900">All Conversations with {contact.name}</h2>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="all">All Conversations</option>
            <option value="open">Open</option>
            <option value="resolved">Resolved</option>
          </select>
        </div>
        <div className="text-sm text-gray-600">{filteredConversations.length} conversations</div>
      </div>

      <div className="divide-y divide-gray-200">
        {filteredConversations.map((conv: any) => (
          <div key={conv.id} className="p-6 hover:bg-gray-50 cursor-pointer transition-colors">
            <div className="flex items-start justify-between mb-2">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <h3 className="text-gray-900">{conv.subject}</h3>
                  <span className={`px-2 py-0.5 rounded text-xs ${
                    conv.channel === 'email' ? 'bg-blue-100 text-blue-700' :
                    conv.channel === 'chat' ? 'bg-green-100 text-green-700' :
                    'bg-purple-100 text-purple-700'
                  }`}>
                    {conv.channel}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mb-2">{conv.preview}</p>
                <div className="text-xs text-gray-500">{conv.date}</div>
              </div>
              <span className={`ml-4 px-3 py-1 rounded-full text-xs flex-shrink-0 ${
                conv.status === 'resolved' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
              }`}>
                {conv.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// Modal Components
function ContactFormModal({ title, contact, onSave, onClose }: any) {
  const [formData, setFormData] = useState({
    name: contact?.name || '',
    email: contact?.email || '',
    phone: contact?.phone || '',
    company: contact?.company || '',
    role: contact?.role || '',
    linkedin: contact?.customFields?.linkedin || '',
    timezone: contact?.customFields?.timezone || '',
    preferredContact: contact?.customFields?.preferredContact || 'Email',
  });

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-4 md:px-6 py-4 flex items-center justify-between z-10">
          <h2 className="text-gray-900 text-base md:text-lg">{title}</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 md:p-6 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Full Name *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="John Doe"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Email *</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="john@company.com"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Phone</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="+1 (555) 123-4567"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Role</label>
              <input
                type="text"
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Product Manager"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Company</label>
            <input
              type="text"
              value={formData.company}
              onChange={(e) => setFormData({ ...formData, company: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Acme Inc"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">LinkedIn</label>
            <input
              type="text"
              value={formData.linkedin}
              onChange={(e) => setFormData({ ...formData, linkedin: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="linkedin.com/in/johndoe"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Timezone</label>
              <select
                value={formData.timezone}
                onChange={(e) => setFormData({ ...formData, timezone: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">Select timezone...</option>
                <option value="PST (UTC-8)">PST (UTC-8)</option>
                <option value="MST (UTC-7)">MST (UTC-7)</option>
                <option value="CST (UTC-6)">CST (UTC-6)</option>
                <option value="EST (UTC-5)">EST (UTC-5)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Preferred Contact</label>
              <select
                value={formData.preferredContact}
                onChange={(e) => setFormData({ ...formData, preferredContact: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="Email">Email</option>
                <option value="Phone">Phone</option>
                <option value="Chat">Chat</option>
                <option value="Video">Video Call</option>
              </select>
            </div>
          </div>
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onSave(formData)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            Save Contact
          </button>
        </div>
      </div>
    </div>
  );
}

function EditAdditionalInfoModal({ contact, onSave, onClose }: any) {
  const [formData, setFormData] = useState({
    linkedin: contact?.customFields?.linkedin || '',
    timezone: contact?.customFields?.timezone || '',
    preferredContact: contact?.customFields?.preferredContact || 'Email',
  });

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full my-8">
        <div className="border-b border-gray-200 px-4 md:px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900 text-base md:text-lg">Edit Additional Info</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm text-gray-700 mb-2">LinkedIn</label>
            <input
              type="text"
              value={formData.linkedin}
              onChange={(e) => setFormData({ ...formData, linkedin: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="linkedin.com/in/johndoe"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Timezone</label>
            <select
              value={formData.timezone}
              onChange={(e) => setFormData({ ...formData, timezone: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">Select timezone...</option>
              <option value="PST (UTC-8)">PST (UTC-8)</option>
              <option value="MST (UTC-7)">MST (UTC-7)</option>
              <option value="CST (UTC-6)">CST (UTC-6)</option>
              <option value="EST (UTC-5)">EST (UTC-5)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Preferred Contact Method</label>
            <select
              value={formData.preferredContact}
              onChange={(e) => setFormData({ ...formData, preferredContact: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="Email">Email</option>
              <option value="Phone">Phone</option>
              <option value="Chat">Chat</option>
              <option value="Video">Video Call</option>
            </select>
          </div>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onSave(formData)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
}

function AccountFormModal({ title, account, onSave, onClose }: any) {
  const [formData, setFormData] = useState({
    name: account?.name || '',
    domain: account?.domain || '',
    industry: account?.industry || '',
    size: account?.size || '',
    location: account?.location || '',
    plan: account?.plan || '',
    website: account?.customFields?.website || '',
    accountOwner: account?.customFields?.accountOwner || '',
    contractStartDate: account?.customFields?.contractStartDate || '',
    contractEndDate: account?.customFields?.contractEndDate || '',
  });

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full my-8 max-h-[calc(100vh-4rem)] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-4 md:px-6 py-4 flex items-center justify-between z-10">
          <h2 className="text-gray-900 text-base md:text-lg">{title}</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Company Name *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Acme Corp"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Domain *</label>
              <input
                type="text"
                value={formData.domain}
                onChange={(e) => setFormData({ ...formData, domain: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="acmecorp.com"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Industry</label>
              <select
                value={formData.industry}
                onChange={(e) => setFormData({ ...formData, industry: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">Select industry...</option>
                <option value="Technology">Technology</option>
                <option value="SaaS">SaaS</option>
                <option value="Manufacturing">Manufacturing</option>
                <option value="Healthcare">Healthcare</option>
                <option value="Finance">Finance</option>
                <option value="Retail">Retail</option>
                <option value="Education">Education</option>
                <option value="Design">Design</option>
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Company Size</label>
              <select
                value={formData.size}
                onChange={(e) => setFormData({ ...formData, size: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">Select size...</option>
                <option value="1-10 employees">1-10 employees</option>
                <option value="10-50 employees">10-50 employees</option>
                <option value="50-100 employees">50-100 employees</option>
                <option value="100-500 employees">100-500 employees</option>
                <option value="500-1000 employees">500-1000 employees</option>
                <option value="1000+ employees">1000+ employees</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Location</label>
            <input
              type="text"
              value={formData.location}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="San Francisco, CA"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Plan</label>
              <select
                value={formData.plan}
                onChange={(e) => setFormData({ ...formData, plan: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">Select plan...</option>
                <option value="Starter">Starter</option>
                <option value="Professional">Professional</option>
                <option value="Enterprise">Enterprise</option>
                <option value="Trial">Trial</option>
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Account Owner</label>
              <input
                type="text"
                value={formData.accountOwner}
                onChange={(e) => setFormData({ ...formData, accountOwner: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="John Smith"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">Contract Start Date</label>
              <input
                type="text"
                value={formData.contractStartDate}
                onChange={(e) => setFormData({ ...formData, contractStartDate: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Jan 1, 2024"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-700 mb-2">Contract End Date</label>
              <input
                type="text"
                value={formData.contractEndDate}
                onChange={(e) => setFormData({ ...formData, contractEndDate: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Dec 31, 2024"
              />
              <p className="text-xs text-gray-500 mt-1">This is when the contract or subscription is scheduled to end</p>
            </div>
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Website</label>
            <input
              type="url"
              value={formData.website}
              onChange={(e) => setFormData({ ...formData, website: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="https://acmecorp.com"
            />
          </div>
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onSave(formData)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            Save Account
          </button>
        </div>
      </div>
    </div>
  );
}

function DeleteConfirmModal({ title, message, onConfirm, onClose }: any) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full my-8">
        <div className="p-4 md:p-6">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="w-6 h-6 text-red-600" />
          </div>
          <h2 className="text-gray-900 text-center mb-2">{title}</h2>
          <p className="text-gray-600 text-center">{message}</p>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors flex items-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

function ExportDataModal({ viewMode, onExport, onClose }: any) {
  const [format, setFormat] = useState('csv');
  const [selectedFields, setSelectedFields] = useState<string[]>(['name', 'email', 'phone', 'company']);

  const allFields = viewMode === 'contacts'
    ? ['name', 'email', 'phone', 'company', 'role', 'tags', 'status', 'created_at', 'last_contact', 'conversations']
    : ['name', 'domain', 'industry', 'plan', 'mrr', 'contacts', 'status', 'created_at'];

  const toggleField = (field: string) => {
    setSelectedFields(prev =>
      prev.includes(field) ? prev.filter(f => f !== field) : [...prev, field]
    );
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">Export {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          <div>
            <label className="block text-sm text-gray-700 mb-2">Export Format</label>
            <select
              value={format}
              onChange={(e) => setFormat(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="csv">CSV (.csv)</option>
              <option value="excel">Excel (.xlsx)</option>
              <option value="json">JSON (.json)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-2">Fields to Export</label>
            <div className="border border-gray-300 rounded-lg divide-y divide-gray-200">
              {allFields.map(field => (
                <label key={field} className="flex items-center gap-3 p-3 hover:bg-gray-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedFields.includes(field)}
                    onChange={() => toggleField(field)}
                    className="w-4 h-4"
                  />
                  <span className="text-sm text-gray-900 capitalize">{field.replace('_', ' ')}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onExport(format, selectedFields)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            Export
          </button>
        </div>
      </div>
    </div>
  );
}

function ImportDataModal({ viewMode, onImport, onClose }: any) {
  const [file, setFile] = useState<File | null>(null);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">Import {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
            <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-900 mb-2">Drop your file here or click to browse</p>
            <p className="text-sm text-gray-600 mb-4">Supports CSV, Excel (.xlsx), and JSON files</p>
            <input
              type="file"
              accept=".csv,.xlsx,.json"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="hidden"
              id="file-upload"
            />
            <label
              htmlFor="file-upload"
              className="inline-block px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
            >
              Choose File
            </label>
            {file && (
              <div className="mt-4 text-sm text-gray-700">
                Selected: <strong>{file.name}</strong>
              </div>
            )}
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="text-sm text-blue-800">
              <strong>Tip:</strong> Make sure your file has column headers matching: name, email, phone, company, etc.
            </div>
          </div>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onImport(file, {})}
            disabled={!file}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Upload className="w-4 h-4" />
            Import
          </button>
        </div>
      </div>
    </div>
  );
}

function BulkActionModal({ selectedCount, onAction, onClose }: any) {
  const [action, setAction] = useState('');

  const actions = [
    { value: 'add_tag', label: 'Add Tag', icon: Tag },
    { value: 'remove_tag', label: 'Remove Tag', icon: Tag },
    { value: 'export', label: 'Export Selected', icon: Download },
    { value: 'delete', label: 'Delete Selected', icon: Trash2, danger: true },
    { value: 'merge', label: 'Merge Contacts', icon: Merge },
    { value: 'send_email', label: 'Send Bulk Email', icon: Send },
  ];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">Bulk Actions</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <p className="text-sm text-gray-600">
            {selectedCount} contact{selectedCount !== 1 && 's'} selected
          </p>

          <div className="space-y-2">
            {actions.map(({ value, label, icon: Icon, danger }) => (
              <button
                key={value}
                onClick={() => setAction(value)}
                className={`w-full flex items-center gap-3 p-3 border-2 rounded-lg transition-all ${
                  action === value
                    ? danger
                      ? 'border-red-500 bg-red-50'
                      : 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <Icon className={`w-5 h-5 ${danger ? 'text-red-600' : 'text-gray-600'}`} />
                <span className={`flex-1 text-left ${danger ? 'text-red-900' : 'text-gray-900'}`}>{label}</span>
                {action === value && <CheckCircle2 className={`w-5 h-5 ${danger ? 'text-red-600' : 'text-blue-600'}`} />}
              </button>
            ))}
          </div>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onAction(action)}
            disabled={!action}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ArrowRight className="w-4 h-4" />
            Continue
          </button>
        </div>
      </div>
    </div>
  );
}

function CheckCircle2({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  );
}
