import { useState } from 'react';
import { Search, Filter, MessageSquare, Mail, Phone, Video, User, Clock, CheckCircle, AlertCircle, X, Send, Paperclip, MoreVertical, Tag, UserCheck, Archive, ExternalLink } from 'lucide-react';

interface InboxProps {
  onNavigateToContact?: (email: string) => void;
}

export function Inbox({ onNavigateToContact }: InboxProps) {
  const [selectedView, setSelectedView] = useState<'all' | 'needs_help' | 'resolved' | 'unread' | 'assigned'>('needs_help');
  const [selectedConversation, setSelectedConversation] = useState<number | null>(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [replyText, setReplyText] = useState('');
  const [showContactLink, setShowContactLink] = useState(false);

  // Mock logged-in user
  const currentUser = {
    id: 1,
    name: 'Alex Thompson',
    email: 'alex@company.com',
    avatar: '👨‍💼',
  };

  const conversations = [
    {
      id: 1,
      customerName: 'John Davis',
      customerEmail: 'john.davis@email.com',
      avatar: '👨',
      channel: 'webchat',
      status: 'needs_help',
      subject: 'Cannot reset my password',
      lastMessage: "I've tried the reset link but it's not working. Can you help?",
      lastMessageTime: '2 min ago',
      unread: true,
      assignedTo: null,
      tags: ['account', 'urgent'],
      messageCount: 5,
      messages: [
        { id: 1, sender: 'customer', text: 'Hi, I need help resetting my password', time: '10:23 AM', avatar: '👨' },
        { id: 2, sender: 'bot', text: 'I can help you with that! I\'ll send a reset link to your email.', time: '10:23 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'I got the email but the link doesn\'t work', time: '10:25 AM', avatar: '👨' },
        { id: 4, sender: 'bot', text: 'I\'m sorry to hear that. Let me connect you with a team member who can help.', time: '10:25 AM', avatar: '🤖', confidence: 'low' },
        { id: 5, sender: 'customer', text: "I've tried the reset link but it's not working. Can you help?", time: '10:27 AM', avatar: '👨' },
      ]
    },
    {
      id: 2,
      customerName: 'Emma Wilson',
      customerEmail: 'emma.w@email.com',
      avatar: '👩',
      channel: 'email',
      status: 'needs_help',
      subject: 'Billing question about my subscription',
      lastMessage: 'Why was I charged twice this month?',
      lastMessageTime: '15 min ago',
      unread: true,
      assignedTo: currentUser.id,
      tags: ['billing'],
      messageCount: 3,
      messages: [
        { id: 1, sender: 'customer', text: 'I have a question about my bill', time: '9:45 AM', avatar: '👩' },
        { id: 2, sender: 'bot', text: 'I can help you with billing questions. What would you like to know?', time: '9:45 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Why was I charged twice this month?', time: '9:47 AM', avatar: '👩' },
      ]
    },
    {
      id: 3,
      customerName: 'Michael Brown',
      customerEmail: 'mbrown@email.com',
      avatar: '👨‍💼',
      channel: 'sms',
      status: 'needs_help',
      subject: 'Order hasn\'t arrived',
      lastMessage: 'My order #12345 still hasn\'t arrived. It\'s been 2 weeks.',
      lastMessageTime: '1 hour ago',
      unread: false,
      assignedTo: null,
      tags: ['shipping', 'urgent'],
      messageCount: 4,
      messages: [
        { id: 1, sender: 'customer', text: 'Where is my order?', time: '8:30 AM', avatar: '👨‍💼' },
        { id: 2, sender: 'bot', text: 'I can help you track your order. What\'s your order number?', time: '8:30 AM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Order #12345', time: '8:32 AM', avatar: '👨‍💼' },
        { id: 4, sender: 'customer', text: 'My order #12345 still hasn\'t arrived. It\'s been 2 weeks.', time: '8:33 AM', avatar: '👨‍💼' },
      ]
    },
    {
      id: 4,
      customerName: 'Lisa Anderson',
      customerEmail: 'lisa.a@email.com',
      avatar: '👩‍🦰',
      channel: 'whatsapp',
      status: 'resolved',
      subject: 'Product features question',
      lastMessage: 'Thanks so much! That helps a lot.',
      lastMessageTime: '2 hours ago',
      unread: false,
      assignedTo: currentUser.id,
      tags: ['product'],
      messageCount: 6,
      messages: [
        { id: 1, sender: 'customer', text: 'Can I export my data?', time: 'Yesterday 3:15 PM', avatar: '👩‍🦰' },
        { id: 2, sender: 'bot', text: 'Yes! You can export your data from Settings → Data Export.', time: 'Yesterday 3:15 PM', avatar: '🤖' },
        { id: 3, sender: 'customer', text: 'Perfect, found it. Thanks!', time: 'Yesterday 3:17 PM', avatar: '👩‍🦰' },
      ]
    },
    {
      id: 5,
      customerName: 'David Martinez',
      customerEmail: 'david.m@email.com',
      avatar: '👨‍🔧',
      channel: 'webchat',
      status: 'resolved',
      subject: 'Payment method update',
      lastMessage: 'All set, thanks!',
      lastMessageTime: '3 hours ago',
      unread: false,
      assignedTo: 2,
      tags: ['billing'],
      messageCount: 4,
      messages: []
    },
  ];

  const views = [
    { id: 'needs_help', label: 'Needs Help', icon: AlertCircle, count: conversations.filter(c => c.status === 'needs_help').length, color: 'orange' },
    { id: 'assigned', label: 'Assigned to Me', icon: UserCheck, count: conversations.filter(c => c.assignedTo === currentUser.id).length, color: 'purple' },
    { id: 'unread', label: 'Unread', icon: MessageSquare, count: conversations.filter(c => c.unread).length, color: 'blue' },
    { id: 'all', label: 'All Conversations', icon: MessageSquare, count: conversations.length, color: 'gray' },
    { id: 'resolved', label: 'Resolved', icon: CheckCircle, count: conversations.filter(c => c.status === 'resolved').length, color: 'green' },
  ];

  const channels = {
    webchat: { icon: MessageSquare, label: 'Web Chat', color: 'blue' },
    email: { icon: Mail, label: 'Email', color: 'red' },
    sms: { icon: Phone, label: 'SMS', color: 'green' },
    whatsapp: { icon: MessageSquare, label: 'WhatsApp', color: 'green' },
    messenger: { icon: MessageSquare, label: 'Messenger', color: 'blue' },
  };

  const filteredConversations = conversations.filter(conv => {
    if (selectedView === 'needs_help') return conv.status === 'needs_help';
    if (selectedView === 'resolved') return conv.status === 'resolved';
    if (selectedView === 'unread') return conv.unread;
    if (selectedView === 'assigned') return conv.assignedTo === currentUser.id;
    return true;
  }).filter(conv => {
    if (!searchQuery) return true;
    return conv.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
           conv.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
           conv.lastMessage.toLowerCase().includes(searchQuery.toLowerCase());
  });

  const selectedConv = conversations.find(c => c.id === selectedConversation);

  const handleSendMessage = () => {
    if (replyText.trim()) {
      console.log('Sending message:', replyText);
      setReplyText('');
    }
  };

  const handleAssignToMe = () => {
    console.log('Assign to me');
  };

  const handleResolve = () => {
    console.log('Mark as resolved');
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Conversations</h1>
        <p className="text-gray-600 text-sm md:text-base">Manage customer conversations across all channels</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 md:gap-6 h-[calc(100vh-240px)]">
        {/* Sidebar - Views */}
        <div className="lg:col-span-3 bg-white rounded-xl border border-gray-200 p-4 overflow-y-auto max-h-60 lg:max-h-none">
          <div className="mb-4">
            <div className="text-sm text-gray-600 mb-3">Views</div>
            <div className="space-y-1">
              {views.map((view) => {
                const Icon = view.icon;
                return (
                  <button
                    key={view.id}
                    onClick={() => setSelectedView(view.id as any)}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-lg transition-colors ${
                      selectedView === view.id
                        ? 'bg-blue-50 text-blue-700'
                        : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Icon className="w-4 h-4" />
                      <span className="text-sm">{view.label}</span>
                    </div>
                    {view.count > 0 && (
                      <span className={`px-2 py-0.5 rounded-full text-xs ${
                        selectedView === view.id
                          ? 'bg-blue-600 text-white'
                          : 'bg-gray-200 text-gray-700'
                      }`}>
                        {view.count}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* User Profile */}
          <div className="pt-4 border-t border-gray-200">
            <div className="text-sm text-gray-600 mb-3">Logged in as</div>
            <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <div className="text-2xl">{currentUser.avatar}</div>
              <div className="flex-1 min-w-0">
                <div className="text-sm text-gray-900">{currentUser.name}</div>
                <div className="text-xs text-gray-500 truncate">{currentUser.email}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Conversation List */}
        <div className="lg:col-span-4 bg-white rounded-xl border border-gray-200 flex flex-col overflow-hidden">
          {/* Search Header */}
          <div className="p-4 border-b border-gray-200 flex-shrink-0">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search conversations..."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
            </div>
            <div className="flex items-center justify-between mt-3">
              <span className="text-sm text-gray-600">
                {filteredConversations.length} conversations
              </span>
            </div>
          </div>

          {/* Conversation List */}
          <div className="flex-1 overflow-y-auto">
            {filteredConversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-center p-6">
                <MessageSquare className="w-12 h-12 text-gray-300 mb-3" />
                <p className="text-gray-600">No conversations found</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {filteredConversations.map((conv) => {
                  const ChannelIcon = channels[conv.channel as keyof typeof channels]?.icon || MessageSquare;
                  return (
                    <button
                      key={conv.id}
                      onClick={() => setSelectedConversation(conv.id)}
                      className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
                        selectedConversation === conv.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <div className="text-2xl flex-shrink-0">{conv.avatar}</div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-start justify-between mb-1">
                            <div className="flex items-center gap-2">
                              <span className="text-gray-900">{conv.customerName}</span>
                              {conv.unread && (
                                <div className="w-2 h-2 bg-blue-600 rounded-full" />
                              )}
                            </div>
                            <span className="text-xs text-gray-500 flex-shrink-0 ml-2">{conv.lastMessageTime}</span>
                          </div>
                          <div className="text-sm text-gray-900 mb-1 truncate">{conv.subject}</div>
                          <div className="text-sm text-gray-600 truncate mb-2">{conv.lastMessage}</div>
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className={`px-2 py-0.5 rounded text-xs flex items-center gap-1 ${
                              channels[conv.channel as keyof typeof channels]?.color === 'blue' ? 'bg-blue-50 text-blue-700' :
                              channels[conv.channel as keyof typeof channels]?.color === 'red' ? 'bg-red-50 text-red-700' :
                              'bg-green-50 text-green-700'
                            }`}>
                              <ChannelIcon className="w-3 h-3" />
                              {channels[conv.channel as keyof typeof channels]?.label}
                            </span>
                            {conv.status === 'needs_help' && (
                              <span className="px-2 py-0.5 bg-orange-50 text-orange-700 rounded text-xs flex items-center gap-1">
                                <AlertCircle className="w-3 h-3" />
                                Needs Help
                              </span>
                            )}
                            {conv.assignedTo === currentUser.id && (
                              <span className="px-2 py-0.5 bg-purple-50 text-purple-700 rounded text-xs">
                                Assigned to you
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Conversation Detail */}
        <div className="lg:col-span-5 bg-white rounded-xl border border-gray-200 flex flex-col overflow-hidden">
          {selectedConv ? (
            <>
              {/* Conversation Header */}
              <div className="p-4 border-b border-gray-200 flex-shrink-0">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-start gap-3">
                    <div className="text-3xl">{selectedConv.avatar}</div>
                    <div>
                      <h3 className="text-gray-900 mb-1">{selectedConv.customerName}</h3>
                      <p className="text-sm text-gray-600">{selectedConv.customerEmail}</p>
                      <div className="flex items-center gap-2 mt-2">
                        {(() => {
                          const ChannelIcon = channels[selectedConv.channel as keyof typeof channels]?.icon || MessageSquare;
                          return (
                            <span className={`px-2 py-0.5 rounded text-xs flex items-center gap-1 ${
                              channels[selectedConv.channel as keyof typeof channels]?.color === 'blue' ? 'bg-blue-50 text-blue-700' :
                              channels[selectedConv.channel as keyof typeof channels]?.color === 'red' ? 'bg-red-50 text-red-700' :
                              'bg-green-50 text-green-700'
                            }`}>
                              <ChannelIcon className="w-3 h-3" />
                              {channels[selectedConv.channel as keyof typeof channels]?.label}
                            </span>
                          );
                        })()}
                        {selectedConv.tags.map((tag, idx) => (
                          <span key={idx} className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs">
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                  <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                    <MoreVertical className="w-5 h-5 text-gray-600" />
                  </button>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-2">
                  <button 
                    onClick={() => setShowContactLink(true)}
                    className="px-3 py-1.5 border border-blue-300 bg-blue-50 text-blue-700 rounded-lg hover:bg-blue-100 transition-colors text-sm flex items-center gap-2"
                  >
                    <User className="w-4 h-4" />
                    View Contact
                  </button>
                  {!selectedConv.assignedTo && (
                    <button 
                      onClick={handleAssignToMe}
                      className="px-3 py-1.5 bg-purple-50 text-purple-700 rounded-lg hover:bg-purple-100 transition-colors text-sm flex items-center gap-2"
                    >
                      <UserCheck className="w-4 h-4" />
                      Assign to Me
                    </button>
                  )}
                  {selectedConv.status === 'needs_help' && (
                    <button 
                      onClick={handleResolve}
                      className="px-3 py-1.5 bg-green-50 text-green-700 rounded-lg hover:bg-green-100 transition-colors text-sm flex items-center gap-2"
                    >
                      <CheckCircle className="w-4 h-4" />
                      Mark Resolved
                    </button>
                  )}
                  <button className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm flex items-center gap-2">
                    <Archive className="w-4 h-4" />
                    Archive
                  </button>
                </div>
              </div>

              {/* Messages */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {selectedConv.messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex gap-3 ${msg.sender === 'customer' ? '' : 'flex-row-reverse'}`}
                  >
                    <div className="text-2xl flex-shrink-0">{msg.avatar}</div>
                    <div className={`flex-1 ${msg.sender === 'customer' ? '' : 'flex flex-col items-end'}`}>
                      <div className={`inline-block max-w-lg px-4 py-3 rounded-lg ${
                        msg.sender === 'customer'
                          ? 'bg-gray-100 text-gray-900'
                          : msg.sender === 'bot'
                          ? 'bg-blue-50 text-gray-900 border border-blue-200'
                          : 'bg-blue-600 text-white'
                      }`}>
                        <p className="text-sm">{msg.text}</p>
                        {msg.confidence === 'low' && (
                          <div className="mt-2 pt-2 border-t border-orange-200 flex items-center gap-2 text-xs text-orange-700">
                            <AlertCircle className="w-3 h-3" />
                            <span>AI wasn't confident - escalated to human</span>
                          </div>
                        )}
                      </div>
                      <span className="text-xs text-gray-500 mt-1">{msg.time}</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Reply Input */}
              <div className="p-4 border-t border-gray-200 flex-shrink-0">
                <div className="flex items-end gap-2">
                  <div className="flex-1">
                    <textarea
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      placeholder="Type your reply..."
                      rows={3}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <button className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                      <Paperclip className="w-5 h-5 text-gray-600" />
                    </button>
                    <button 
                      onClick={handleSendMessage}
                      disabled={!replyText.trim()}
                      className="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Send className="w-5 h-5" />
                    </button>
                  </div>
                </div>
                <div className="flex items-center gap-4 mt-2 text-xs text-gray-500">
                  <span>Press Enter to send</span>
                  <span>•</span>
                  <span>Shift + Enter for new line</span>
                </div>
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-center p-6">
              <MessageSquare className="w-16 h-16 text-gray-300 mb-4" />
              <h3 className="text-gray-900 mb-2">No conversation selected</h3>
              <p className="text-gray-600 text-sm">Choose a conversation from the list to view details</p>
            </div>
          )}
        </div>
      </div>

      {/* Contact Link Modal */}
      {showContactLink && selectedConv && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto" onClick={() => setShowContactLink(false)}>
          <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full my-8 p-4 md:p-6">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h2 className="text-gray-900 mb-1">View Contact Profile</h2>
                <p className="text-sm text-gray-600">Navigate to the contact's detailed profile</p>
              </div>
              <button onClick={() => setShowContactLink(false)} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-gray-50 rounded-lg p-4 mb-6">
              <div className="flex items-center gap-3 mb-3">
                <div className="text-3xl">{selectedConv.avatar}</div>
                <div>
                  <div className="text-gray-900">{selectedConv.customerName}</div>
                  <div className="text-sm text-gray-600">{selectedConv.customerEmail}</div>
                </div>
              </div>
              <p className="text-sm text-gray-600">
                Click below to view full contact details including conversation history, notes, and account information.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowContactLink(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  if (onNavigateToContact) {
                    onNavigateToContact(selectedConv.customerEmail);
                  }
                  setShowContactLink(false);
                }}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2"
              >
                <ExternalLink className="w-4 h-4" />
                View Contact
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}