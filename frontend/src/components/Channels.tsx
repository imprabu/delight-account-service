import { useState } from 'react';
import { Globe, MessageSquare, Phone, Mail, Link2, Plus, Check, Copy, Eye, EyeOff, Settings, Trash2, ChevronDown, ChevronRight, ExternalLink, Code, Palette, Zap, CheckCircle, AlertCircle, Loader } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface ChannelsProps {
  onNavigate: (screen: string) => void;
}

export function Channels({ onNavigate }: ChannelsProps) {
  const [selectedChannel, setSelectedChannel] = useState<string | null>('webchat');
  const [showApiKey, setShowApiKey] = useState(false);
  const [widgetColor, setWidgetColor] = useState('#2563eb');
  const [widgetPosition, setWidgetPosition] = useState<'bottom-right' | 'bottom-left'>('bottom-right');
  const [showConnectModal, setShowConnectModal] = useState(false);
  const [selectedNewChannel, setSelectedNewChannel] = useState<string | null>(null);

  const connectedChannels = [
    {
      id: 'webchat',
      name: 'Web Chat Widget',
      icon: Globe,
      status: 'active',
      color: 'blue',
      description: 'Chat widget for your website',
      conversations: 145,
      setupComplete: true,
    },
    {
      id: 'whatsapp',
      name: 'WhatsApp Business',
      icon: MessageSquare,
      status: 'active',
      color: 'green',
      description: 'WhatsApp Business API',
      conversations: 89,
      setupComplete: true,
      phoneNumber: '+1 (555) 123-4567',
    },
    {
      id: 'instagram',
      name: 'Instagram',
      icon: MessageSquare,
      status: 'active',
      color: 'pink',
      description: 'Instagram Direct Messages',
      conversations: 34,
      setupComplete: true,
      accountHandle: '@yourbrand',
    },
  ];

  const availableChannels = [
    {
      id: 'messenger',
      name: 'Facebook Messenger',
      icon: MessageSquare,
      color: 'blue',
      description: 'Connect to Facebook Messenger',
      requiresAuth: true,
    },
    {
      id: 'twitter',
      name: 'Twitter / X',
      icon: MessageSquare,
      color: 'black',
      description: 'Twitter Direct Messages',
      requiresAuth: true,
    },
    {
      id: 'telegram',
      name: 'Telegram',
      icon: MessageSquare,
      color: 'blue',
      description: 'Telegram Bot API',
      requiresAuth: true,
    },
    {
      id: 'sms',
      name: 'SMS / Text',
      icon: Phone,
      color: 'purple',
      description: 'SMS via Twilio',
      requiresAuth: true,
    },
    {
      id: 'email',
      name: 'Email',
      icon: Mail,
      color: 'red',
      description: 'Email support inbox',
      requiresAuth: false,
    },
    {
      id: 'slack',
      name: 'Slack',
      icon: MessageSquare,
      color: 'purple',
      description: 'Slack workspace integration',
      requiresAuth: true,
    },
    {
      id: 'discord',
      name: 'Discord',
      icon: MessageSquare,
      color: 'indigo',
      description: 'Discord server bot',
      requiresAuth: true,
    },
  ];

  const embedCode = `<!-- DelightChat Chat Widget -->
<script>
  window.DelightChatConfig = {
    apiKey: 'dc_live_a1b2c3d4e5f6',
    position: '${widgetPosition}',
    primaryColor: '${widgetColor}',
    greeting: 'Hi! How can I help you today?'
  };
</script>
<script src="https://cdn.dmbot.ai/widget.js" async></script>`;

  const handleCopyCode = () => {
    navigator.clipboard.writeText(embedCode);
  };

  const handleConnectChannel = (channelId: string) => {
    setSelectedNewChannel(channelId);
    setShowConnectModal(true);
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <BackToSettings onNavigate={onNavigate} />
      <DemoBanner />
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Channels</h1>
        <p className="text-gray-600 text-sm md:text-base">Connect DelightChat to your website and social media platforms</p>
      </div>

      {/* Info Banner */}
      <div className="mb-6 bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-xl p-4 md:p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <div className="flex-1">
            <h3 className="text-gray-900 mb-2">Meet Your Customers Where They Are</h3>
            <p className="text-gray-700 text-sm mb-3">
              Connect DelightChat to all your customer communication channels. All conversations flow into one unified inbox, 
              and your AI assistant responds consistently across every platform.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 bg-blue-600 rounded-full" />
                <span className="text-gray-700">Unified inbox for all channels</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 bg-purple-600 rounded-full" />
                <span className="text-gray-700">Same AI across all platforms</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 bg-blue-600 rounded-full" />
                <span className="text-gray-700">Quick setup with guides</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 md:gap-6">
        {/* Channel List */}
        <div className="lg:col-span-4 space-y-6">
          {/* Connected Channels */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-gray-900">Connected Channels</h2>
              <span className="text-sm text-gray-600">{connectedChannels.length} active</span>
            </div>
            <div className="space-y-2">
              {connectedChannels.map((channel) => {
                const Icon = channel.icon;
                return (
                  <button
                    key={channel.id}
                    onClick={() => setSelectedChannel(channel.id)}
                    className={`w-full p-4 border-2 rounded-xl text-left transition-all ${
                      selectedChannel === channel.id
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`w-10 h-10 bg-${channel.color}-50 rounded-lg flex items-center justify-center flex-shrink-0`}>
                        <Icon className={`w-5 h-5 text-${channel.color}-600`} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-gray-900">{channel.name}</span>
                          <span className="px-2 py-0.5 bg-green-50 text-green-700 rounded text-xs flex items-center gap-1">
                            <Check className="w-3 h-3" />
                            Active
                          </span>
                        </div>
                        <p className="text-sm text-gray-600 mb-2">{channel.description}</p>
                        <div className="text-xs text-gray-500">
                          {channel.conversations} conversations
                        </div>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Available Channels */}
          <div>
            <h2 className="text-gray-900 mb-4">Available Channels</h2>
            <div className="space-y-2">
              {availableChannels.map((channel) => {
                const Icon = channel.icon;
                return (
                  <div
                    key={channel.id}
                    className="p-4 border border-gray-200 rounded-xl bg-white hover:border-gray-300 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <div className={`w-10 h-10 bg-gray-50 rounded-lg flex items-center justify-center flex-shrink-0`}>
                        <Icon className="w-5 h-5 text-gray-600" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-gray-900">{channel.name}</span>
                          <button 
                            onClick={() => handleConnectChannel(channel.id)}
                            className="px-3 py-1 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm flex items-center gap-1"
                          >
                            <Plus className="w-3 h-3" />
                            Connect
                          </button>
                        </div>
                        <p className="text-sm text-gray-600">{channel.description}</p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Channel Configuration */}
        <div className="lg:col-span-8 bg-white rounded-xl border border-gray-200 p-4 md:p-6">
          {selectedChannel === 'webchat' && (
            <WebChatConfig 
              embedCode={embedCode}
              onCopyCode={handleCopyCode}
              widgetColor={widgetColor}
              setWidgetColor={setWidgetColor}
              widgetPosition={widgetPosition}
              setWidgetPosition={setWidgetPosition}
            />
          )}
          {selectedChannel === 'whatsapp' && <WhatsAppConfig />}
          {selectedChannel === 'instagram' && <InstagramConfig />}
          {!selectedChannel && (
            <div className="flex flex-col items-center justify-center h-full text-center p-12">
              <MessageSquare className="w-16 h-16 text-gray-300 mb-4" />
              <h3 className="text-gray-900 mb-2">Select a Channel</h3>
              <p className="text-gray-600">Choose a channel to view configuration and settings</p>
            </div>
          )}
        </div>
      </div>

      {/* Connect Channel Modal */}
      {showConnectModal && selectedNewChannel && (
        <ConnectChannelModal
          channelId={selectedNewChannel}
          channelInfo={availableChannels.find(c => c.id === selectedNewChannel)!}
          onClose={() => {
            setShowConnectModal(false);
            setSelectedNewChannel(null);
          }}
          onConnect={() => {
            setShowConnectModal(false);
            setSelectedNewChannel(null);
          }}
        />
      )}
    </div>
  );
}

// Web Chat Configuration Component
function WebChatConfig({ embedCode, onCopyCode, widgetColor, setWidgetColor, widgetPosition, setWidgetPosition }: any) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    onCopyCode();
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-gray-900 mb-2">Web Chat Widget</h2>
          <p className="text-gray-600 text-sm">Add DelightChat to your website with a simple code snippet</p>
        </div>
        <span className="px-3 py-1 bg-green-50 text-green-700 rounded-lg text-sm flex items-center gap-2">
          <CheckCircle className="w-4 h-4" />
          Active
        </span>
      </div>

      {/* Customization */}
      <div className="p-4 bg-gray-50 rounded-lg space-y-4">
        <h3 className="text-gray-900">Customize Widget</h3>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-gray-700 mb-2 text-sm">Widget Color</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={widgetColor}
                onChange={(e) => setWidgetColor(e.target.value)}
                className="w-12 h-10 rounded border border-gray-300 cursor-pointer"
              />
              <input
                type="text"
                value={widgetColor}
                onChange={(e) => setWidgetColor(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-gray-700 mb-2 text-sm">Position</label>
            <select 
              value={widgetPosition}
              onChange={(e) => setWidgetPosition(e.target.value as any)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
            >
              <option value="bottom-right">Bottom Right</option>
              <option value="bottom-left">Bottom Left</option>
            </select>
          </div>
        </div>

        {/* Preview */}
        <div className="mt-4">
          <label className="block text-gray-700 mb-2 text-sm">Preview</label>
          <div className="bg-white border-2 border-gray-300 rounded-lg p-4 h-48 relative">
            <div className="absolute inset-0 flex items-center justify-center text-gray-400 text-sm">
              Your Website
            </div>
            <div 
              className={`absolute ${widgetPosition === 'bottom-right' ? 'bottom-4 right-4' : 'bottom-4 left-4'}`}
              style={{ backgroundColor: widgetColor }}
            >
              <div className="w-14 h-14 rounded-full shadow-lg flex items-center justify-center text-white cursor-pointer">
                <MessageSquare className="w-6 h-6" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Installation Instructions */}
      <div>
        <h3 className="text-gray-900 mb-3">Installation Instructions</h3>
        <div className="space-y-3">
          <div className="flex items-start gap-3">
            <div className="w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm flex-shrink-0">1</div>
            <div>
              <div className="text-gray-900 mb-1">Copy the code snippet</div>
              <p className="text-sm text-gray-600">Copy the embed code below</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm flex-shrink-0">2</div>
            <div>
              <div className="text-gray-900 mb-1">Add to your website</div>
              <p className="text-sm text-gray-600">Paste the code before the closing &lt;/body&gt; tag in your HTML</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm flex-shrink-0">3</div>
            <div>
              <div className="text-gray-900 mb-1">That's it!</div>
              <p className="text-sm text-gray-600">The chat widget will appear on your site automatically</p>
            </div>
          </div>
        </div>
      </div>

      {/* Embed Code */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="block text-gray-700">Embed Code</label>
          <button
            onClick={handleCopy}
            className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2"
          >
            {copied ? (
              <>
                <Check className="w-4 h-4 text-green-600" />
                Copied!
              </>
            ) : (
              <>
                <Copy className="w-4 h-4" />
                Copy Code
              </>
            )}
          </button>
        </div>
        <div className="bg-gray-900 rounded-lg p-4 font-mono text-sm text-gray-100 overflow-x-auto">
          <pre>{embedCode}</pre>
        </div>
      </div>

      {/* Popular Platforms */}
      <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <h3 className="text-gray-900 mb-3">Platform-Specific Guides</h3>
        <div className="grid grid-cols-3 gap-2">
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>WordPress</span>
            <ExternalLink className="w-3 h-3" />
          </button>
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>Shopify</span>
            <ExternalLink className="w-3 h-3" />
          </button>
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>Webflow</span>
            <ExternalLink className="w-3 h-3" />
          </button>
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>Wix</span>
            <ExternalLink className="w-3 h-3" />
          </button>
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>Squarespace</span>
            <ExternalLink className="w-3 h-3" />
          </button>
          <button className="px-3 py-2 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-sm flex items-center justify-between">
            <span>Custom HTML</span>
            <ExternalLink className="w-3 h-3" />
          </button>
        </div>
      </div>
    </div>
  );
}

// WhatsApp Configuration Component
function WhatsAppConfig() {
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-gray-900 mb-2">WhatsApp Business</h2>
          <p className="text-gray-600 text-sm">Connect via WhatsApp Business API</p>
        </div>
        <span className="px-3 py-1 bg-green-50 text-green-700 rounded-lg text-sm flex items-center gap-2">
          <CheckCircle className="w-4 h-4" />
          Connected
        </span>
      </div>

      {/* Connection Info */}
      <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
        <div className="flex items-start gap-3">
          <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <div className="text-green-900 mb-1">WhatsApp Business Connected</div>
            <div className="text-sm text-green-700 mb-2">Phone: +1 (555) 123-4567</div>
            <div className="text-sm text-green-700">All messages are being received and processed</div>
          </div>
        </div>
      </div>

      {/* Settings */}
      <div className="space-y-4">
        <h3 className="text-gray-900">Settings</h3>
        
        <div>
          <label className="block text-gray-700 mb-2 text-sm">Business Phone Number</label>
          <input
            type="text"
            value="+1 (555) 123-4567"
            disabled
            className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600"
          />
        </div>

        <div>
          <label className="block text-gray-700 mb-2 text-sm">WhatsApp Business Account ID</label>
          <input
            type="text"
            value="123456789012345"
            disabled
            className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600 font-mono text-sm"
          />
        </div>

        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
          <div>
            <div className="text-gray-900 text-sm mb-1">Auto-reply on WhatsApp</div>
            <div className="text-xs text-gray-600">AI responds automatically to messages</div>
          </div>
          <label className="relative inline-block w-12 h-6">
            <input type="checkbox" className="sr-only peer" defaultChecked />
            <div className="w-12 h-6 bg-gray-300 rounded-full peer peer-checked:bg-blue-600 peer-focus:ring-2 peer-focus:ring-blue-500 transition-colors"></div>
            <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:translate-x-6 transition-transform"></div>
          </label>
        </div>
      </div>

      {/* Instructions */}
      <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <h3 className="text-gray-900 mb-2">How it Works</h3>
        <div className="space-y-2 text-sm text-gray-700">
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>Customers send messages to your WhatsApp Business number</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>DelightChat AI processes and responds automatically</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>All conversations appear in your unified inbox</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>You can take over any conversation anytime</span>
          </div>
        </div>
      </div>

      <div className="flex gap-3">
        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm">
          Update Settings
        </button>
        <button className="px-4 py-2 border border-red-300 text-red-700 rounded-lg hover:bg-red-50 transition-colors text-sm">
          Disconnect
        </button>
      </div>
    </div>
  );
}

// Instagram Configuration Component
function InstagramConfig() {
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-gray-900 mb-2">Instagram Direct Messages</h2>
          <p className="text-gray-600 text-sm">Respond to Instagram DMs automatically</p>
        </div>
        <span className="px-3 py-1 bg-green-50 text-green-700 rounded-lg text-sm flex items-center gap-2">
          <CheckCircle className="w-4 h-4" />
          Connected
        </span>
      </div>

      {/* Connection Info */}
      <div className="p-4 bg-pink-50 border border-pink-200 rounded-lg">
        <div className="flex items-start gap-3">
          <CheckCircle className="w-5 h-5 text-pink-600 flex-shrink-0 mt-0.5" />
          <div>
            <div className="text-pink-900 mb-1">Instagram Account Connected</div>
            <div className="text-sm text-pink-700 mb-2">@yourbrand</div>
            <div className="text-sm text-pink-700">DelightChat is monitoring your Direct Messages</div>
          </div>
        </div>
      </div>

      {/* Settings */}
      <div className="space-y-4">
        <h3 className="text-gray-900">Settings</h3>
        
        <div>
          <label className="block text-gray-700 mb-2 text-sm">Connected Account</label>
          <input
            type="text"
            value="@yourbrand"
            disabled
            className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600"
          />
        </div>

        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
          <div>
            <div className="text-gray-900 text-sm mb-1">Auto-reply to DMs</div>
            <div className="text-xs text-gray-600">AI responds automatically to direct messages</div>
          </div>
          <label className="relative inline-block w-12 h-6">
            <input type="checkbox" className="sr-only peer" defaultChecked />
            <div className="w-12 h-6 bg-gray-300 rounded-full peer peer-checked:bg-blue-600 peer-focus:ring-2 peer-focus:ring-blue-500 transition-colors"></div>
            <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:translate-x-6 transition-transform"></div>
          </label>
        </div>

        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
          <div>
            <div className="text-gray-900 text-sm mb-1">Reply to story mentions</div>
            <div className="text-xs text-gray-600">Respond when users mention you in stories</div>
          </div>
          <label className="relative inline-block w-12 h-6">
            <input type="checkbox" className="sr-only peer" />
            <div className="w-12 h-6 bg-gray-300 rounded-full peer peer-checked:bg-blue-600 peer-focus:ring-2 peer-focus:ring-blue-500 transition-colors"></div>
            <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:translate-x-6 transition-transform"></div>
          </label>
        </div>

        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
          <div>
            <div className="text-gray-900 text-sm mb-1">Reply to comments</div>
            <div className="text-xs text-gray-600">Respond to comments on your posts</div>
          </div>
          <label className="relative inline-block w-12 h-6">
            <input type="checkbox" className="sr-only peer" defaultChecked />
            <div className="w-12 h-6 bg-gray-300 rounded-full peer peer-checked:bg-blue-600 peer-focus:ring-2 peer-focus:ring-blue-500 transition-colors"></div>
            <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full peer-checked:translate-x-6 transition-transform"></div>
          </label>
        </div>
      </div>

      {/* Instructions */}
      <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <h3 className="text-gray-900 mb-2">What DelightChat Can Do</h3>
        <div className="space-y-2 text-sm text-gray-700">
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>Answer questions in Direct Messages</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>Reply to comments on your posts</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>Respond to story mentions</span>
          </div>
          <div className="flex items-start gap-2">
            <div className="w-1.5 h-1.5 bg-blue-600 rounded-full mt-1.5" />
            <span>Handle product inquiries and support</span>
          </div>
        </div>
      </div>

      <div className="flex gap-3">
        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm">
          Update Settings
        </button>
        <button className="px-4 py-2 border border-red-300 text-red-700 rounded-lg hover:bg-red-50 transition-colors text-sm">
          Disconnect
        </button>
      </div>
    </div>
  );
}

// Connect Channel Modal
function ConnectChannelModal({ channelId, channelInfo, onClose, onConnect }: any) {
  const [step, setStep] = useState(1);
  const [isConnecting, setIsConnecting] = useState(false);
  const [credentials, setCredentials] = useState<any>({});

  const getStepContent = () => {
    if (channelId === 'whatsapp') {
      return (
        <div className="space-y-4">
          <div>
            <h3 className="text-gray-900 mb-3">Connect WhatsApp Business</h3>
            <p className="text-sm text-gray-600 mb-4">
              You'll need a WhatsApp Business API account. If you don't have one, we'll help you set it up.
            </p>
          </div>

          {step === 1 && (
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 mb-2">WhatsApp Business Account ID</label>
                <input
                  type="text"
                  placeholder="123456789012345"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  onChange={(e) => setCredentials({ ...credentials, accountId: e.target.value })}
                />
                <p className="text-xs text-gray-500 mt-1">Find this in your Meta Business Manager</p>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">Access Token</label>
                <input
                  type="password"
                  placeholder="EAAG..."
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
                  onChange={(e) => setCredentials({ ...credentials, token: e.target.value })}
                />
                <p className="text-xs text-gray-500 mt-1">Your WhatsApp API access token</p>
              </div>

              <div>
                <label className="block text-gray-700 mb-2">Phone Number ID</label>
                <input
                  type="text"
                  placeholder="123456789012345"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  onChange={(e) => setCredentials({ ...credentials, phoneId: e.target.value })}
                />
                <p className="text-xs text-gray-500 mt-1">Your WhatsApp phone number ID</p>
              </div>
            </div>
          )}
        </div>
      );
    }

    if (channelId === 'instagram' || channelId === 'messenger') {
      return (
        <div className="space-y-4">
          <div>
            <h3 className="text-gray-900 mb-3">Connect {channelInfo.name}</h3>
            <p className="text-sm text-gray-600 mb-4">
              Connect your Facebook/Instagram account to enable messaging
            </p>
          </div>

          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm text-blue-900 mb-3">
              Click the button below to authorize DelightChat to access your {channelInfo.name} messages
            </p>
            <button className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2">
              <ExternalLink className="w-4 h-4" />
              Connect with Facebook
            </button>
          </div>
        </div>
      );
    }

    if (channelId === 'twitter') {
      return (
        <div className="space-y-4">
          <div>
            <h3 className="text-gray-900 mb-3">Connect Twitter / X</h3>
            <p className="text-sm text-gray-600 mb-4">
              Connect your Twitter account to respond to Direct Messages
            </p>
          </div>

          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm text-blue-900 mb-3">
              Click the button below to authorize DelightChat to access your Twitter DMs
            </p>
            <button className="w-full px-4 py-3 bg-black text-white rounded-lg hover:bg-gray-800 transition-colors flex items-center justify-center gap-2">
              <ExternalLink className="w-4 h-4" />
              Connect with Twitter
            </button>
          </div>
        </div>
      );
    }

    if (channelId === 'sms') {
      return (
        <div className="space-y-4">
          <div>
            <h3 className="text-gray-900 mb-3">Connect SMS (Twilio)</h3>
            <p className="text-sm text-gray-600 mb-4">
              Send and receive SMS messages via Twilio
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-gray-700 mb-2">Twilio Account SID</label>
              <input
                type="text"
                placeholder="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">Auth Token</label>
              <input
                type="password"
                placeholder="your_auth_token"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
              />
            </div>

            <div>
              <label className="block text-gray-700 mb-2">Twilio Phone Number</label>
              <input
                type="text"
                placeholder="+1 (555) 123-4567"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
          </div>
        </div>
      );
    }

    if (channelId === 'email') {
      return (
        <div className="space-y-4">
          <div>
            <h3 className="text-gray-900 mb-3">Connect Email</h3>
            <p className="text-sm text-gray-600 mb-4">
              Forward emails to DelightChat to handle customer support
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-gray-700 mb-2">Support Email Address</label>
              <input
                type="email"
                placeholder="support@yourcompany.com"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>

            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-900 mb-2">Forward emails to:</p>
              <div className="font-mono text-sm bg-white px-3 py-2 rounded border border-blue-200">
                inbox-a1b2c3@dmbot.ai
              </div>
            </div>
          </div>
        </div>
      );
    }

    return null;
  };

  const handleConnect = async () => {
    setIsConnecting(true);
    // Simulate connection
    setTimeout(() => {
      setIsConnecting(false);
      onConnect();
    }, 2000);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-white rounded-xl max-w-lg w-full my-8 max-h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 md:p-6 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
              <MessageSquare className="w-5 h-5 text-gray-600" />
            </div>
            <div>
              <h2 className="text-gray-900">{channelInfo.name}</h2>
              <p className="text-sm text-gray-600">{channelInfo.description}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {getStepContent()}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 flex items-center justify-between flex-shrink-0 bg-gray-50">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleConnect}
            disabled={isConnecting}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            {isConnecting ? (
              <>
                <Loader className="w-4 h-4 animate-spin" />
                Connecting...
              </>
            ) : (
              <>
                <Check className="w-4 h-4" />
                Connect Channel
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}