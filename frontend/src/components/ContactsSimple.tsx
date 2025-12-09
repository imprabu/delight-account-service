import { useState } from 'react';
import { Search, User, Mail, Phone, MessageSquare, Calendar, X, Plus, Building2, Filter, StickyNote, Edit2, Save } from 'lucide-react';
import { Pagination } from './Pagination';
import { DemoBanner } from './DemoBanner';

interface Contact {
  id: number;
  name: string;
  email: string;
  phone: string;
  company: string;
  lastContact: string;
  conversationCount: number;
  notes: string;
  customFields: { [key: string]: string };
}

interface ContactsSimpleProps {
  initialContactEmail?: string | null;
  onClearInitial?: () => void;
}

export function ContactsSimple({ initialContactEmail, onClearInitial }: ContactsSimpleProps = {}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [showNewContactModal, setShowNewContactModal] = useState(false);
  const [editingNotes, setEditingNotes] = useState(false);
  const [notesText, setNotesText] = useState('');
  
  // Filters
  const [companyFilter, setCompanyFilter] = useState<string>('all');
  const [activityFilter, setActivityFilter] = useState<string>('all');
  const [showFilters, setShowFilters] = useState(false);
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // Mock data for contacts
  const contacts: Contact[] = [
    {
      id: 1,
      name: 'John Davis',
      email: 'john.davis@email.com',
      phone: '+1 (555) 123-4567',
      company: 'Tech Corp',
      lastContact: '2 days ago',
      conversationCount: 5,
      notes: 'John is a key decision maker.',
      customFields: {
        'Job Title': 'CEO',
        'Department': 'Technology',
      },
    },
    {
      id: 2,
      name: 'Emma Wilson',
      email: 'emma.w@email.com',
      phone: '+1 (555) 234-5678',
      company: 'Design Studio',
      lastContact: '1 week ago',
      conversationCount: 3,
      notes: 'Emma is the lead designer.',
      customFields: {
        'Job Title': 'Lead Designer',
        'Department': 'Creative',
      },
    },
    {
      id: 3,
      name: 'Michael Brown',
      email: 'mbrown@email.com',
      phone: '+1 (555) 345-6789',
      company: 'Retail Plus',
      lastContact: '3 days ago',
      conversationCount: 4,
      notes: 'Michael is the retail manager.',
      customFields: {
        'Job Title': 'Retail Manager',
        'Department': 'Retail',
      },
    },
    {
      id: 4,
      name: 'Lisa Anderson',
      email: 'lisa.a@email.com',
      phone: '+1 (555) 456-7890',
      company: 'Marketing Agency',
      lastContact: '5 days ago',
      conversationCount: 6,
      notes: 'Lisa is the marketing director.',
      customFields: {
        'Job Title': 'Marketing Director',
        'Department': 'Marketing',
      },
    },
    {
      id: 5,
      name: 'David Martinez',
      email: 'david.m@email.com',
      phone: '+1 (555) 567-8901',
      company: 'Tech Corp',
      lastContact: '1 day ago',
      conversationCount: 2,
      notes: 'David is a software engineer.',
      customFields: {
        'Job Title': 'Software Engineer',
        'Department': 'Technology',
      },
    },
    {
      id: 6,
      name: 'Sarah Johnson',
      email: 'sarah.j@email.com',
      phone: '+1 (555) 678-9012',
      company: 'Software Inc',
      lastContact: '4 days ago',
      conversationCount: 7,
      notes: 'Sarah is the project manager.',
      customFields: {
        'Job Title': 'Project Manager',
        'Department': 'Technology',
      },
    },
  ];

  // Filter based on search
  const filteredContacts = contacts.filter(contact => {
    // Search filter
    const matchesSearch = contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.company.toLowerCase().includes(searchQuery.toLowerCase());
    
    // Company filter
    const matchesCompany = companyFilter === 'all' || contact.company === companyFilter;
    
    // Activity filter
    let matchesActivity = true;
    if (activityFilter === 'recent') {
      matchesActivity = ['1 day ago', '2 days ago', '3 days ago'].includes(contact.lastContact);
    } else if (activityFilter === 'older') {
      matchesActivity = ['5 days ago', '1 week ago'].includes(contact.lastContact);
    }
    
    return matchesSearch && matchesCompany && matchesActivity;
  });

  // Get unique companies for filter
  const companies = Array.from(new Set(contacts.map(c => c.company))).sort();

  // Pagination
  const totalPages = Math.ceil(filteredContacts.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedContacts = filteredContacts.slice(startIndex, endIndex);

  // Reset to page 1 when filters change
  const handleFilterChange = () => {
    setCurrentPage(1);
  };

  // Auto-select contact if initialContactEmail is provided
  useState(() => {
    if (initialContactEmail) {
      const contact = contacts.find(c => c.email === initialContactEmail);
      if (contact) {
        setSelectedContact(contact);
        onClearInitial?.();
      }
    }
  });

  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="px-6 pt-2">
          <DemoBanner />
        </div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-gray-900 mb-1">
              Contacts
            </h1>
            <p className="text-sm text-gray-600">
              View and manage customer contacts
            </p>
          </div>

          {/* View Toggle */}
          <div className="flex gap-2">
            <button
              onClick={() => setShowNewContactModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              New Contact
            </button>
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                showFilters
                  ? 'bg-blue-50 text-blue-600 border border-blue-200'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              <Filter className="w-4 h-4" />
              Filters
              {(companyFilter !== 'all' || activityFilter !== 'all') && (
                <span className="px-2 py-0.5 bg-blue-600 text-white rounded-full text-xs">
                  {(companyFilter !== 'all' ? 1 : 0) + (activityFilter !== 'all' ? 1 : 0)}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Filters Panel */}
        {showFilters && (
          <div className="p-4 bg-gray-50 border-t border-gray-200">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs text-gray-600 mb-2">Company</label>
                <select
                  value={companyFilter}
                  onChange={(e) => {
                    setCompanyFilter(e.target.value);
                    handleFilterChange();
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                >
                  <option value="all">All Companies</option>
                  {companies.map(company => (
                    <option key={company} value={company}>{company}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-2">Activity</label>
                <select
                  value={activityFilter}
                  onChange={(e) => {
                    setActivityFilter(e.target.value);
                    handleFilterChange();
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                >
                  <option value="all">All Activity</option>
                  <option value="recent">Recent (Last 3 days)</option>
                  <option value="older">Older (5+ days)</option>
                </select>
              </div>
              <div className="flex items-end">
                <button
                  onClick={() => {
                    setCompanyFilter('all');
                    setActivityFilter('all');
                    handleFilterChange();
                  }}
                  className="w-full px-3 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded-lg transition-colors text-sm"
                >
                  Clear Filters
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Search */}
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`Search contacts...`}
            className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* List View */}
        <div className={selectedContact ? 'w-96 border-r border-gray-200' : 'flex-1'}>
          <div className="h-full overflow-y-auto bg-white">
            {/* Contacts List */}
            <div className="divide-y divide-gray-200">
              {paginatedContacts.length === 0 ? (
                <div className="p-8 text-center">
                  <User className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                  <p className="text-gray-600">No contacts found</p>
                </div>
              ) : (
                paginatedContacts.map(contact => (
                  <div
                    key={contact.id}
                    onClick={() => setSelectedContact(contact)}
                    className={`p-4 cursor-pointer transition-colors hover:bg-gray-50 ${
                      selectedContact?.id === contact.id ? 'bg-blue-50 border-l-4 border-l-blue-600' : ''
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3 flex-1">
                        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0">
                          <User className="w-5 h-5 text-blue-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="text-sm text-gray-900 mb-1">{contact.name}</div>
                          <div className="text-xs text-gray-600 mb-1 flex items-center gap-1">
                            <Mail className="w-3 h-3" />
                            {contact.email}
                          </div>
                          <div className="text-xs text-gray-600 mb-1 flex items-center gap-1">
                            <Phone className="w-3 h-3" />
                            {contact.phone}
                          </div>
                          <div className="text-xs text-gray-600 flex items-center gap-1">
                            <Building2 className="w-3 h-3" />
                            {contact.company}
                          </div>
                        </div>
                      </div>
                      <div className="text-right flex-shrink-0 ml-4">
                        <div className="text-xs text-gray-500 mb-1">{contact.lastContact}</div>
                        <div className="text-xs text-gray-600 flex items-center gap-1">
                          <MessageSquare className="w-3 h-3" />
                          {contact.conversationCount} conversations
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
          {/* Pagination */}
          {!selectedContact && filteredContacts.length > 0 && (
            <div className="border-t border-gray-200 p-4 bg-white">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                itemsPerPage={itemsPerPage}
                totalItems={filteredContacts.length}
                onPageChange={setCurrentPage}
                onItemsPerPageChange={(items) => {
                  setItemsPerPage(items);
                  setCurrentPage(1);
                }}
              />
            </div>
          )}
        </div>

        {/* Detail View */}
        {selectedContact && (
          <div className="flex-1 bg-white overflow-y-auto">
            <div className="p-6">
              {/* Header */}
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-start gap-4">
                  <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                    <User className="w-8 h-8 text-blue-600" />
                  </div>
                  <div>
                    <h2 className="text-gray-900 mb-1">{selectedContact.name}</h2>
                    <p className="text-sm text-gray-600">{selectedContact.company}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedContact(null)}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5 text-gray-600" />
                </button>
              </div>

              {/* Contact Information */}
              <div className="space-y-6">
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Contact Information</h3>
                  <div className="space-y-3">
                    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                      <Mail className="w-5 h-5 text-gray-400" />
                      <div>
                        <div className="text-xs text-gray-600 mb-1">Email</div>
                        <div className="text-sm text-gray-900">{selectedContact.email}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                      <Phone className="w-5 h-5 text-gray-400" />
                      <div>
                        <div className="text-xs text-gray-600 mb-1">Phone</div>
                        <div className="text-sm text-gray-900">{selectedContact.phone}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                      <Building2 className="w-5 h-5 text-gray-400" />
                      <div>
                        <div className="text-xs text-gray-600 mb-1">Company</div>
                        <div className="text-sm text-gray-900">{selectedContact.company}</div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Activity Summary */}
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Activity</h3>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                      <div className="flex items-center gap-2 mb-2">
                        <MessageSquare className="w-5 h-5 text-blue-600" />
                        <div className="text-xs text-blue-600">Conversations</div>
                      </div>
                      <div className="text-2xl text-blue-900">{selectedContact.conversationCount}</div>
                    </div>
                    <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                      <div className="flex items-center gap-2 mb-2">
                        <Calendar className="w-5 h-5 text-gray-600" />
                        <div className="text-xs text-gray-600">Last Contact</div>
                      </div>
                      <div className="text-sm text-gray-900">{selectedContact.lastContact}</div>
                    </div>
                  </div>
                </div>

                {/* Notes */}
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Notes</h3>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    {editingNotes ? (
                      <div className="flex items-center gap-2">
                        <textarea
                          value={notesText}
                          onChange={(e) => setNotesText(e.target.value)}
                          className="w-full h-20 p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                        <button
                          onClick={() => {
                            setEditingNotes(false);
                            selectedContact.notes = notesText;
                          }}
                          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        >
                          <Save className="w-5 h-5 text-gray-600" />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <div className="text-sm text-gray-900">{selectedContact.notes}</div>
                        <button
                          onClick={() => {
                            setEditingNotes(true);
                            setNotesText(selectedContact.notes);
                          }}
                          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        >
                          <Edit2 className="w-5 h-5 text-gray-600" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {/* Custom Fields */}
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Custom Fields</h3>
                  <div className="space-y-3">
                    {Object.entries(selectedContact.customFields).map(([key, value]) => (
                      <div key={key} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                        <StickyNote className="w-5 h-5 text-gray-400" />
                        <div>
                          <div className="text-xs text-gray-600 mb-1">{key}</div>
                          <div className="text-sm text-gray-900">{value}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Empty State */}
        {!selectedContact && (selectedContact !== null) && (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <User className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-gray-600">Select a contact to view details</p>
            </div>
          </div>
        )}
      </div>

      {/* New Contact Modal */}
      {showNewContactModal && <NewContactModal onClose={() => setShowNewContactModal(false)} />}
    </div>
  );
}

function NewContactModal({ onClose }: { onClose: () => void }) {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    company: '',
    notes: '',
    jobTitle: '',
    department: ''
  });

  const handleSubmit = () => {
    // Here you would handle saving the contact
    console.log('New contact:', formData);
    onClose();
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="sticky top-0 bg-white flex items-center justify-between px-6 py-4 border-b border-gray-200 rounded-t-xl">
            <div>
              <h2 className="text-gray-900">Add New Contact</h2>
              <p className="text-sm text-gray-600 mt-1">
                Enter contact information
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-5 h-5 text-gray-600" />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-4 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Name *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="John Doe"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-700 mb-2">Email *</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="john@email.com"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Phone</label>
                <input
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  placeholder="+1 (555) 123-4567"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-700 mb-2">Company</label>
                <input
                  type="text"
                  value={formData.company}
                  onChange={(e) => setFormData({ ...formData, company: e.target.value })}
                  placeholder="Company Name"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm text-gray-700 mb-2">Notes</label>
              <textarea
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                placeholder="Add notes about this contact..."
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </div>

            <div className="border-t border-gray-200 pt-4">
              <h3 className="text-sm text-gray-700 mb-3">Custom Fields</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Job Title</label>
                  <input
                    type="text"
                    value={formData.jobTitle}
                    onChange={(e) => setFormData({ ...formData, jobTitle: e.target.value })}
                    placeholder="CEO, Manager, etc."
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Department</label>
                  <input
                    type="text"
                    value={formData.department}
                    onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                    placeholder="Sales, Marketing, etc."
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="sticky bottom-0 bg-gray-50 flex items-center justify-end gap-2 px-6 py-4 border-t border-gray-200 rounded-b-xl">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={!formData.name.trim() || !formData.email.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Add Contact
            </button>
          </div>
        </div>
      </div>
    </>
  );
}