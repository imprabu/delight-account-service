import { useState } from 'react';
import { Building2, ChevronRight, Edit, DollarSign, Users, MessageSquare, Calendar, Clock, TrendingUp, CheckCircle2, Star, ExternalLink, Globe, MapPin, Briefcase, Tag, Phone, Mail, UserPlus, FileText, Activity } from 'lucide-react';
import { Pagination } from './Pagination';
import { AccountFormModal, EditNotesModal } from './ContactAccountModals';

interface AccountDetailViewProps {
  account: any;
  contacts: any[];
  onClose: () => void;
  onNavigateToContact: (contact: any) => void;
  onEdit: () => void;
}

export function AccountDetailView({
  account,
  contacts,
  onClose,
  onNavigateToContact,
  onEdit,
}: AccountDetailViewProps) {
  const [contactsPage, setContactsPage] = useState(1);
  const [contactsPerPage, setContactsPerPage] = useState(5);
  const [activeModal, setActiveModal] = useState<string | null>(null);

  // Get contacts for this account
  const accountContacts = contacts.filter((c: any) => c.accountId === account.id);

  // Pagination for contacts
  const totalContactPages = Math.ceil(accountContacts.length / contactsPerPage);
  const paginatedContacts = accountContacts.slice(
    (contactsPage - 1) * contactsPerPage,
    contactsPage * contactsPerPage
  );

  // Recent conversations for this account
  const recentConversations = [
    {
      id: 1,
      subject: 'Contract Renewal Discussion',
      preview: 'Discussing terms for the upcoming renewal...',
      date: '2 hours ago',
      status: 'open',
      contactName: accountContacts[0]?.name || 'Unknown',
    },
    {
      id: 2,
      subject: 'Support Ticket: Integration Issue',
      preview: 'Having trouble with API authentication...',
      date: '1 day ago',
      status: 'resolved',
      contactName: accountContacts[1]?.name || 'Unknown',
    },
    {
      id: 3,
      subject: 'Feature Request',
      preview: 'Would like to see custom reporting features...',
      date: '3 days ago',
      status: 'open',
      contactName: accountContacts[0]?.name || 'Unknown',
    },
  ];

  // Activity timeline
  const activityTimeline = [
    {
      id: 1,
      type: 'conversation',
      icon: MessageSquare,
      color: 'blue',
      description: 'New conversation started about contract renewal',
      time: '2 hours ago',
    },
    {
      id: 2,
      type: 'resolved',
      icon: CheckCircle2,
      color: 'green',
      description: 'Support ticket resolved: Integration issue',
      time: '1 day ago',
    },
    {
      id: 3,
      type: 'contact',
      icon: UserPlus,
      color: 'purple',
      description: 'New contact added to account',
      time: '2 days ago',
    },
    {
      id: 4,
      type: 'note',
      icon: FileText,
      color: 'orange',
      description: 'Account notes updated',
      time: '3 days ago',
    },
    {
      id: 5,
      type: 'tag',
      icon: Tag,
      color: 'pink',
      description: 'Tagged as VIP customer',
      time: '1 week ago',
    },
  ];

  return (
    <div>
      <button
        onClick={onClose}
        className="mb-4 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
      >
        <ChevronRight className="w-4 h-4 rotate-180" />
        Back to Accounts
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Account Info Sidebar */}
        <div className="lg:col-span-1 space-y-6">
          {/* Main Account Card */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-start justify-between mb-6">
              <div className="w-16 h-16 bg-purple-100 rounded-lg flex items-center justify-center">
                <Building2 className="w-8 h-8 text-purple-600" />
              </div>
              <button
                onClick={onEdit}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <Edit className="w-4 h-4 text-gray-600" />
              </button>
            </div>

            <h2 className="text-gray-900 mb-1">{account.name}</h2>
            <p className="text-sm text-gray-600 mb-4">{account.industry} • {account.size}</p>

            {/* Tags */}
            {account.tags && account.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 mb-6">
                {account.tags.map((tag: string, idx: number) => (
                  <span
                    key={idx}
                    className={`px-2 py-0.5 rounded text-xs ${
                      tag === 'VIP' ? 'bg-purple-100 text-purple-700' :
                      tag === 'Enterprise' ? 'bg-blue-100 text-blue-700' :
                      tag === 'Trial' ? 'bg-yellow-100 text-yellow-700' :
                      tag === 'Hot Lead' ? 'bg-red-100 text-red-700' :
                      'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}

            {/* Status Badge */}
            <div className="mb-6">
              <span className={`px-3 py-1 rounded-full text-xs ${
                account.status === 'active' ? 'bg-green-100 text-green-700' :
                account.status === 'trial' ? 'bg-yellow-100 text-yellow-700' :
                'bg-gray-100 text-gray-700'
              }`}>
                {account.status.charAt(0).toUpperCase() + account.status.slice(1)}
              </span>
            </div>

            {/* Key Details */}
            <div className="space-y-3">
              <div className="flex items-center gap-3 text-sm">
                <MapPin className="w-4 h-4 text-gray-400" />
                <span className="text-gray-900">{account.location}</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <Briefcase className="w-4 h-4 text-gray-400" />
                <span className="text-gray-900">{account.plan}</span>
              </div>
              {account.domain && (
                <div className="flex items-center gap-3 text-sm">
                  <Globe className="w-4 h-4 text-gray-400" />
                  <a
                    href={`https://${account.domain}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline flex items-center gap-1"
                  >
                    {account.domain}
                    <ExternalLink className="w-3 h-3" />
                  </a>
                </div>
              )}
              <div className="flex items-center gap-3 text-sm">
                <Calendar className="w-4 h-4 text-gray-400" />
                <span className="text-gray-600">Created {account.createdAt}</span>
              </div>
            </div>
          </div>

          {/* Account Details */}
          {account.customFields && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Account Details</h3>
                <button
                  onClick={onEdit}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <div className="space-y-3 text-sm">
                {account.customFields.website && (
                  <div>
                    <div className="text-gray-600 mb-1">Website</div>
                    <a
                      href={account.customFields.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:underline flex items-center gap-1"
                    >
                      {account.customFields.website}
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}
                {account.customFields.accountOwner && (
                  <div>
                    <div className="text-gray-600 mb-1">Account Owner</div>
                    <div className="text-gray-900">{account.customFields.accountOwner}</div>
                  </div>
                )}
                {account.customFields.contractStartDate && (
                  <div>
                    <div className="text-gray-600 mb-1">Contract Start</div>
                    <div className="text-gray-900">{account.customFields.contractStartDate}</div>
                  </div>
                )}
                {account.customFields.contractEndDate && (
                  <div>
                    <div className="text-gray-600 mb-1">Contract End</div>
                    <div className="text-gray-900">{account.customFields.contractEndDate}</div>
                  </div>
                )}
                {account.customFields.annualValue && (
                  <div>
                    <div className="text-gray-600 mb-1">Annual Contract Value</div>
                    <div className="text-gray-900">${account.customFields.annualValue.toLocaleString()}</div>
                  </div>
                )}
                {account.customFields.trialStartDate && (
                  <div>
                    <div className="text-gray-600 mb-1">Trial Start</div>
                    <div className="text-gray-900">{account.customFields.trialStartDate}</div>
                  </div>
                )}
                {account.customFields.trialEndDate && (
                  <div>
                    <div className="text-gray-600 mb-1">Trial End</div>
                    <div className="text-gray-900">{account.customFields.trialEndDate}</div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Notes */}
          {account.notes && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-gray-900">Notes</h3>
                <button
                  onClick={() => setActiveModal('editNotes')}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <Edit className="w-4 h-4 text-gray-600" />
                </button>
              </div>
              <p className="text-sm text-gray-700">{account.notes}</p>
            </div>
          )}

          {/* Contacts List */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-4 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-gray-900">Contacts</h3>
                <span className="text-sm text-gray-600">{accountContacts.length} contact{accountContacts.length !== 1 ? 's' : ''}</span>
              </div>
            </div>
            
            {accountContacts.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <Users className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                <p>No contacts found for this account</p>
              </div>
            ) : (
              <>
                <div className="divide-y divide-gray-200">
                  {paginatedContacts.map((contact: any) => (
                    <button
                      key={contact.id}
                      onClick={() => onNavigateToContact(contact)}
                      className="w-full flex items-center gap-3 p-4 hover:bg-gray-50 transition-colors text-left"
                    >
                      <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 text-sm">
                        {contact.name.split(' ').map((n: string) => n[0]).join('')}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm text-gray-900 truncate">{contact.name}</div>
                        <div className="text-xs text-gray-600">{contact.role}</div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-gray-400 flex-shrink-0" />
                    </button>
                  ))}
                </div>

                {totalContactPages > 1 && (
                  <div className="border-t border-gray-200">
                    <Pagination
                      currentPage={contactsPage}
                      totalPages={totalContactPages}
                      totalItems={accountContacts.length}
                      itemsPerPage={contactsPerPage}
                      onPageChange={setContactsPage}
                      onItemsPerPageChange={setContactsPerPage}
                    />
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        {/* Main Content Area */}
        <div className="lg:col-span-2 space-y-6">
          {/* Stats Cards */}
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <DollarSign className="w-5 h-5 text-blue-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">${account.mrr.toLocaleString()}</div>
              <div className="text-sm text-gray-600">Monthly Revenue</div>
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <Users className="w-5 h-5 text-green-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">{accountContacts.length}</div>
              <div className="text-sm text-gray-600">Contacts</div>
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-2">
                <MessageSquare className="w-5 h-5 text-purple-600" />
              </div>
              <div className="text-2xl text-gray-900 mb-1">{account.conversations || 0}</div>
              <div className="text-sm text-gray-600">Conversations</div>
            </div>
          </div>

          {/* Recent Conversations */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="p-6 border-b border-gray-200 flex items-center justify-between">
              <h3 className="text-gray-900">Recent Conversations</h3>
              <button className="text-sm text-blue-600 hover:underline">
                View All
              </button>
            </div>
            <div className="divide-y divide-gray-200">
              {recentConversations.map((conv) => (
                <div key={conv.id} className="p-6 hover:bg-gray-50 cursor-pointer transition-colors">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="text-gray-900 mb-1">{conv.subject}</div>
                      <p className="text-sm text-gray-600 mb-1">{conv.preview}</p>
                      <div className="text-xs text-gray-500">
                        <span className="text-blue-600">{conv.contactName}</span> • {conv.date}
                      </div>
                    </div>
                    <span className={`ml-4 px-2 py-1 rounded-full text-xs flex-shrink-0 ${
                      conv.status === 'resolved' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>
                      {conv.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Activity Timeline */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="text-gray-900 mb-4">Activity Timeline</h3>
            <div className="space-y-4">
              {activityTimeline.map((activity) => {
                const Icon = activity.icon;
                return (
                  <div key={activity.id} className="flex gap-3">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                      activity.color === 'blue' ? 'bg-blue-100' :
                      activity.color === 'green' ? 'bg-green-100' :
                      activity.color === 'purple' ? 'bg-purple-100' :
                      activity.color === 'orange' ? 'bg-orange-100' :
                      activity.color === 'pink' ? 'bg-pink-100' :
                      'bg-gray-100'
                    }`}>
                      <Icon className={`w-4 h-4 ${
                        activity.color === 'blue' ? 'text-blue-600' :
                        activity.color === 'green' ? 'text-green-600' :
                        activity.color === 'purple' ? 'text-purple-600' :
                        activity.color === 'orange' ? 'text-orange-600' :
                        activity.color === 'pink' ? 'text-pink-600' :
                        'text-gray-600'
                      }`} />
                    </div>
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">{activity.description}</div>
                      <div className="text-xs text-gray-500">{activity.time}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Modals */}
      {activeModal === 'editNotes' && (
        <EditNotesModal
          account={account}
          onClose={() => setActiveModal(null)}
        />
      )}
      {activeModal === 'editAccount' && (
        <AccountFormModal
          account={account}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  );
}