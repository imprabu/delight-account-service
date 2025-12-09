import { Users, Building2, Bot, Globe, Bell, Shield, Palette, CreditCard, ChevronRight, Database, GitBranch, TestTube, Zap, Crown } from 'lucide-react';
import { DemoBanner } from './DemoBanner';

interface SettingsProps {
  onNavigate: (screen: string) => void;
  currentPlan?: 'free' | 'professional' | 'enterprise';
}

export function Settings({ onNavigate, currentPlan = 'professional' }: SettingsProps) {
  const canManageUsers = currentPlan !== 'free';
  const settingsGroups = [
    {
      title: 'AI & Assistant',
      description: 'Configure your AI assistant, knowledge base, and automation',
      settings: [
        {
          id: 'bot',
          name: 'Assistant Settings',
          description: 'Configure AI model, behavior, personality, and response settings',
          icon: Bot,
          iconColor: 'bg-indigo-500',
        },
        {
          id: 'knowledge',
          name: 'Knowledge Base',
          description: 'Manage your AI knowledge sources, documents, and training data',
          icon: Database,
          iconColor: 'bg-blue-500',
        },
        {
          id: 'intents',
          name: 'Intents & Flows',
          description: 'Configure conversation flows, intent detection, and automation rules',
          icon: GitBranch,
          iconColor: 'bg-purple-500',
        },
        {
          id: 'testing',
          name: 'Testing Lab',
          description: 'Test your AI assistant responses and conversation flows',
          icon: TestTube,
          iconColor: 'bg-cyan-500',
        },
      ],
    },
    {
      title: 'Channels & Communication',
      description: 'Manage communication channels and integrations',
      settings: [
        {
          id: 'channels',
          name: 'Channels',
          description: 'Configure website chat, WhatsApp, email, and other channels',
          icon: Globe,
          iconColor: 'bg-green-500',
        },
        {
          id: 'integrations',
          name: 'Integrations',
          description: 'Connect third-party tools and services via APIs and MCPs',
          icon: Zap,
          iconColor: 'bg-yellow-500',
        },
      ],
    },

    {
      title: 'Data Management',
      description: 'Configure how your contacts are managed',
      settings: [
        {
          id: 'contactSettings',
          name: 'Contact Settings',
          description: 'Configure contact fields, custom fields, and privacy settings',
          icon: Building2,
          iconColor: 'bg-indigo-500',
        },
      ],
    },
    {
      title: 'System & Security',
      description: 'General system settings and security configurations',
      settings: [
        {
          id: 'notifications',
          name: 'Notifications',
          description: 'Configure email notifications and alerts for your team',
          icon: Bell,
          iconColor: 'bg-orange-500',
        },
        {
          id: 'security',
          name: 'Security & Permissions',
          description: 'Manage team members, roles, access control, and security settings',
          icon: Shield,
          iconColor: 'bg-red-500',
          disabled: !canManageUsers,
          requiresPlan: canManageUsers ? undefined : 'Professional',
        },

        {
          id: 'billing',
          name: 'Billing & Subscription',
          description: 'Manage your subscription, usage, and payment methods',
          icon: CreditCard,
          iconColor: 'bg-yellow-500',
        },
      ],
    },
  ];

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <DemoBanner />
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Settings</h1>
        <p className="text-gray-600 text-sm md:text-base">Manage all your DelightChat configuration and preferences</p>
      </div>

      <div className="space-y-6 md:space-y-8">
        {settingsGroups.map((group) => (
          <div key={group.title}>
            <div className="mb-4">
              <h2 className="text-gray-900">{group.title}</h2>
              <p className="text-sm text-gray-600">{group.description}</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {group.settings.map((setting: any) => {
                const Icon = setting.icon;
                const isDisabled = setting.disabled || setting.comingSoon;
                const requiresPlanUpgrade = setting.requiresPlan;
                
                return (
                  <button
                    key={setting.id}
                    onClick={() => !isDisabled && onNavigate(setting.id)}
                    disabled={isDisabled}
                    className={`relative bg-white border border-gray-200 rounded-xl p-6 text-left transition-all ${
                      isDisabled
                        ? 'opacity-50 cursor-not-allowed'
                        : 'hover:border-blue-500 hover:shadow-md cursor-pointer'
                    }`}
                  >
                    <div className="flex items-start gap-4">
                      <div className={`${setting.iconColor} rounded-lg p-3 flex-shrink-0`}>
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="text-gray-900">{setting.name}</h3>
                          {setting.comingSoon && (
                            <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded">
                              Coming Soon
                            </span>
                          )}
                          {requiresPlanUpgrade && (
                            <span className="px-2 py-0.5 text-xs bg-purple-100 text-purple-700 rounded flex items-center gap-1">
                              <Crown className="w-3 h-3" />
                              {requiresPlanUpgrade}+
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-600">{setting.description}</p>
                      </div>

                      {!isDisabled && (
                        <ChevronRight className="w-5 h-5 text-gray-400 flex-shrink-0 mt-1" />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Quick Access Info */}
      <div className="mt-8 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p className="text-sm text-blue-900">
          <strong>Tip:</strong> Settings marked as "Coming Soon" will be available in future updates. 
          Contact your administrator if you need access to specific settings.
        </p>
      </div>
    </div>
  );
}