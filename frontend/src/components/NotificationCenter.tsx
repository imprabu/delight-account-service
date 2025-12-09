import { useState, useRef, useEffect } from 'react';
import { Bell, X, Check, MessageSquare, Users, AlertCircle, TrendingUp, Settings as SettingsIcon, Clock, CheckCheck } from 'lucide-react';

interface Notification {
  id: string;
  type: 'conversation' | 'contact' | 'ai-assistant' | 'analytics' | 'system';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  actionText?: string;
  onAction?: () => void;
}

interface NotificationCenterProps {
  onNavigate?: (screen: string) => void;
}

export function NotificationCenter({ onNavigate }: NotificationCenterProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([
    {
      id: '1',
      type: 'conversation',
      title: 'New Message from Sarah Johnson',
      message: 'Hi, I need help with my recent order #12345',
      timestamp: new Date(Date.now() - 5 * 60000), // 5 minutes ago
      read: false,
      actionText: 'View Conversation',
    },
    {
      id: '2',
      type: 'ai-assistant',
      title: 'AI Escalated Conversation',
      message: 'Customer asking about custom pricing - requires human assistance',
      timestamp: new Date(Date.now() - 15 * 60000), // 15 minutes ago
      read: false,
      actionText: 'Handle Now',
    },
    {
      id: '3',
      type: 'conversation',
      title: 'New Conversation Assigned',
      message: 'You have been assigned to help Michael Chen with product inquiry',
      timestamp: new Date(Date.now() - 30 * 60000), // 30 minutes ago
      read: false,
      actionText: 'View',
    },
    {
      id: '4',
      type: 'contact',
      title: 'Lifecycle Stage Changed',
      message: 'Emma Wilson moved from Lead to Customer',
      timestamp: new Date(Date.now() - 60 * 60000), // 1 hour ago
      read: true,
    },
    {
      id: '5',
      type: 'analytics',
      title: 'Weekly Report Ready',
      message: 'Your weekly performance report is now available to view',
      timestamp: new Date(Date.now() - 2 * 60 * 60000), // 2 hours ago
      read: true,
      actionText: 'View Report',
    },
    {
      id: '6',
      type: 'system',
      title: 'New Feature Available',
      message: 'Custom reports builder is now available for Professional plans',
      timestamp: new Date(Date.now() - 24 * 60 * 60000), // 1 day ago
      read: true,
    },
    {
      id: '7',
      type: 'ai-assistant',
      title: 'Low Confidence Response',
      message: 'AI provided response with 65% confidence - may need review',
      timestamp: new Date(Date.now() - 3 * 60 * 60000), // 3 hours ago
      read: true,
    },
  ]);

  const dropdownRef = useRef<HTMLDivElement>(null);
  const unreadCount = notifications.filter(n => !n.read).length;

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'conversation':
        return { icon: MessageSquare, color: 'bg-blue-500' };
      case 'contact':
        return { icon: Users, color: 'bg-purple-500' };
      case 'ai-assistant':
        return { icon: AlertCircle, color: 'bg-indigo-500' };
      case 'analytics':
        return { icon: TrendingUp, color: 'bg-green-500' };
      case 'system':
        return { icon: Bell, color: 'bg-orange-500' };
      default:
        return { icon: Bell, color: 'bg-gray-500' };
    }
  };

  const formatTimestamp = (date: Date) => {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const markAsRead = (id: string) => {
    setNotifications(prev =>
      prev.map(notif =>
        notif.id === id ? { ...notif, read: true } : notif
      )
    );
  };

  const markAllAsRead = () => {
    setNotifications(prev =>
      prev.map(notif => ({ ...notif, read: true }))
    );
  };

  const deleteNotification = (id: string) => {
    setNotifications(prev => prev.filter(notif => notif.id !== id));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  const handleNotificationAction = (notification: Notification) => {
    markAsRead(notification.id);
    if (notification.type === 'conversation') {
      onNavigate && onNavigate('conversations');
    } else if (notification.type === 'analytics') {
      onNavigate && onNavigate('analytics');
    } else if (notification.type === 'contact') {
      onNavigate && onNavigate('contacts');
    }
    setIsOpen(false);
  };

  const renderNotification = (notification: Notification) => {
    const { icon: Icon, color } = getNotificationIcon(notification.type);
    
    return (
      <div
        key={notification.id}
        className={`p-4 hover:bg-gray-50 transition-all cursor-pointer ${
          !notification.read ? 'bg-blue-50/50 border-l-2 border-blue-500' : 'border-l-2 border-transparent'
        }`}
        onClick={() => !notification.read && markAsRead(notification.id)}
      >
        <div className="flex gap-3">
          {/* Icon */}
          <div className={`w-10 h-10 ${color} rounded-lg flex items-center justify-center flex-shrink-0`}>
            <Icon className="w-5 h-5 text-white" />
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2 mb-1">
              <h4 className={`text-sm font-medium text-gray-900 ${!notification.read ? 'font-semibold' : ''}`}>
                {notification.title}
              </h4>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  deleteNotification(notification.id);
                }}
                className="text-gray-400 hover:text-gray-600 flex-shrink-0"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm text-gray-600 mb-2 line-clamp-2">
              {notification.message}
            </p>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Clock className="w-3 h-3 text-gray-400" />
                <span className="text-xs text-gray-500">
                  {formatTimestamp(notification.timestamp)}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {!notification.read && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      markAsRead(notification.id);
                    }}
                    className="text-xs text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
                  >
                    <Check className="w-3 h-3" />
                    Mark read
                  </button>
                )}
                {notification.actionText && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleNotificationAction(notification);
                    }}
                    className="text-xs text-blue-600 hover:text-blue-700 font-medium"
                  >
                    {notification.actionText}
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Unread indicator */}
          {!notification.read && (
            <div className="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0 mt-1.5"></div>
          )}
        </div>
      </div>
    );
  };

  const unreadNotifications = notifications.filter(n => !n.read);
  const readNotifications = notifications.filter(n => n.read);

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Notification Bell Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-all"
        title={unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}` : 'Notifications'}
        aria-label={`Notifications ${unreadCount > 0 ? `(${unreadCount} unread)` : ''}`}
      >
        <Bell className={`w-5 h-5 transition-transform ${unreadCount > 0 ? 'text-blue-600' : ''}`} />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 w-5 h-5 bg-red-500 text-white text-xs font-medium rounded-full flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Dropdown */}
      {isOpen && (
        <div className="fixed md:absolute right-0 md:right-0 left-0 md:left-auto top-14 md:top-auto md:mt-2 w-full md:w-96 bg-white md:rounded-lg shadow-xl border-t md:border border-gray-200 z-50 max-h-[calc(100vh-3.5rem)] md:max-h-[600px] flex flex-col">
          {/* Header */}
          <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="font-medium text-gray-900">Notifications</h3>
              {unreadCount > 0 && (
                <span className="px-2 py-0.5 bg-red-100 text-red-700 text-xs font-medium rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              {notifications.length > 0 && (
                <>
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllAsRead}
                      className="text-xs text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
                      title="Mark all as read"
                    >
                      <CheckCheck className="w-3 h-3" />
                    </button>
                  )}
                  <button
                    onClick={() => {
                      onNavigate && onNavigate('notifications');
                      setIsOpen(false);
                    }}
                    className="text-xs text-gray-600 hover:text-gray-700"
                    title="Notification Settings"
                  >
                    <SettingsIcon className="w-4 h-4" />
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Notifications List */}
          <div className="overflow-y-auto flex-1">
            {notifications.length === 0 ? (
              <div className="p-8 text-center">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
                  <Bell className="w-8 h-8 text-gray-400" />
                </div>
                <p className="text-gray-600 font-medium mb-1">All caught up!</p>
                <p className="text-sm text-gray-500">No new notifications</p>
              </div>
            ) : (
              <div>
                {/* Unread Notifications */}
                {unreadNotifications.length > 0 && (
                  <div>
                    <div className="px-4 py-2 bg-gray-50">
                      <span className="text-xs font-medium text-gray-700">Unread</span>
                    </div>
                    <div className="divide-y divide-gray-100">
                      {unreadNotifications.map(renderNotification)}
                    </div>
                  </div>
                )}

                {/* Read Notifications */}
                {readNotifications.length > 0 && (
                  <div>
                    {unreadNotifications.length > 0 && (
                      <div className="px-4 py-2 bg-gray-50 border-t border-gray-200">
                        <span className="text-xs font-medium text-gray-700">Earlier</span>
                      </div>
                    )}
                    <div className="divide-y divide-gray-100">
                      {readNotifications.map(renderNotification)}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            {notifications.length > 0 ? (
              <div className="flex items-center justify-between">
                <button
                  onClick={clearAll}
                  className="text-sm text-gray-600 hover:text-gray-900 font-medium"
                >
                  Clear all
                </button>
                <button
                  onClick={() => {
                    onNavigate && onNavigate('notifications');
                    setIsOpen(false);
                  }}
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
                >
                  <SettingsIcon className="w-3 h-3" />
                  Settings
                </button>
              </div>
            ) : (
              <button
                onClick={() => {
                  onNavigate && onNavigate('notifications');
                  setIsOpen(false);
                }}
                className="w-full text-sm text-blue-600 hover:text-blue-700 font-medium text-center"
              >
                Configure Notification Preferences
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
