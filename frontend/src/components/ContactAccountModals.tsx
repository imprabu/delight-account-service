import { useState } from 'react';
import { X, CheckCircle2, AlertTriangle, Download, Upload, FileText, Settings as SettingsIcon } from 'lucide-react';

// Contact Form Modal
export function ContactFormModal({ title, contact, onSave, onClose }: any) {
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
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">{title}</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
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

          <div className="grid grid-cols-2 gap-4">
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

// Edit Additional Info Modal
export function EditAdditionalInfoModal({ contact, onSave, onClose }: any) {
  const [formData, setFormData] = useState({
    linkedin: contact?.customFields?.linkedin || '',
    timezone: contact?.customFields?.timezone || '',
    preferredContact: contact?.customFields?.preferredContact || 'Email',
  });

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">Edit Additional Info</h2>
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
            Save
          </button>
        </div>
      </div>
    </div>
  );
}

// Edit Notes Modal
export function EditNotesModal({ contact, account, onSave, onClose }: any) {
  const [notes, setNotes] = useState((contact?.notes || account?.notes) || '');

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">Edit Notes</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={8}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            placeholder="Add notes about this contact..."
          />
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onSave({ notes })}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            Save Notes
          </button>
        </div>
      </div>
    </div>
  );
}

// Account Form Modal
export function AccountFormModal({ title, account, onSave, onClose }: any) {
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
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">{title}</h2>
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

// All Conversations Modal
export function AllConversationsModal({ contact, onClose }: any) {
  const [filter, setFilter] = useState('all');

  const conversations = [
    {
      id: 1,
      subject: 'API Integration Question',
      preview: "Hi, I'm trying to integrate the webhook...",
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
      subject: 'Support Ticket: Login Issue',
      preview: "Can't seem to log in with my credentials...",
      date: '1 week ago',
      status: 'resolved',
      channel: 'phone',
    },
    {
      id: 5,
      subject: 'Product Feedback',
      preview: 'Really loving the new dashboard design...',
      date: '2 weeks ago',
      status: 'open',
      channel: 'chat',
    },
  ];

  const filteredConversations = filter === 'all' 
    ? conversations 
    : conversations.filter((c) => c.status === filter);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-gray-900 mb-1">All Conversations with {contact.name}</h2>
            <p className="text-sm text-gray-600">{filteredConversations.length} conversations</p>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All Conversations</option>
              <option value="open">Open</option>
              <option value="resolved">Resolved</option>
            </select>
            <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto divide-y divide-gray-200">
          {filteredConversations.map((conv) => (
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
    </div>
  );
}

// Export Data Modal
export function ExportDataModal({ viewMode, onExport, onClose }: any) {
  const [format, setFormat] = useState('csv');
  const [selectedFields, setSelectedFields] = useState<string[]>([
    'name',
    'email',
    'phone',
    'company',
    'role',
  ]);

  const availableFields = viewMode === 'contacts' 
    ? [
        { id: 'name', label: 'Name' },
        { id: 'email', label: 'Email' },
        { id: 'phone', label: 'Phone' },
        { id: 'company', label: 'Company' },
        { id: 'role', label: 'Role' },
        { id: 'tags', label: 'Tags' },
        { id: 'status', label: 'Status' },
        { id: 'lastContact', label: 'Last Contact' },
        { id: 'conversations', label: 'Conversations' },
      ]
    : [
        { id: 'name', label: 'Company Name' },
        { id: 'domain', label: 'Domain' },
        { id: 'industry', label: 'Industry' },
        { id: 'size', label: 'Company Size' },
        { id: 'location', label: 'Location' },
        { id: 'plan', label: 'Plan' },
        { id: 'mrr', label: 'MRR' },
        { id: 'status', label: 'Status' },
      ];

  const toggleField = (fieldId: string) => {
    if (selectedFields.includes(fieldId)) {
      setSelectedFields(selectedFields.filter(f => f !== fieldId));
    } else {
      setSelectedFields([...selectedFields, fieldId]);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-gray-900">Export {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</h2>
            <p className="text-sm text-gray-600 mt-1">Choose format and fields to export</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Format Selection */}
          <div>
            <label className="block text-sm text-gray-700 mb-3">Export Format</label>
            <div className="grid grid-cols-3 gap-3">
              <button
                onClick={() => setFormat('csv')}
                className={`p-4 border-2 rounded-lg transition-all ${
                  format === 'csv'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <FileText className="w-6 h-6 mx-auto mb-2 text-gray-700" />
                <div className="text-sm text-gray-900">CSV</div>
                <div className="text-xs text-gray-500 mt-1">Spreadsheet</div>
              </button>
              <button
                onClick={() => setFormat('json')}
                className={`p-4 border-2 rounded-lg transition-all ${
                  format === 'json'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <FileText className="w-6 h-6 mx-auto mb-2 text-gray-700" />
                <div className="text-sm text-gray-900">JSON</div>
                <div className="text-xs text-gray-500 mt-1">Developer</div>
              </button>
              <button
                onClick={() => setFormat('xlsx')}
                className={`p-4 border-2 rounded-lg transition-all ${
                  format === 'xlsx'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <FileText className="w-6 h-6 mx-auto mb-2 text-gray-700" />
                <div className="text-sm text-gray-900">XLSX</div>
                <div className="text-xs text-gray-500 mt-1">Excel</div>
              </button>
            </div>
          </div>

          {/* Field Selection */}
          <div>
            <label className="block text-sm text-gray-700 mb-3">Fields to Export</label>
            <div className="border border-gray-200 rounded-lg p-4 max-h-64 overflow-y-auto">
              <div className="space-y-2">
                {availableFields.map((field) => (
                  <label key={field.id} className="flex items-center gap-3 p-2 hover:bg-gray-50 rounded cursor-pointer">
                    <input
                      type="checkbox"
                      checked={selectedFields.includes(field.id)}
                      onChange={() => toggleField(field.id)}
                      className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    />
                    <span className="text-sm text-gray-900">{field.label}</span>
                  </label>
                ))}
              </div>
            </div>
            <p className="text-xs text-gray-500 mt-2">{selectedFields.length} fields selected</p>
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
            disabled={selectedFields.length === 0}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-4 h-4" />
            Export {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}
          </button>
        </div>
      </div>
    </div>
  );
}

// Import Data Modal
export function ImportDataModal({ viewMode, onImport, onClose }: any) {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-gray-900">Import {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</h2>
            <p className="text-sm text-gray-600 mt-1">Upload a CSV, JSON, or XLSX file</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          {/* File Upload Area */}
          <div
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
              dragActive
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-300 hover:border-gray-400'
            }`}
          >
            {selectedFile ? (
              <div>
                <FileText className="w-12 h-12 mx-auto mb-3 text-blue-600" />
                <p className="text-gray-900 mb-1">{selectedFile.name}</p>
                <p className="text-sm text-gray-500 mb-4">
                  {(selectedFile.size / 1024).toFixed(2)} KB
                </p>
                <button
                  onClick={() => setSelectedFile(null)}
                  className="text-sm text-blue-600 hover:underline"
                >
                  Choose different file
                </button>
              </div>
            ) : (
              <div>
                <Upload className="w-12 h-12 mx-auto mb-3 text-gray-400" />
                <p className="text-gray-900 mb-2">
                  Drag and drop your file here, or{' '}
                  <label className="text-blue-600 hover:underline cursor-pointer">
                    browse
                    <input
                      type="file"
                      className="hidden"
                      accept=".csv,.json,.xlsx,.xls"
                      onChange={handleFileChange}
                    />
                  </label>
                </p>
                <p className="text-sm text-gray-500">
                  Supports CSV, JSON, and XLSX files
                </p>
              </div>
            )}
          </div>

          {/* Instructions */}
          <div className="mt-6 p-4 bg-blue-50 rounded-lg">
            <h3 className="text-sm text-gray-900 mb-2">Import Guidelines</h3>
            <ul className="text-xs text-gray-600 space-y-1">
              <li>• Ensure your file has headers matching the field names</li>
              <li>• Email is required for contacts, domain is required for accounts</li>
              <li>• Duplicate entries will be skipped or updated based on email/domain</li>
              <li>• Maximum file size: 10MB</li>
            </ul>
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
            onClick={() => onImport(selectedFile)}
            disabled={!selectedFile}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Upload className="w-4 h-4" />
            Import {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}
          </button>
        </div>
      </div>
    </div>
  );
}

// Settings Modal
export function SettingsModal({ viewMode, onSave, onClose }: any) {
  const [settings, setSettings] = useState({
    defaultView: 'all',
    itemsPerPage: 10,
    autoRefresh: true,
    emailNotifications: true,
    showInactiveContacts: true,
    compactMode: false,
  });

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-gray-900">{viewMode === 'contacts' ? 'Contacts' : 'Accounts'} Settings</h2>
            <p className="text-sm text-gray-600 mt-1">Customize your preferences</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Display Settings */}
          <div>
            <h3 className="text-gray-900 mb-4 flex items-center gap-2">
              <SettingsIcon className="w-5 h-5" />
              Display Settings
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Default View</label>
                <select
                  value={settings.defaultView}
                  onChange={(e) => setSettings({ ...settings, defaultView: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="all">All {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</option>
                  <option value="active">Active Only</option>
                  <option value="vip">VIP Only</option>
                  <option value="recent">Recent Activity</option>
                </select>
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">Items per Page</label>
                <select
                  value={settings.itemsPerPage}
                  onChange={(e) => setSettings({ ...settings, itemsPerPage: Number(e.target.value) })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value={5}>5 items</option>
                  <option value={10}>10 items</option>
                  <option value={25}>25 items</option>
                  <option value={50}>50 items</option>
                  <option value={100}>100 items</option>
                </select>
              </div>

              <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                <div>
                  <div className="text-sm text-gray-900">Compact Mode</div>
                  <div className="text-xs text-gray-500">Show more items with reduced spacing</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.compactMode}
                  onChange={(e) => setSettings({ ...settings, compactMode: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
              </label>

              <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                <div>
                  <div className="text-sm text-gray-900">Show Inactive {viewMode === 'contacts' ? 'Contacts' : 'Accounts'}</div>
                  <div className="text-xs text-gray-500">Include inactive items in lists</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.showInactiveContacts}
                  onChange={(e) => setSettings({ ...settings, showInactiveContacts: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
              </label>
            </div>
          </div>

          {/* Notification Settings */}
          <div className="pt-6 border-t border-gray-200">
            <h3 className="text-gray-900 mb-4">Notifications</h3>
            <div className="space-y-3">
              <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                <div>
                  <div className="text-sm text-gray-900">Auto Refresh</div>
                  <div className="text-xs text-gray-500">Automatically refresh data every 30 seconds</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.autoRefresh}
                  onChange={(e) => setSettings({ ...settings, autoRefresh: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
              </label>

              <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                <div>
                  <div className="text-sm text-gray-900">Email Notifications</div>
                  <div className="text-xs text-gray-500">Get notified about new conversations</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.emailNotifications}
                  onChange={(e) => setSettings({ ...settings, emailNotifications: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
              </label>
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
            onClick={() => onSave(settings)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" />
            Save Settings
          </button>
        </div>
      </div>
    </div>
  );
}