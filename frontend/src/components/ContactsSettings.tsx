import { useState } from 'react';
import { Eye, Tag, Download, Upload, Bell, Shield, Sparkles, Check, Database, Layout, User, Mail, Phone, Briefcase, Globe, Clock, MessageSquare, X, Plus, Sliders } from 'lucide-react';
import { CustomFieldsSettings } from './CustomFieldsSettings';

type SettingsTab = 'display' | 'ai' | 'communication' | 'tags' | 'notifications' | 'privacy' | 'customFields';

export function ContactsSettings() {
  const [activeTab, setActiveTab] = useState<SettingsTab>('display');
  const [enableLLMNotes, setEnableLLMNotes] = useState(true);
  const [autoExportEnabled, setAutoExportEnabled] = useState(false);
  const [exportFormat, setExportFormat] = useState('csv');
  const [dataRetention, setDataRetention] = useState('forever');
  const [accessControl, setAccessControl] = useState('all');
  const [communicationSettings, setCommunicationSettings] = useState({
    autoLogEmails: true,
    autoLogCalls: true,
    trackEmailOpens: false,
    requireCallNotes: true,
  });
  const [notificationSettings, setNotificationSettings] = useState({
    newContact: true,
    statusChange: true,
    tagUpdate: false,
    missedFollowUp: true,
    birthdayReminder: true,
  });
  
  // Initialize visible fields state
  const [visibleFields, setVisibleFields] = useState({
    email: true,
    phone: true,
    role: true,
    company: true,
    accountLink: true,
    contactOwner: true,
    linkedin: false,
    twitter: false,
    website: false,
    timezone: true,
    preferredContact: true,
    language: false,
    tags: true,
    notes: true,
    customFields: true,
  });

  const tabs = [
    { id: 'display', label: 'Display & Fields', icon: Layout },
    { id: 'customFields', label: 'Custom Fields', icon: Sliders },
    { id: 'ai', label: 'AI Features', icon: Sparkles },
    { id: 'communication', label: 'Communication', icon: MessageSquare },
    { id: 'tags', label: 'Tags', icon: Tag },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'privacy', label: 'Privacy & Data', icon: Shield },
  ];

  const contactFieldGroups = {
    'Basic Information': [
      { key: 'email', label: 'Email Address', description: 'Primary email with click-to-email', icon: Mail },
      { key: 'phone', label: 'Phone Number', description: 'Phone with click-to-call functionality', icon: Phone },
      { key: 'role', label: 'Job Title/Role', description: 'Contact position and responsibilities', icon: Briefcase },
    ],
    'Company & Relationship': [
      { key: 'company', label: 'Company Name', description: 'Associated company with quick navigation', icon: Briefcase },
      { key: 'accountLink', label: 'Account Link', description: 'Link to parent account details', icon: Briefcase },
      { key: 'contactOwner', label: 'Contact Owner', description: 'Team member responsible for this contact', icon: User },
    ],
    'Social & Professional': [
      { key: 'linkedin', label: 'LinkedIn Profile', description: 'Social media and professional network links', icon: Globe },
      { key: 'twitter', label: 'Twitter/X Handle', description: 'Social media presence', icon: Globe },
      { key: 'website', label: 'Personal Website', description: 'Blog or portfolio URL', icon: Globe },
    ],
    'Preferences & Context': [
      { key: 'timezone', label: 'Timezone', description: 'For scheduling and optimal contact times', icon: Clock },
      { key: 'preferredContact', label: 'Preferred Contact Method', description: 'How they like to be reached', icon: MessageSquare },
      { key: 'language', label: 'Preferred Language', description: 'Communication language preference', icon: Globe },
    ],
    'Organization': [
      { key: 'tags', label: 'Tags & Labels', description: 'Contact categorization and segments', icon: Tag },
      { key: 'notes', label: 'Notes Section', description: 'Internal notes and observations', icon: MessageSquare },
      { key: 'customFields', label: 'Custom Fields', description: 'Additional custom data fields', icon: Layout },
    ],
  };

  const contactTags = ['VIP', 'Decision Maker', 'Technical Contact', 'Billing Contact', 'Hot Lead', 'Cold Lead', 'Champion', 'Influencer', 'Blocker', 'Supporter'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-gray-900 mb-2">Contact Settings</h1>
          <p className="text-gray-600">Configure how contacts are displayed and managed</p>
        </div>
        <button
          onClick={() => { /* Add your close logic here */ }}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
        >
          <Check className="w-4 h-4" />
          Done
        </button>
      </div>

      {/* Tabs Navigation */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="border-b border-gray-200 bg-gray-50">
          <div className="flex overflow-x-auto">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as SettingsTab)}
                  className={`flex items-center gap-2 px-6 py-4 border-b-2 transition-all whitespace-nowrap ${
                    activeTab === tab.id
                      ? 'border-blue-600 bg-white text-blue-600'
                      : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="text-sm">{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Tab Content */}
        <div className="p-8">
          {/* Display & Fields Tab */}
          {activeTab === 'display' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Contact Field Visibility</h2>
                <p className="text-sm text-gray-600">Choose which fields appear in contact detail pages</p>
              </div>

              <div className="space-y-6">
                {Object.entries(contactFieldGroups).map(([groupName, fields]) => (
                  <div key={groupName}>
                    <h3 className="text-sm text-gray-700 mb-3 pb-2 border-b border-gray-200 flex items-center gap-2">
                      <span>{groupName}</span>
                      <span className="text-xs text-gray-500">({fields.length} fields)</span>
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {fields.map(field => {
                        const FieldIcon = field.icon;
                        return (
                          <label 
                            key={field.key} 
                            className={`flex items-start gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                              visibleFields[field.key]
                                ? 'border-green-500 bg-green-50'
                                : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                            }`}
                          >
                            <input
                              type="checkbox"
                              checked={visibleFields[field.key]}
                              onChange={(e) => setVisibleFields({
                                ...visibleFields,
                                [field.key]: e.target.checked
                              })}
                              className="w-5 h-5 mt-0.5 flex-shrink-0"
                            />
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 mb-1">
                                <FieldIcon className="w-4 h-4 text-gray-600" />
                                <span className="text-sm text-gray-900">{field.label}</span>
                              </div>
                              <div className="text-xs text-gray-600">{field.description}</div>
                            </div>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-gray-200">
                <button
                  onClick={() => {
                    const allEnabled = Object.keys(visibleFields).reduce((acc, key) => ({ ...acc, [key]: true }), {});
                    setVisibleFields(allEnabled);
                  }}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm"
                >
                  Show All Fields
                </button>
                <button
                  onClick={() => {
                    const allDisabled = Object.keys(visibleFields).reduce((acc, key) => ({ ...acc, [key]: false }), {});
                    setVisibleFields(allDisabled);
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                >
                  Hide All Fields
                </button>
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="text-sm text-blue-800">
                  <strong>Note:</strong> Hidden fields are not displayed in contact details but remain in the database and can be exported.
                </div>
              </div>
            </div>
          )}

          {/* AI Features Tab */}
          {activeTab === 'ai' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">AI-Powered Contact Features</h2>
                <p className="text-sm text-gray-600">Enable intelligent assistance for managing contacts</p>
              </div>

              {/* AI Note Assistant */}
              <div className="border-2 border-purple-200 rounded-lg p-6 bg-purple-50">
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 bg-purple-600 rounded-xl flex items-center justify-center flex-shrink-0">
                    <Sparkles className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-gray-900">AI Note Assistant</h3>
                      <span className="px-2 py-0.5 bg-purple-600 text-white rounded text-xs uppercase tracking-wide">Recommended</span>
                    </div>
                    <p className="text-sm text-gray-700 mb-4">
                      Let AI help you write, improve, and organize notes about your contacts
                    </p>

                    <div className="bg-white rounded-lg p-4 mb-4">
                      <div className="text-sm text-gray-900 mb-3">Capabilities for Contact Notes:</div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Generate professional meeting summaries</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Extract action items from conversations</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Summarize long interaction histories</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Improve clarity and professionalism</span>
                        </div>
                      </div>
                    </div>

                    <label className="flex items-center gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={enableLLMNotes}
                        onChange={(e) => setEnableLLMNotes(e.target.checked)}
                        className="w-5 h-5"
                      />
                      <span className="text-sm text-gray-900">Enable AI Note Assistant for Contacts</span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Future AI Features */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="border border-gray-200 rounded-lg p-4 bg-gray-50 opacity-60">
                  <div className="flex items-center gap-2 mb-2">
                    <User className="w-4 h-4 text-gray-500" />
                    <h4 className="text-sm text-gray-900">Contact Insights</h4>
                    <span className="px-2 py-0.5 bg-gray-200 text-gray-600 rounded text-xs">Coming Soon</span>
                  </div>
                  <p className="text-xs text-gray-600">AI-powered personality and communication style analysis</p>
                </div>

                <div className="border border-gray-200 rounded-lg p-4 bg-gray-50 opacity-60">
                  <div className="flex items-center gap-2 mb-2">
                    <MessageSquare className="w-4 h-4 text-gray-500" />
                    <h4 className="text-sm text-gray-900">Smart Follow-ups</h4>
                    <span className="px-2 py-0.5 bg-gray-200 text-gray-600 rounded text-xs">Coming Soon</span>
                  </div>
                  <p className="text-xs text-gray-600">Automatic reminders based on conversation context</p>
                </div>
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
                <Shield className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-blue-800">
                  <strong>Privacy:</strong> AI features use your configured DelightChat LLM. Contact data is processed securely and never used for training external models.
                </div>
              </div>
            </div>
          )}

          {/* Communication Tab */}
          {activeTab === 'communication' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Communication Settings</h2>
                <p className="text-sm text-gray-600">Configure how communications with contacts are tracked and logged</p>
              </div>

              {/* Email Settings */}
              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Mail className="w-5 h-5 text-blue-600" />
                  <h3 className="text-gray-900">Email Settings</h3>
                </div>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Auto-log Email Conversations</div>
                      <div className="text-xs text-gray-600">Automatically record email interactions in contact timeline</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={communicationSettings.autoLogEmails}
                      onChange={(e) => setCommunicationSettings({...communicationSettings, autoLogEmails: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Track Email Opens</div>
                      <div className="text-xs text-gray-600">Get notified when contacts open your emails</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={communicationSettings.trackEmailOpens}
                      onChange={(e) => setCommunicationSettings({...communicationSettings, trackEmailOpens: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>

              {/* Call Settings */}
              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Phone className="w-5 h-5 text-green-600" />
                  <h3 className="text-gray-900">Call Settings</h3>
                </div>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Auto-log Phone Calls</div>
                      <div className="text-xs text-gray-600">Automatically record call interactions in contact timeline</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={communicationSettings.autoLogCalls}
                      onChange={(e) => setCommunicationSettings({...communicationSettings, autoLogCalls: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Require Call Notes</div>
                      <div className="text-xs text-gray-600">Prompt to add notes after every call</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={communicationSettings.requireCallNotes}
                      onChange={(e) => setCommunicationSettings({...communicationSettings, requireCallNotes: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>

              {/* Data Export */}
              <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
                <h3 className="text-gray-900 mb-4">Export Communication History</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm text-gray-700 mb-2">Export Format</label>
                    <select 
                      value={exportFormat}
                      onChange={(e) => setExportFormat(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                    >
                      <option value="csv">CSV (.csv)</option>
                      <option value="excel">Excel (.xlsx)</option>
                      <option value="json">JSON (.json)</option>
                    </select>
                  </div>
                  <div className="flex items-end">
                    <button className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2">
                      <Download className="w-4 h-4" />
                      Export All Contacts
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Tags Tab */}
          {activeTab === 'tags' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Contact Tags</h2>
                <p className="text-sm text-gray-600">Organize and categorize your contacts with custom tags</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-gray-900">Available Contact Tags ({contactTags.length})</h3>
                  <button className="px-3 py-1.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm flex items-center gap-2">
                    <Plus className="w-4 h-4" />
                    Add Tag
                  </button>
                </div>

                <div className="flex flex-wrap gap-2 mb-4">
                  {contactTags.map(tag => (
                    <div
                      key={tag}
                      className="px-3 py-1.5 bg-blue-100 text-blue-700 rounded-lg flex items-center gap-2 group hover:bg-blue-200 transition-colors"
                    >
                      <span className="text-sm">{tag}</span>
                      <button className="opacity-0 group-hover:opacity-100 hover:text-red-600 transition-all">
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
                <h3 className="text-gray-900 mb-4">Tag Usage by Contact Type</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">Decision Maker</span>
                    <span className="text-gray-600">24 contacts</span>
                  </div>
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">Technical Contact</span>
                    <span className="text-gray-600">18 contacts</span>
                  </div>
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">VIP</span>
                    <span className="text-gray-600">12 contacts</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Contact Notifications</h2>
                <p className="text-sm text-gray-600">Choose what you want to be notified about</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <h3 className="text-gray-900 mb-4">Contact Activity</h3>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">New Contact Created</div>
                      <div className="text-xs text-gray-600">When a new contact is added to the system</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.newContact}
                      onChange={(e) => setNotificationSettings({...notificationSettings, newContact: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Contact Status Changes</div>
                      <div className="text-xs text-gray-600">When contact status is updated (active/inactive)</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.statusChange}
                      onChange={(e) => setNotificationSettings({...notificationSettings, statusChange: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Tag Updates</div>
                      <div className="text-xs text-gray-600">When tags are added or removed from contacts</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.tagUpdate}
                      onChange={(e) => setNotificationSettings({...notificationSettings, tagUpdate: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <h3 className="text-gray-900 mb-4">Reminders</h3>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Missed Follow-ups</div>
                      <div className="text-xs text-gray-600">Remind me when I haven't followed up with a contact</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.missedFollowUp}
                      onChange={(e) => setNotificationSettings({...notificationSettings, missedFollowUp: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Birthday Reminders</div>
                      <div className="text-xs text-gray-600">Get notified about contact birthdays</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.birthdayReminder}
                      onChange={(e) => setNotificationSettings({...notificationSettings, birthdayReminder: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>
            </div>
          )}

          {/* Privacy & Data Tab */}
          {activeTab === 'privacy' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Privacy & Data Management</h2>
                <p className="text-sm text-gray-600">Control contact data access, retention, and security</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Shield className="w-5 h-5 text-red-600" />
                  <h3 className="text-gray-900">Access Control</h3>
                </div>
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Who can view contact details?</label>
                  <select 
                    value={accessControl}
                    onChange={(e) => setAccessControl(e.target.value)}
                    className="w-full md:w-96 px-4 py-2 border border-gray-300 rounded-lg"
                  >
                    <option value="all">All team members</option>
                    <option value="assigned">Contact owner only</option>
                    <option value="team">Contact owner's team</option>
                    <option value="admin">Administrators only</option>
                  </select>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Database className="w-5 h-5 text-blue-600" />
                  <h3 className="text-gray-900">Data Retention</h3>
                </div>
                <div>
                  <label className="block text-sm text-gray-700 mb-2">How long to keep contact data after last interaction?</label>
                  <select 
                    value={dataRetention}
                    onChange={(e) => setDataRetention(e.target.value)}
                    className="w-full md:w-96 px-4 py-2 border border-gray-300 rounded-lg"
                  >
                    <option value="forever">Keep forever</option>
                    <option value="1year">1 year</option>
                    <option value="2years">2 years</option>
                    <option value="5years">5 years</option>
                    <option value="7years">7 years (compliance)</option>
                  </select>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
                <h3 className="text-gray-900 mb-4">Compliance & Audit</h3>
                <div className="space-y-2">
                  <button className="w-full px-4 py-3 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 text-sm text-left flex items-center justify-between">
                    <span>View Contact Audit Log</span>
                    <span className="text-xs text-gray-500">Track all changes</span>
                  </button>
                  <button className="w-full px-4 py-3 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 text-sm text-left flex items-center justify-between">
                    <span>Export GDPR Report</span>
                    <span className="text-xs text-gray-500">Compliance ready</span>
                  </button>
                </div>
              </div>

              <label className="flex items-start gap-3 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={autoExportEnabled}
                  onChange={(e) => setAutoExportEnabled(e.target.checked)}
                  className="w-5 h-5 mt-0.5"
                />
                <div className="flex-1">
                  <div className="text-sm text-gray-900 mb-1">Weekly Automatic Backup</div>
                  <div className="text-xs text-gray-600">Export all contact data every Sunday for backup purposes</div>
                </div>
              </label>
            </div>
          )}

          {/* Custom Fields Tab */}
          {activeTab === 'customFields' && (
            <CustomFieldsSettings entityType="contact" />
          )}
        </div>
      </div>
    </div>
  );
}

function Plus({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
    </svg>
  );
}