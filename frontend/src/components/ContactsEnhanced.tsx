import { useState, useEffect } from 'react';
import { Search, Plus, Building2, User, List, Download, Upload, Settings, MoreVertical, X, CheckCircle2, Edit } from 'lucide-react';
import { ViewsPanel } from './ViewsPanel';
import { ColumnCustomizer } from './ColumnCustomizer';
import { AdvancedFilterPanel, FilterGroup } from './AdvancedFilterPanel';
import { CreateViewModal } from './CreateViewModal';
import { Pagination } from './Pagination';
import { ContactDetailView } from './ContactDetailView';
import { AccountDetailView } from './AccountDetailView';
import { ContactFormModal, AccountFormModal, ExportDataModal, ImportDataModal, SettingsModal } from './ContactAccountModals';

type ViewMode = 'contacts' | 'accounts';
type DetailView = 'list' | 'contactDetail' | 'accountDetail';
type ActiveModal = 'newContact' | 'newAccount' | 'export' | 'import' | 'settings' | 'createView' | null;

interface Column {
  id: string;
  label: string;
  visible: boolean;
  required?: boolean;
}

interface View {
  id: string;
  name: string;
  isDefault?: boolean;
  isPredefined?: boolean;
  filters: FilterGroup;
  visibleColumns: string[];
  icon?: any;
}

interface ContactsEnhancedProps {
  initialContactEmail?: string | null;
  onClearInitial?: () => void;
}

export function ContactsEnhanced({ initialContactEmail, onClearInitial }: ContactsEnhancedProps = {}) {
  const [viewMode, setViewMode] = useState<ViewMode>('contacts');
  const [detailView, setDetailView] = useState<DetailView>('list');
  const [selectedContact, setSelectedContact] = useState<any>(null);
  const [selectedAccount, setSelectedAccount] = useState<any>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(5);
  
  // Modal State
  const [activeModal, setActiveModal] = useState<ActiveModal>(null);

  // Views Management
  const [views, setViews] = useState<View[]>([
    {
      id: 'all-contacts',
      name: 'All Contacts',
      isPredefined: true,
      isDefault: true,
      icon: List,
      filters: { logic: 'AND', conditions: [] },
      visibleColumns: ['contact', 'company', 'role', 'tags', 'conversations', 'lastContact', 'status'],
    },
    {
      id: 'active-contacts',
      name: 'Active Contacts',
      isPredefined: true,
      icon: User,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'status-active',
          field: 'status',
          operator: 'equals',
          value: 'active',
        }],
      },
      visibleColumns: ['contact', 'company', 'role', 'tags', 'conversations', 'lastContact', 'status'],
    },
    {
      id: 'vip-contacts',
      name: 'VIP Contacts',
      isPredefined: true,
      icon: User,
      filters: {
        logic: 'AND',
        conditions: [{
          id: 'tag-vip',
          field: 'tags',
          operator: 'contains',
          value: 'VIP',
        }],
      },
      visibleColumns: ['contact', 'company', 'role', 'tags', 'conversations', 'lastContact', 'status'],
    },
  ]);

  const [activeViewId, setActiveViewId] = useState('all-contacts');
  const activeView = views.find(v => v.id === activeViewId) || views[0];

  // Columns Management
  const contactColumns: Column[] = [
    { id: 'contact', label: 'Contact', visible: true, required: true },
    { id: 'company', label: 'Company', visible: true },
    { id: 'role', label: 'Role', visible: true },
    { id: 'email', label: 'Email', visible: false },
    { id: 'phone', label: 'Phone', visible: false },
    { id: 'tags', label: 'Tags', visible: true },
    { id: 'conversations', label: 'Conversations', visible: true },
    { id: 'lastContact', label: 'Last Contact', visible: true },
    { id: 'status', label: 'Status', visible: true },
    { id: 'createdAt', label: 'Created', visible: false },
  ];

  const accountColumns: Column[] = [
    { id: 'account', label: 'Account', visible: true, required: true },
    { id: 'industry', label: 'Industry', visible: true },
    { id: 'size', label: 'Size', visible: true },
    { id: 'location', label: 'Location', visible: true },
    { id: 'plan', label: 'Plan', visible: true },
    { id: 'mrr', label: 'MRR', visible: true },
    { id: 'contacts', label: 'Contacts', visible: true },
    { id: 'status', label: 'Status', visible: true },
    { id: 'createdAt', label: 'Created', visible: false },
  ];

  const [columns, setColumns] = useState(contactColumns);

  // Filter Panel
  const [showFilterPanel, setShowFilterPanel] = useState(false);
  const [filterGroup, setFilterGroup] = useState<FilterGroup>(activeView.filters);

  // Available fields for filtering
  const contactFilterFields = [
    { id: 'name', label: 'Name', type: 'text' as const },
    { id: 'email', label: 'Email', type: 'text' as const },
    { id: 'company', label: 'Company', type: 'text' as const },
    { id: 'role', label: 'Role', type: 'text' as const },
    { id: 'status', label: 'Status', type: 'select' as const, options: ['active', 'inactive'] },
    { id: 'tags', label: 'Tags', type: 'text' as const },
    { id: 'conversations', label: 'Conversations', type: 'number' as const },
    { id: 'createdAt', label: 'Created Date', type: 'date' as const },
  ];

  const accountFilterFields = [
    { id: 'name', label: 'Account Name', type: 'text' as const },
    { id: 'industry', label: 'Industry', type: 'text' as const },
    { id: 'size', label: 'Company Size', type: 'text' as const },
    { id: 'status', label: 'Status', type: 'select' as const, options: ['active', 'trial', 'inactive'] },
    { id: 'plan', label: 'Plan', type: 'text' as const },
    { id: 'mrr', label: 'MRR', type: 'number' as const },
    { id: 'contacts', label: 'Number of Contacts', type: 'number' as const },
    { id: 'createdAt', label: 'Created Date', type: 'date' as const },
  ];

  // Create/Edit View Modal
  const [showCreateViewModal, setShowCreateViewModal] = useState(false);
  const [editingView, setEditingView] = useState<View | null>(null);

  // Sample Data with accountIds
  const contacts = [
    {
      id: 1,
      name: 'Sarah Johnson',
      email: 'sarah.johnson@techcorp.com',
      phone: '+1 (555) 123-4567',
      company: 'TechCorp Industries',
      accountId: 1,
      role: 'Product Manager',
      tags: ['VIP', 'Enterprise'],
      conversationCount: 12,
      lastContact: '2 hours ago',
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
      tags: ['Trial', 'Technical'],
      conversationCount: 5,
      lastContact: '1 day ago',
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
      tags: ['Enterprise', 'Support'],
      conversationCount: 28,
      lastContact: '3 days ago',
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
      tags: ['Small Business'],
      conversationCount: 3,
      lastContact: '1 week ago',
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
      tags: ['VIP', 'Enterprise'],
      conversationCount: 45,
      lastContact: '4 hours ago',
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
      tags: ['Trial'],
      conversationCount: 2,
      lastContact: '2 weeks ago',
      status: 'inactive',
      createdAt: 'Mar 25, 2024',
      notes: 'Trial expired. Follow up needed.',
      customFields: {
        linkedin: 'linkedin.com/in/robertmartinez',
        timezone: 'MST (UTC-7)',
        preferredContact: 'Email',
      },
    },
    {
      id: 7,
      name: 'John Davis',
      email: 'john.davis@email.com',
      phone: '+1 (555) 789-0123',
      company: 'Tech Innovations',
      accountId: 7,
      role: 'Customer',
      tags: ['Support'],
      conversationCount: 5,
      lastContact: '2 min ago',
      status: 'active',
      createdAt: 'Nov 28, 2024',
      notes: 'Active support conversation about password reset.',
      customFields: {
        linkedin: 'linkedin.com/in/johndavis',
        timezone: 'EST (UTC-5)',
        preferredContact: 'Chat',
      },
    },
    {
      id: 8,
      name: 'Emma Wilson',
      email: 'emma.w@email.com',
      phone: '+1 (555) 890-1234',
      company: 'Wilson Corp',
      accountId: 8,
      role: 'Account Manager',
      tags: ['Billing'],
      conversationCount: 3,
      lastContact: '15 min ago',
      status: 'active',
      createdAt: 'Nov 20, 2024',
      notes: 'Billing inquiry about subscription charges.',
      customFields: {
        linkedin: 'linkedin.com/in/emmawilson',
        timezone: 'PST (UTC-8)',
        preferredContact: 'Email',
      },
    },
    {
      id: 9,
      name: 'Michael Brown',
      email: 'mbrown@email.com',
      phone: '+1 (555) 901-2345',
      company: 'Brown Industries',
      accountId: 9,
      role: 'Operations Manager',
      tags: ['Shipping', 'Urgent'],
      conversationCount: 4,
      lastContact: '1 hour ago',
      status: 'active',
      createdAt: 'Nov 15, 2024',
      notes: 'Following up on delayed order #12345.',
      customFields: {
        linkedin: 'linkedin.com/in/michaelbrown',
        timezone: 'CST (UTC-6)',
        preferredContact: 'SMS',
      },
    },
    {
      id: 10,
      name: 'Lisa Anderson',
      email: 'lisa.a@email.com',
      phone: '+1 (555) 012-3456',
      company: 'Anderson LLC',
      accountId: 10,
      role: 'Product Owner',
      tags: ['Product'],
      conversationCount: 6,
      lastContact: '2 hours ago',
      status: 'active',
      createdAt: 'Nov 10, 2024',
      notes: 'Product features conversation - data export.',
      customFields: {
        linkedin: 'linkedin.com/in/lisaanderson',
        timezone: 'PST (UTC-8)',
        preferredContact: 'WhatsApp',
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
        accountOwner: 'Alex Thompson',
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
        accountOwner: 'Alex Thompson',
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
        accountOwner: 'Alex Thompson',
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
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Mar 1, 2024',
        contractEndDate: 'Mar 1, 2025',
        annualValue: 348,
      },
    },
    {
      id: 5,
      name: 'Enterprise Solutions',
      domain: 'enterprise.com',
      industry: 'Enterprise Software',
      size: '1000+ employees',
      location: 'Boston, MA',
      status: 'active',
      plan: 'Enterprise Plus',
      mrr: 7500,
      contacts: 25,
      conversations: 200,
      createdAt: 'Nov 10, 2023',
      tags: ['VIP', 'Enterprise'],
      notes: 'Premium tier customer. White glove service.',
      customFields: {
        website: 'https://enterprise.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Nov 1, 2023',
        contractEndDate: 'Oct 31, 2024',
        annualValue: 90000,
      },
    },
    {
      id: 6,
      name: 'Martinez Consulting',
      domain: 'consulting.com',
      industry: 'Consulting',
      size: '50-100 employees',
      location: 'Denver, CO',
      status: 'inactive',
      plan: 'Professional',
      mrr: 0,
      contacts: 1,
      conversations: 2,
      createdAt: 'Mar 25, 2024',
      tags: ['Trial'],
      notes: 'Trial expired. Follow up needed.',
      customFields: {
        website: 'https://consulting.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Mar 25, 2024',
        contractEndDate: 'Apr 25, 2024',
        annualValue: 0,
      },
    },
    {
      id: 7,
      name: 'Tech Innovations',
      domain: 'techinnovations.com',
      industry: 'Technology',
      size: '100-500 employees',
      location: 'Seattle, WA',
      status: 'active',
      plan: 'Professional',
      mrr: 499,
      contacts: 5,
      conversations: 15,
      createdAt: 'Nov 28, 2024',
      tags: ['Support'],
      notes: 'Growing account. Good engagement.',
      customFields: {
        website: 'https://techinnovations.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Nov 28, 2024',
        contractEndDate: 'Nov 28, 2025',
        annualValue: 5988,
      },
    },
    {
      id: 8,
      name: 'Wilson Corp',
      domain: 'wilsoncorp.com',
      industry: 'Retail',
      size: '500-1000 employees',
      location: 'Portland, OR',
      status: 'active',
      plan: 'Enterprise',
      mrr: 1999,
      contacts: 12,
      conversations: 35,
      createdAt: 'Nov 20, 2024',
      tags: ['Billing'],
      notes: 'Large retail chain. Seasonal demand.',
      customFields: {
        website: 'https://wilsoncorp.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Nov 20, 2024',
        contractEndDate: 'Nov 20, 2025',
        annualValue: 23988,
      },
    },
    {
      id: 9,
      name: 'Brown Industries',
      domain: 'brownindustries.com',
      industry: 'Manufacturing',
      size: '1000+ employees',
      location: 'Detroit, MI',
      status: 'active',
      plan: 'Enterprise',
      mrr: 3500,
      contacts: 18,
      conversations: 85,
      createdAt: 'Nov 15, 2024',
      tags: ['Shipping', 'Urgent'],
      notes: 'Large manufacturer. Complex supply chain needs.',
      customFields: {
        website: 'https://brownindustries.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Nov 15, 2024',
        contractEndDate: 'Nov 15, 2025',
        annualValue: 42000,
      },
    },
    {
      id: 10,
      name: 'Anderson LLC',
      domain: 'andersonllc.com',
      industry: 'Legal Services',
      size: '50-100 employees',
      location: 'Miami, FL',
      status: 'active',
      plan: 'Professional',
      mrr: 699,
      contacts: 6,
      conversations: 22,
      createdAt: 'Nov 10, 2024',
      tags: ['Product'],
      notes: 'Legal firm. Data privacy concerns.',
      customFields: {
        website: 'https://andersonllc.com',
        accountOwner: 'Alex Thompson',
        contractStartDate: 'Nov 10, 2024',
        contractEndDate: 'Nov 10, 2025',
        annualValue: 8388,
      },
    },
  ];

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

  const handleSaveView = (viewData: { name: string; description: string }) => {
    if (editingView) {
      // Update existing view
      setViews(views.map(v =>
        v.id === editingView.id
          ? { ...v, name: viewData.name }
          : v
      ));
    } else {
      // Create new view
      const newView: View = {
        id: `view-${Date.now()}`,
        name: viewData.name,
        isPredefined: false,
        filters: filterGroup,
        visibleColumns: columns.filter(c => c.visible).map(c => c.id),
      };
      setViews([...views, newView]);
      setActiveViewId(newView.id);
    }
  };

  const handleColumnsChange = (newColumns: Column[]) => {
    setColumns(newColumns);
  };

  const handleApplyFilters = () => {
    console.log('Applying filters:', filterGroup);
  };

  const handleResetFilters = () => {
    setFilterGroup({ logic: 'AND', conditions: [] });
  };

  const handleContactClick = (contact: any) => {
    setSelectedContact(contact);
    setDetailView('contactDetail');
  };

  const handleAccountClick = (account: any) => {
    setSelectedAccount(account);
    setDetailView('accountDetail');
  };

  const handleBackToList = () => {
    setDetailView('list');
    setSelectedContact(null);
    setSelectedAccount(null);
  };

  const visibleColumns = columns.filter(col => col.visible);
  const activeFilterCount = filterGroup.conditions.length;

  // Pagination
  const totalContacts = contacts.length;
  const totalAccounts = accounts.length;
  const totalPages = viewMode === 'contacts' 
    ? Math.ceil(totalContacts / itemsPerPage)
    : Math.ceil(totalAccounts / itemsPerPage);

  const paginatedContacts = contacts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const paginatedAccounts = accounts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  // Handle navigation from conversations
  useEffect(() => {
    if (initialContactEmail) {
      const contact = contacts.find(c => c.email === initialContactEmail);
      if (contact) {
        setSelectedContact(contact);
        setDetailView('contactDetail');
        setViewMode('contacts');
      }
      if (onClearInitial) {
        onClearInitial();
      }
    }
  }, [initialContactEmail]);

  // Show detail views
  if (detailView === 'contactDetail' && selectedContact) {
    return (
      <div className="p-8">
        <ContactDetailView
          contact={selectedContact}
          accounts={accounts}
          visibleFields={{
            email: true,
            phone: true,
            company: true,
            role: true,
            linkedin: true,
            timezone: true,
            preferredContact: true,
            tags: true,
            notes: true,
          }}
          onBack={handleBackToList}
          onEdit={(field) => console.log('Edit field:', field)}
          onViewAllConversations={() => console.log('View all conversations')}
          onNavigateToAccount={(accountId) => {
            const account = accounts.find(a => a.id === accountId);
            if (account) {
              setSelectedAccount(account);
              setDetailView('accountDetail');
            }
          }}
        />
      </div>
    );
  }

  if (detailView === 'accountDetail' && selectedAccount) {
    return (
      <div className="p-8">
        <AccountDetailView
          account={selectedAccount}
          contacts={contacts}
          onClose={handleBackToList}
          onEdit={() => console.log('Edit account')}
          onNavigateToContact={(contact) => {
            setSelectedContact(contact);
            setDetailView('contactDetail');
          }}
        />
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-gray-900 mb-2">Contacts & Accounts</h1>
          <p className="text-gray-600">Manage customer relationships and view conversation history</p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={() => setActiveModal('export')}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            Export
          </button>
          <button 
            onClick={() => setActiveModal('import')}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <Upload className="w-4 h-4" />
            Import
          </button>
          <button 
            onClick={() => setActiveModal('settings')}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <Settings className="w-4 h-4" />
            Settings
          </button>
          <button 
            onClick={() => setActiveModal(viewMode === 'contacts' ? 'newContact' : 'newAccount')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="w-5 h-5" />
            New {viewMode === 'contacts' ? 'Contact' : 'Account'}
          </button>
        </div>
      </div>

      {/* View Toggle & Controls */}
      <div className="mb-6 flex items-center gap-4">
        {/* View Mode Toggle */}
        <div className="flex bg-gray-100 rounded-lg p-1">
          <button
            onClick={() => {
              setViewMode('contacts');
              setColumns(contactColumns);
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
              setColumns(accountColumns);
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

        {/* Views Selector */}
        <ViewsPanel
          views={views}
          activeView={activeViewId}
          onViewChange={setActiveViewId}
          onCreateView={handleCreateView}
          onEditView={handleEditView}
          onDeleteView={handleDeleteView}
          onDuplicateView={handleDuplicateView}
        />

        {/* Search */}
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

        {/* Column Customizer */}
        <ColumnCustomizer
          columns={columns}
          onColumnsChange={handleColumnsChange}
        />

        {/* Filter Button */}
        <button
          onClick={() => setShowFilterPanel(true)}
          className={`px-4 py-2 border rounded-lg transition-colors flex items-center gap-2 ${
            activeFilterCount > 0
              ? 'border-blue-500 bg-blue-50 text-blue-700'
              : 'border-gray-300 hover:bg-gray-50'
          }`}
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          Filter
          {activeFilterCount > 0 && (
            <span className="px-2 py-0.5 bg-blue-600 text-white rounded-full text-xs">
              {activeFilterCount}
            </span>
          )}
        </button>
      </div>

      {/* Data Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">
                <input type="checkbox" className="w-4 h-4" />
              </th>
              {visibleColumns.map(col => (
                <th key={col.id} className="text-left px-6 py-3 text-xs text-gray-600 uppercase tracking-wider">
                  {col.label}
                </th>
              ))}
              <th className="px-6 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {viewMode === 'contacts' ? (
              paginatedContacts.map((contact) => (
                <tr key={contact.id} className="hover:bg-gray-50 cursor-pointer transition-colors" onClick={() => handleContactClick(contact)}>
                  <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                    <input type="checkbox" className="w-4 h-4" />
                  </td>
                  {visibleColumns.map(col => {
                    if (col.id === 'contact') {
                      return (
                        <td key={col.id} className="px-6 py-4">
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
                      );
                    }
                    if (col.id === 'company') return <td key={col.id} className="px-6 py-4 text-gray-900">{contact.company}</td>;
                    if (col.id === 'role') return <td key={col.id} className="px-6 py-4 text-gray-600 text-sm">{contact.role}</td>;
                    if (col.id === 'email') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{contact.email}</td>;
                    if (col.id === 'phone') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{contact.phone}</td>;
                    if (col.id === 'tags') {
                      return (
                        <td key={col.id} className="px-6 py-4">
                          <div className="flex gap-1 flex-wrap">
                            {contact.tags.map((tag, idx) => (
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
                        </td>
                      );
                    }
                    if (col.id === 'conversations') return <td key={col.id} className="px-6 py-4 text-gray-900">{contact.conversationCount}</td>;
                    if (col.id === 'lastContact') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{contact.lastContact}</td>;
                    if (col.id === 'status') {
                      return (
                        <td key={col.id} className="px-6 py-4">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            contact.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                          }`}>
                            {contact.status}
                          </span>
                        </td>
                      );
                    }
                    if (col.id === 'createdAt') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{contact.createdAt}</td>;
                    return null;
                  })}
                  <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                    <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                      <MoreVertical className="w-4 h-4 text-gray-600" />
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              paginatedAccounts.map((account) => (
                <tr key={account.id} className="hover:bg-gray-50 cursor-pointer transition-colors" onClick={() => handleAccountClick(account)}>
                  <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                    <input type="checkbox" className="w-4 h-4" />
                  </td>
                  {visibleColumns.map(col => {
                    if (col.id === 'account') {
                      return (
                        <td key={col.id} className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                              <Building2 className="w-5 h-5 text-purple-700" />
                            </div>
                            <div>
                              <div className="text-gray-900">{account.name}</div>
                              <div className="text-sm text-gray-600">{account.industry}</div>
                            </div>
                          </div>
                        </td>
                      );
                    }
                    if (col.id === 'industry') return <td key={col.id} className="px-6 py-4 text-gray-900">{account.industry}</td>;
                    if (col.id === 'size') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{account.size}</td>;
                    if (col.id === 'location') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{account.location}</td>;
                    if (col.id === 'plan') return <td key={col.id} className="px-6 py-4 text-gray-900">{account.plan}</td>;
                    if (col.id === 'mrr') return <td key={col.id} className="px-6 py-4 text-gray-900">${account.mrr}</td>;
                    if (col.id === 'contacts') return <td key={col.id} className="px-6 py-4 text-gray-900">{account.contacts}</td>;
                    if (col.id === 'status') {
                      return (
                        <td key={col.id} className="px-6 py-4">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            account.status === 'active' ? 'bg-green-100 text-green-700' :
                            account.status === 'trial' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-gray-100 text-gray-700'
                          }`}>
                            {account.status}
                          </span>
                        </td>
                      );
                    }
                    if (col.id === 'createdAt') return <td key={col.id} className="px-6 py-4 text-sm text-gray-600">{account.createdAt}</td>;
                    return null;
                  })}
                  <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                    <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                      <MoreVertical className="w-4 h-4 text-gray-600" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={viewMode === 'contacts' ? totalContacts : totalAccounts}
          itemsPerPage={itemsPerPage}
          onPageChange={setCurrentPage}
          onItemsPerPageChange={setItemsPerPage}
        />
      </div>

      {/* Advanced Filter Panel */}
      <AdvancedFilterPanel
        isOpen={showFilterPanel}
        onClose={() => setShowFilterPanel(false)}
        filterGroup={filterGroup}
        onFilterChange={setFilterGroup}
        availableFields={viewMode === 'contacts' ? contactFilterFields : accountFilterFields}
        onApply={handleApplyFilters}
        onReset={handleResetFilters}
      />

      {/* Create/Edit View Modal */}
      <CreateViewModal
        isOpen={showCreateViewModal}
        onClose={() => {
          setShowCreateViewModal(false);
          setEditingView(null);
        }}
        onSave={handleSaveView}
        editingView={editingView ? { name: editingView.name, description: '' } : null}
      />

      {/* New Contact/Account Modals */}
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

      {/* Export/Import/Settings Modals */}
      {activeModal === 'export' && (
        <ExportDataModal
          viewMode={viewMode}
          onExport={(format: string, fields: string[]) => {
            console.log('Exporting:', format, fields);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'import' && (
        <ImportDataModal
          viewMode={viewMode}
          onImport={(file: File | null) => {
            console.log('Importing file:', file);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'settings' && (
        <SettingsModal
          viewMode={viewMode}
          onSave={(settings: any) => {
            console.log('Saving settings:', settings);
            setActiveModal(null);
          }}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  );
}