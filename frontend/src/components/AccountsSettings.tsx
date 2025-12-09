import { useState } from 'react';
import { Eye, Tag, Download, Upload, Bell, Shield, Sparkles, Check, Database, Layout, Building2, DollarSign, TrendingUp, Calendar, Users, Globe, X, Plus, Sliders } from 'lucide-react';
import { CustomFieldsSettings } from './CustomFieldsSettings';

type SettingsTab = 'display' | 'ai' | 'business' | 'tags' | 'notifications' | 'privacy' | 'customFields';

export function AccountsSettings() {
  const [activeTab, setActiveTab] = useState<SettingsTab>('display');
  const [enableLLMNotes, setEnableLLMNotes] = useState(true);
  const [autoExportEnabled, setAutoExportEnabled] = useState(false);
  const [exportFormat, setExportFormat] = useState('csv');
  const [dataRetention, setDataRetention] = useState('forever');
  const [accessControl, setAccessControl] = useState('all');
  const [businessSettings, setBusinessSettings] = useState({
    trackRevenue: true,
    contractReminders: true,
    healthScoreEnabled: true,
    autoCalculateMRR: true,
  });
  const [notificationSettings, setNotificationSettings] = useState({
    newAccount: true,
    statusChange: true,
    contractExpiring: true,
    revenueThreshold: true,
    healthScoreChange: false,
  });
  
  // Initialize visible fields state
  const [visibleFields, setVisibleFields] = useState({
    accountName: true,
    domain: true,
    industry: true,
    accountOwner: true,
    plan: true,
    mrr: true,
    arr: true,
    healthScore: true,
    contractStart: true,
    contractEnd: true,
    renewalDate: true,
    billingCycle: true,
    contacts: true,
    conversations: true,
    accountAge: true,
    tags: true,
    notes: true,
    customFields: true,
  });

  const tabs = [
    { id: 'display', label: 'Display & Fields', icon: Layout },
    { id: 'customFields', label: 'Custom Fields', icon: Sliders },
    { id: 'ai', label: 'AI Features', icon: Sparkles },
    { id: 'business', label: 'Business Metrics', icon: TrendingUp },
    { id: 'tags', label: 'Tags', icon: Tag },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'privacy', label: 'Privacy & Data', icon: Shield },
  ];

  const accountFieldGroups = {
    'Basic Information': [
      { key: 'accountName', label: 'Account Name', description: 'Company or organization name', icon: Building2 },
      { key: 'domain', label: 'Website/Domain', description: 'Company website and online presence', icon: Globe },
      { key: 'industry', label: 'Industry', description: 'Business sector and category', icon: Building2 },
      { key: 'accountOwner', label: 'Account Owner', description: 'Team member managing this account', icon: Users },
    ],
    'Business Metrics': [
      { key: 'plan', label: 'Subscription Plan', description: 'Current plan or tier', icon: TrendingUp },
      { key: 'mrr', label: 'Monthly Recurring Revenue', description: 'MRR and revenue tracking', icon: DollarSign },
      { key: 'arr', label: 'Annual Contract Value', description: 'Total annual contract value', icon: DollarSign },
      { key: 'healthScore', label: 'Health Score', description: 'Account health indicator', icon: TrendingUp },
    ],
    'Contract Details': [
      { key: 'contractStart', label: 'Contract Start Date', description: 'When the contract began', icon: Calendar },
      { key: 'contractEnd', label: 'Contract End Date', description: 'When the contract expires', icon: Calendar },
      { key: 'renewalDate', label: 'Next Renewal Date', description: 'Upcoming renewal milestone', icon: Calendar },
      { key: 'billingCycle', label: 'Billing Cycle', description: 'Monthly, quarterly, or annual', icon: Calendar },
    ],
    'Relationship': [
      { key: 'contacts', label: 'Associated Contacts', description: 'Number and list of contacts', icon: Users },
      { key: 'conversations', label: 'Conversation History', description: 'Chat and support interactions', icon: Users },
      { key: 'accountAge', label: 'Customer Since', description: 'How long they have been a customer', icon: Calendar },
    ],
    'Organization': [
      { key: 'tags', label: 'Tags & Labels', description: 'Account categorization', icon: Tag },
      { key: 'notes', label: 'Notes Section', description: 'Internal notes and observations', icon: Layout },
      { key: 'customFields', label: 'Custom Fields', description: 'Additional custom data fields', icon: Layout },
    ],
  };

  const accountTags = ['Enterprise', 'SMB', 'Startup', 'VIP', 'Trial', 'At Risk', 'Champion', 'High Value', 'Strategic', 'Partner'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-gray-900 mb-2">Account Settings</h1>
          <p className="text-gray-600">Configure how accounts are displayed and managed</p>
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
                <h2 className="text-gray-900 mb-1">Account Field Visibility</h2>
                <p className="text-sm text-gray-600">Choose which fields appear in account detail pages</p>
              </div>

              <div className="space-y-6">
                {Object.entries(accountFieldGroups).map(([groupName, fields]) => (
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
                  <strong>Note:</strong> Hidden fields are not displayed in account details but remain in the database and can be exported.
                </div>
              </div>
            </div>
          )}

          {/* Custom Fields Tab */}
          {activeTab === 'customFields' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Custom Fields</h2>
                <p className="text-sm text-gray-600">Add and manage custom fields for accounts</p>
              </div>

              <CustomFieldsSettings entityType="account" />
            </div>
          )}

          {/* AI Features Tab */}
          {activeTab === 'ai' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">AI-Powered Account Features</h2>
                <p className="text-sm text-gray-600">Enable intelligent assistance for managing accounts</p>
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
                      Let AI help you write, improve, and organize notes about your accounts
                    </p>

                    <div className="bg-white rounded-lg p-4 mb-4">
                      <div className="text-sm text-gray-900 mb-3">Capabilities for Account Notes:</div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Generate account status summaries</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Extract key business insights</span>
                        </div>
                        <div className="flex items-start gap-2">
                          <Check className="w-4 h-4 text-purple-600 mt-0.5 flex-shrink-0" />
                          <span className="text-sm text-gray-700">Summarize contract negotiations</span>
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
                      <span className="text-sm text-gray-900">Enable AI Note Assistant for Accounts</span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Future AI Features */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="border border-gray-200 rounded-lg p-4 bg-gray-50 opacity-60">
                  <div className="flex items-center gap-2 mb-2">
                    <TrendingUp className="w-4 h-4 text-gray-500" />
                    <h4 className="text-sm text-gray-900">Health Score Prediction</h4>
                    <span className="px-2 py-0.5 bg-gray-200 text-gray-600 rounded text-xs">Coming Soon</span>
                  </div>
                  <p className="text-xs text-gray-600">AI-powered churn risk and growth opportunity detection</p>
                </div>

                <div className="border border-gray-200 rounded-lg p-4 bg-gray-50 opacity-60">
                  <div className="flex items-center gap-2 mb-2">
                    <DollarSign className="w-4 h-4 text-gray-500" />
                    <h4 className="text-sm text-gray-900">Revenue Forecasting</h4>
                    <span className="px-2 py-0.5 bg-gray-200 text-gray-600 rounded text-xs">Coming Soon</span>
                  </div>
                  <p className="text-xs text-gray-600">Predict expansion and renewal opportunities</p>
                </div>
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
                <Shield className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-blue-800">
                  <strong>Privacy:</strong> AI features use your configured DelightChat LLM. Account data is processed securely and never used for training external models.
                </div>
              </div>
            </div>
          )}

          {/* Business Metrics Tab */}
          {activeTab === 'business' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Business Metrics & Tracking</h2>
                <p className="text-sm text-gray-600">Configure how business metrics are calculated and tracked</p>
              </div>

              {/* Revenue Tracking */}
              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <DollarSign className="w-5 h-5 text-green-600" />
                  <h3 className="text-gray-900">Revenue Tracking</h3>
                </div>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Track Revenue & MRR</div>
                      <div className="text-xs text-gray-600">Enable monthly recurring revenue tracking and reporting</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={businessSettings.trackRevenue}
                      onChange={(e) => setBusinessSettings({...businessSettings, trackRevenue: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Auto-calculate MRR</div>
                      <div className="text-xs text-gray-600">Automatically calculate based on plan and billing cycle</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={businessSettings.autoCalculateMRR}
                      onChange={(e) => setBusinessSettings({...businessSettings, autoCalculateMRR: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>

              {/* Contract Management */}
              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Calendar className="w-5 h-5 text-blue-600" />
                  <h3 className="text-gray-900">Contract Management</h3>
                </div>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Contract Expiration Reminders</div>
                      <div className="text-xs text-gray-600">Get notified 90, 60, and 30 days before contract ends</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={businessSettings.contractReminders}
                      onChange={(e) => setBusinessSettings({...businessSettings, contractReminders: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <div className="p-4 bg-gray-50 rounded-lg">
                    <div className="text-sm text-gray-900 mb-2">Reminder Schedule</div>
                    <div className="space-y-1 text-xs text-gray-600">
                      <div className="flex items-center gap-2">
                        <Check className="w-3 h-3 text-green-600" />
                        <span>90 days before expiration</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Check className="w-3 h-3 text-green-600" />
                        <span>60 days before expiration</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Check className="w-3 h-3 text-green-600" />
                        <span>30 days before expiration</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Check className="w-3 h-3 text-green-600" />
                        <span>7 days before expiration</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Health Score */}
              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <TrendingUp className="w-5 h-5 text-purple-600" />
                  <h3 className="text-gray-900">Account Health Score</h3>
                </div>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Enable Health Score Tracking</div>
                      <div className="text-xs text-gray-600">Calculate account health based on engagement and usage</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={businessSettings.healthScoreEnabled}
                      onChange={(e) => setBusinessSettings({...businessSettings, healthScoreEnabled: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                    <div className="text-sm text-purple-900 mb-2">Health Score Factors</div>
                    <div className="space-y-1 text-xs text-purple-800">
                      <div>• Conversation frequency and engagement</div>
                      <div>• Last contact date</div>
                      <div>• Response time and satisfaction</div>
                      <div>• Feature usage and adoption</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Export Options */}
              <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
                <h3 className="text-gray-900 mb-4">Export Business Reports</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm text-gray-700 mb-2">Report Format</label>
                    <select 
                      value={exportFormat}
                      onChange={(e) => setExportFormat(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                    >
                      <option value="csv">CSV (.csv)</option>
                      <option value="excel">Excel (.xlsx)</option>
                      <option value="pdf">PDF Report</option>
                    </select>
                  </div>
                  <div className="flex items-end">
                    <button className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2">
                      <Download className="w-4 h-4" />
                      Export Report
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
                <h2 className="text-gray-900 mb-1">Account Tags</h2>
                <p className="text-sm text-gray-600">Organize and categorize your accounts with custom tags</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-gray-900">Available Account Tags ({accountTags.length})</h3>
                  <button className="px-3 py-1.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm flex items-center gap-2">
                    <Plus className="w-4 h-4" />
                    Add Tag
                  </button>
                </div>

                <div className="flex flex-wrap gap-2 mb-4">
                  {accountTags.map(tag => (
                    <div
                      key={tag}
                      className="px-3 py-1.5 bg-green-100 text-green-700 rounded-lg flex items-center gap-2 group hover:bg-green-200 transition-colors"
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
                <h3 className="text-gray-900 mb-4">Tag Usage by Account Type</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">Enterprise</span>
                    <span className="text-gray-600">8 accounts • $125K MRR</span>
                  </div>
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">VIP</span>
                    <span className="text-gray-600">5 accounts • $85K MRR</span>
                  </div>
                  <div className="flex items-center justify-between text-sm p-3 bg-white rounded-lg">
                    <span className="text-gray-700">At Risk</span>
                    <span className="text-gray-600">3 accounts • Needs attention</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-gray-900 mb-1">Account Notifications</h2>
                <p className="text-sm text-gray-600">Choose what you want to be notified about</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <h3 className="text-gray-900 mb-4">Account Activity</h3>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">New Account Created</div>
                      <div className="text-xs text-gray-600">When a new account is added to the system</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.newAccount}
                      onChange={(e) => setNotificationSettings({...notificationSettings, newAccount: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Account Status Changes</div>
                      <div className="text-xs text-gray-600">When account status is updated (active/trial/inactive)</div>
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
                      <div className="text-sm text-gray-900 mb-1">Health Score Changes</div>
                      <div className="text-xs text-gray-600">Alert when account health score drops significantly</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.healthScoreChange}
                      onChange={(e) => setNotificationSettings({...notificationSettings, healthScoreChange: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <h3 className="text-gray-900 mb-4">Business Alerts</h3>
                <div className="space-y-3">
                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Contract Expiring Soon</div>
                      <div className="text-xs text-gray-600">Notify when contracts are nearing expiration</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.contractExpiring}
                      onChange={(e) => setNotificationSettings({...notificationSettings, contractExpiring: e.target.checked})}
                      className="w-5 h-5 ml-4"
                    />
                  </label>

                  <label className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex-1">
                      <div className="text-sm text-gray-900 mb-1">Revenue Threshold Alerts</div>
                      <div className="text-xs text-gray-600">Get notified when account revenue crosses important milestones</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={notificationSettings.revenueThreshold}
                      onChange={(e) => setNotificationSettings({...notificationSettings, revenueThreshold: e.target.checked})}
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
                <p className="text-sm text-gray-600">Control account data access, retention, and security</p>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center gap-3 mb-4">
                  <Shield className="w-5 h-5 text-red-600" />
                  <h3 className="text-gray-900">Access Control</h3>
                </div>
                <div>
                  <label className="block text-sm text-gray-700 mb-2">Who can view account details?</label>
                  <select 
                    value={accessControl}
                    onChange={(e) => setAccessControl(e.target.value)}
                    className="w-full md:w-96 px-4 py-2 border border-gray-300 rounded-lg"
                  >
                    <option value="all">All team members</option>
                    <option value="assigned">Account owner only</option>
                    <option value="team">Account owner's team</option>
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
                  <label className="block text-sm text-gray-700 mb-2">How long to keep account data after closure?</label>
                  <select 
                    value={dataRetention}
                    onChange={(e) => setDataRetention(e.target.value)}
                    className="w-full md:w-96 px-4 py-2 border border-gray-300 rounded-lg"
                  >
                    <option value="forever">Keep forever</option>
                    <option value="1year">1 year after closure</option>
                    <option value="2years">2 years after closure</option>
                    <option value="5years">5 years after closure</option>
                    <option value="7years">7 years (compliance)</option>
                  </select>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
                <h3 className="text-gray-900 mb-4">Compliance & Audit</h3>
                <div className="space-y-2">
                  <button className="w-full px-4 py-3 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 text-sm text-left flex items-center justify-between">
                    <span>View Account Audit Log</span>
                    <span className="text-xs text-gray-500">Track all changes</span>
                  </button>
                  <button className="w-full px-4 py-3 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 text-sm text-left flex items-center justify-between">
                    <span>Export Financial Report</span>
                    <span className="text-xs text-gray-500">Revenue & contracts</span>
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
                  <div className="text-xs text-gray-600">Export all account data every Sunday for backup purposes</div>
                </div>
              </label>
            </div>
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