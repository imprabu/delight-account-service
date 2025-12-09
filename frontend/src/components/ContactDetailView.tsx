import { useState } from 'react';
import { ChevronRight, Mail, Phone, Building2, Calendar, Clock, MessageSquare, TrendingUp, CheckCircle2, Star, Edit, ExternalLink } from 'lucide-react';
import { Pagination } from './Pagination';
import { ContactFormModal, EditAdditionalInfoModal, EditNotesModal, AllConversationsModal } from './ContactAccountModals';

interface ContactDetailViewProps {
  contact: any;
  accounts: any[];
  visibleFields: any;
  onBack: () => void;
  onEdit: (field: string) => void;
  onViewAllConversations: () => void;
  onNavigateToAccount: (accountId: number) => void;
}

export function ContactDetailView({
  contact,
  accounts,
  visibleFields,
  onBack,
  onEdit,
  onViewAllConversations,
  onNavigateToAccount,
}: ContactDetailViewProps) {
  const [accountsPage, setAccountsPage] = useState(1);
  const [accountsPerPage, setAccountsPerPage] = useState(5);
  const [activeModal, setActiveModal] = useState<string | null>(null);

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

  // Get associated accounts
  const associatedAccounts = accounts.filter(acc => 
    contact.accountId === acc.id || contact.accountIds?.includes(acc.id)
  );

  // Pagination for associated accounts
  const totalAccountPages = Math.ceil(associatedAccounts.length / accountsPerPage);
  const paginatedAccounts = associatedAccounts.slice(
    (accountsPage - 1) * accountsPerPage,
    accountsPage * accountsPerPage
  );

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
                onClick={() => setActiveModal('contact')}
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
          {(visibleFields.linkedin || visibleFields.timezone || visibleFields.preferredContact) && contact.customFields && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Additional Info</h3>
                <button 
                  onClick={() => setActiveModal('additionalInfo')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <div className="space-y-3 text-sm">
                {visibleFields.linkedin && contact.customFields.linkedin && (
                  <div>
                    <div className="text-gray-600 mb-1">LinkedIn</div>
                    <a href={`https://${contact.customFields.linkedin}`} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline flex items-center gap-1">
                      {contact.customFields.linkedin}
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}
                {visibleFields.timezone && contact.customFields.timezone && (
                  <div>
                    <div className="text-gray-600 mb-1">Timezone</div>
                    <div className="text-gray-900">{contact.customFields.timezone}</div>
                  </div>
                )}
                {visibleFields.preferredContact && contact.customFields.preferredContact && (
                  <div>
                    <div className="text-gray-600 mb-1">Preferred Contact</div>
                    <div className="text-gray-900">{contact.customFields.preferredContact}</div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Notes */}
          {visibleFields.notes && contact.notes && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Notes</h3>
                <button 
                  onClick={() => setActiveModal('notes')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <p className="text-sm text-gray-700">{contact.notes}</p>
            </div>
          )}

          {/* Associated Accounts */}
          {associatedAccounts.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200">
              <div className="p-4 border-b border-gray-200">
                <h3 className="text-gray-900">Associated Accounts</h3>
                <p className="text-xs text-gray-600 mt-1">{associatedAccounts.length} account{associatedAccounts.length !== 1 ? 's' : ''}</p>
              </div>
              <div className="divide-y divide-gray-200">
                {paginatedAccounts.map((account: any) => (
                  <button
                    key={account.id}
                    onClick={() => onNavigateToAccount(account.id)}
                    className="w-full flex items-center gap-3 p-4 hover:bg-gray-50 transition-colors text-left"
                  >
                    <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0">
                      <Building2 className="w-5 h-5 text-purple-700" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm text-gray-900 truncate">{account.name}</div>
                      <div className="text-xs text-gray-600">{account.industry}</div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-gray-400 flex-shrink-0" />
                  </button>
                ))}
              </div>
              {totalAccountPages > 1 && (
                <div className="border-t border-gray-200">
                  <Pagination
                    currentPage={accountsPage}
                    totalPages={totalAccountPages}
                    totalItems={associatedAccounts.length}
                    itemsPerPage={accountsPerPage}
                    onPageChange={setAccountsPage}
                    onItemsPerPageChange={setAccountsPerPage}
                  />
                </div>
              )}
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
                onClick={() => setActiveModal('allConversations')}
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

      {/* Modals */}
      {activeModal === 'contact' && (
        <ContactFormModal
          contact={contact}
          onClose={() => setActiveModal(null)}
        />
      )}
      {activeModal === 'additionalInfo' && (
        <EditAdditionalInfoModal
          contact={contact}
          onClose={() => setActiveModal(null)}
        />
      )}
      {activeModal === 'notes' && (
        <EditNotesModal
          contact={contact}
          onClose={() => setActiveModal(null)}
        />
      )}
      {activeModal === 'allConversations' && (
        <AllConversationsModal
          contact={contact}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  );
}