import { useState } from 'react';
import { Bell, Mail, MessageSquare, Users, AlertCircle, CheckCircle, Clock, TrendingUp, ChevronDown, ChevronUp } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface NotificationSettingsProps {
  onNavigate: (screen: string) => void;
}

interface NotificationPreference {
  id: string;
  name: string;
  description: string;
  email: boolean;
  inApp: boolean;
}

interface NotificationCategory {
  id: string;
  name: string;
  description: string;
  icon: any;
  iconColor: string;
  preferences: NotificationPreference[];
}

export function NotificationSettings({ onNavigate }: NotificationSettingsProps) {
  const [expandedCategories, setExpandedCategories] = useState<string[]>(['conversations', 'contacts']);
  const [notificationPreferences, setNotificationPreferences] = useState<NotificationCategory[]>([
    {
      id: 'conversations',
      name: 'Conversations',
      description: 'Get notified about new messages and conversation updates',
      icon: MessageSquare,
      iconColor: 'bg-blue-500',
      preferences: [
        {
          id: 'new-message',
          name: 'New Message',
          description: 'When a new message is received from a customer',
          email: true,
          inApp: true,
        },
        {
          id: 'assigned-conversation',
          name: 'Assigned Conversation',
          description: 'When a conversation is assigned to you',
          email: true,
          inApp: true,
        },
        {
          id: 'conversation-resolved',
          name: 'Conversation Resolved',
          description: 'When a conversation you\'re assigned to is marked as resolved',
          email: false,
          inApp: true,
        },
        {
          id: 'customer-replied',
          name: 'Customer Replied',
          description: 'When a customer replies to your message',
          email: true,
          inApp: true,
        },
      ],
    },
    {
      id: 'contacts',
      name: 'Contacts',
      description: 'Updates about contact activities and changes',
      icon: Users,
      iconColor: 'bg-purple-500',
      preferences: [
        {
          id: 'new-contact',
          name: 'New Contact',
          description: 'When a new contact is created',
          email: false,
          inApp: true,
        },
        {
          id: 'contact-updated',
          name: 'Contact Updated',
          description: 'When a contact\'s information is updated',
          email: false,
          inApp: false,
        },
        {
          id: 'lifecycle-change',
          name: 'Lifecycle Stage Changed',
          description: 'When a contact moves to a different lifecycle stage',
          email: true,
          inApp: true,
        },
      ],
    },
    {
      id: 'ai-assistant',
      name: 'AI Assistant',
      description: 'Notifications about AI assistant performance and actions',
      icon: AlertCircle,
      iconColor: 'bg-indigo-500',
      preferences: [
        {
          id: 'ai-escalation',
          name: 'AI Escalation',
          description: 'When the AI assistant escalates a conversation to a human agent',
          email: true,
          inApp: true,
        },
        {
          id: 'ai-low-confidence',
          name: 'Low Confidence Response',
          description: 'When the AI provides a response with low confidence score',
          email: false,
          inApp: true,
        },
        {
          id: 'ai-learning-suggestion',
          name: 'Learning Suggestions',
          description: 'When the AI suggests new knowledge base content or intents',
          email: false,
          inApp: true,
        },
      ],
    },
    {
      id: 'analytics',
      name: 'Analytics & Reports',
      description: 'Get updates about your metrics and performance',
      icon: TrendingUp,
      iconColor: 'bg-green-500',
      preferences: [
        {
          id: 'daily-summary',
          name: 'Daily Summary',
          description: 'Daily email with conversation metrics and key insights',
          email: true,
          inApp: false,
        },
        {
          id: 'weekly-report',
          name: 'Weekly Report',
          description: 'Weekly performance report with trends and analytics',
          email: true,
          inApp: false,
        },
        {
          id: 'custom-report-ready',
          name: 'Custom Report Ready',
          description: 'When a scheduled custom report is generated',
          email: true,
          inApp: true,
        },
      ],
    },
    {
      id: 'system',
      name: 'System',
      description: 'Important system updates and notifications',
      icon: Bell,
      iconColor: 'bg-orange-500',
      preferences: [
        {
          id: 'system-maintenance',
          name: 'System Maintenance',
          description: 'Scheduled maintenance and system updates',
          email: true,
          inApp: true,
        },
        {
          id: 'billing-updates',
          name: 'Billing Updates',
          description: 'Payment receipts and subscription changes',
          email: true,
          inApp: true,
        },
        {
          id: 'security-alerts',
          name: 'Security Alerts',
          description: 'Important security notifications and login alerts',
          email: true,
          inApp: true,
        },
      ],
    },
  ]);

  const toggleCategory = (categoryId: string) => {
    setExpandedCategories(prev =>
      prev.includes(categoryId)
        ? prev.filter(id => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  const togglePreference = (categoryId: string, preferenceId: string, type: 'email' | 'inApp') => {
    setNotificationPreferences(prev =>
      prev.map(category => {
        if (category.id === categoryId) {
          return {
            ...category,
            preferences: category.preferences.map(pref => {
              if (pref.id === preferenceId) {
                return {
                  ...pref,
                  [type]: !pref[type],
                };
              }
              return pref;
            }),
          };
        }
        return category;
      })
    );
  };

  const toggleAllInCategory = (categoryId: string, type: 'email' | 'inApp', value: boolean) => {
    setNotificationPreferences(prev =>
      prev.map(category => {
        if (category.id === categoryId) {
          return {
            ...category,
            preferences: category.preferences.map(pref => ({
              ...pref,
              [type]: value,
            })),
          };
        }
        return category;
      })
    );
  };

  const getCategoryStats = (category: NotificationCategory) => {
    const emailEnabled = category.preferences.filter(p => p.email).length;
    const inAppEnabled = category.preferences.filter(p => p.inApp).length;
    const total = category.preferences.length;
    return { emailEnabled, inAppEnabled, total };
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <DemoBanner />
      <BackToSettings onNavigate={onNavigate} />

      {/* Header */}
      <div className="mb-6 md:mb-8">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center">
            <Bell className="w-5 h-5 text-white" />
          </div>
          <h1 className="text-gray-900">Notification Settings</h1>
        </div>
        <p className="text-gray-600 text-sm md:text-base">Configure how and when you receive notifications</p>
      </div>

      {/* Global Settings */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 md:p-6 mb-6">
        <div className="flex items-start gap-3">
          <Mail className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="text-gray-900 font-medium mb-1">Notification Delivery</h3>
            <p className="text-sm text-gray-600 mb-4">
              Email notifications are sent to your account email. In-app notifications appear in the notification center.
            </p>
            <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-blue-600"></div>
                <span className="text-sm text-gray-700">Email notifications enabled</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-purple-600"></div>
                <span className="text-sm text-gray-700">In-app notifications enabled</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Notification Categories */}
      <div className="space-y-4">
        {notificationPreferences.map((category) => {
          const Icon = category.icon;
          const isExpanded = expandedCategories.includes(category.id);
          const stats = getCategoryStats(category);

          return (
            <div key={category.id} className="bg-white border border-gray-200 rounded-lg overflow-hidden">
              {/* Category Header */}
              <button
                onClick={() => toggleCategory(category.id)}
                className="w-full p-6 flex items-center justify-between hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-4">
                  <div className={`w-12 h-12 ${category.iconColor} rounded-lg flex items-center justify-center`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <div className="text-left">
                    <h3 className="text-gray-900 font-medium mb-1">{category.name}</h3>
                    <p className="text-sm text-gray-600">{category.description}</p>
                    <div className="flex items-center gap-4 mt-2">
                      <span className="text-xs text-gray-500">
                        <span className="font-medium text-blue-600">{stats.emailEnabled}</span> email
                      </span>
                      <span className="text-xs text-gray-500">
                        <span className="font-medium text-purple-600">{stats.inAppEnabled}</span> in-app
                      </span>
                      <span className="text-xs text-gray-400">of {stats.total} total</span>
                    </div>
                  </div>
                </div>
                {isExpanded ? (
                  <ChevronUp className="w-5 h-5 text-gray-400" />
                ) : (
                  <ChevronDown className="w-5 h-5 text-gray-400" />
                )}
              </button>

              {/* Category Preferences */}
              {isExpanded && (
                <div className="border-t border-gray-200">
                  {/* Column Headers */}
                  <div className="bg-gray-50 px-6 py-3 flex items-center justify-between">
                    <div className="flex-1">
                      <span className="text-xs font-medium text-gray-700">Notification Type</span>
                    </div>
                    <div className="flex items-center gap-12">
                      <button
                        onClick={() => {
                          const allEnabled = category.preferences.every(p => p.email);
                          toggleAllInCategory(category.id, 'email', !allEnabled);
                        }}
                        className="text-xs font-medium text-blue-600 hover:text-blue-700 flex items-center gap-1"
                      >
                        <Mail className="w-3 h-3" />
                        Email
                      </button>
                      <button
                        onClick={() => {
                          const allEnabled = category.preferences.every(p => p.inApp);
                          toggleAllInCategory(category.id, 'inApp', !allEnabled);
                        }}
                        className="text-xs font-medium text-purple-600 hover:text-purple-700 flex items-center gap-1"
                      >
                        <Bell className="w-3 h-3" />
                        In-App
                      </button>
                    </div>
                  </div>

                  {/* Preferences List */}
                  <div className="divide-y divide-gray-100">
                    {category.preferences.map((preference) => (
                      <div key={preference.id} className="px-6 py-4 flex items-center justify-between hover:bg-gray-50">
                        <div className="flex-1">
                          <h4 className="text-sm font-medium text-gray-900 mb-1">{preference.name}</h4>
                          <p className="text-xs text-gray-600">{preference.description}</p>
                        </div>
                        <div className="flex items-center gap-12">
                          {/* Email Toggle */}
                          <button
                            onClick={() => togglePreference(category.id, preference.id, 'email')}
                            className={`w-11 h-6 rounded-full transition-colors relative ${
                              preference.email ? 'bg-blue-600' : 'bg-gray-300'
                            }`}
                          >
                            <div
                              className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                                preference.email ? 'translate-x-6' : 'translate-x-1'
                              }`}
                            />
                          </button>

                          {/* In-App Toggle */}
                          <button
                            onClick={() => togglePreference(category.id, preference.id, 'inApp')}
                            className={`w-11 h-6 rounded-full transition-colors relative ${
                              preference.inApp ? 'bg-purple-600' : 'bg-gray-300'
                            }`}
                          >
                            <div
                              className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                                preference.inApp ? 'translate-x-6' : 'translate-x-1'
                              }`}
                            />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Save Actions */}
      <div className="mt-8 flex items-center justify-between bg-white border border-gray-200 rounded-lg p-6">
        <div className="flex items-center gap-3">
          <CheckCircle className="w-5 h-5 text-green-600" />
          <div>
            <p className="text-sm font-medium text-gray-900">Notification preferences saved</p>
            <p className="text-xs text-gray-600">Changes are applied immediately</p>
          </div>
        </div>
        <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
          Save Changes
        </button>
      </div>

      {/* Quiet Hours Section */}
      <div className="mt-6 bg-white border border-gray-200 rounded-lg p-6">
        <div className="flex items-start gap-3 mb-4">
          <Clock className="w-5 h-5 text-gray-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="text-gray-900 font-medium mb-1">Quiet Hours</h3>
            <p className="text-sm text-gray-600 mb-4">
              Set hours when you don't want to receive notifications (except critical alerts)
            </p>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Start Time</label>
            <input
              type="time"
              defaultValue="22:00"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">End Time</label>
            <input
              type="time"
              defaultValue="08:00"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
        <div className="mt-4">
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500" />
            <span className="text-sm text-gray-700">Enable quiet hours</span>
          </label>
        </div>
      </div>
    </div>
  );
}
