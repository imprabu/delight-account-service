import { useState, useRef, useEffect } from 'react';
import { User, Settings, Bell, Clock, Globe, Moon, Sun, LogOut, ChevronRight, Shield, CreditCard, HelpCircle } from 'lucide-react';
import { useSession } from '../store/SessionStore';

interface UserProfileMenuProps {
  onNavigate?: (screen: string) => void;
  onLogout?: () => void;
}

export function UserProfileMenu({ onNavigate, onLogout }: UserProfileMenuProps) {
  const { user: sessionUser, account, session } = useSession();
  const [isOpen, setIsOpen] = useState(false);
  const [showProfileSettings, setShowProfileSettings] = useState(false);
  const [showStatusMenu, setShowStatusMenu] = useState(false);
  const [availability, setAvailability] = useState('Available');
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Get user data from session store
  const user = sessionUser ? {
    name: `${sessionUser.firstName} ${sessionUser.lastName}`,
    email: sessionUser.emailAddress,
    role: session?.roleName || session?.roleCode || 'User',
    avatar: `${sessionUser.firstName.charAt(0)}${sessionUser.lastName.charAt(0)}`,
    timezone: 'America/New_York (EST)', // TODO: Get from user preferences
  } : {
    name: 'Guest',
    email: '',
    role: 'Guest',
    avatar: 'GU',
    timezone: 'America/New_York (EST)',
  };

  const statusOptions = [
    { value: 'Available', color: 'green', icon: '✓' },
    { value: 'Busy', color: 'red', icon: '●' },
    { value: 'Away', color: 'yellow', icon: '○' },
    { value: 'Do Not Disturb', color: 'purple', icon: '⊘' },
  ];

  const getStatusColor = (status: string) => {
    const option = statusOptions.find(opt => opt.value === status);
    return option?.color || 'gray';
  };

  const getStatusColorClass = (color: string) => {
    const colorMap: Record<string, { bg: string; text: string; dot: string }> = {
      green: { bg: 'bg-green-50', text: 'text-green-700', dot: 'bg-green-500' },
      red: { bg: 'bg-red-50', text: 'text-red-700', dot: 'bg-red-500' },
      yellow: { bg: 'bg-yellow-50', text: 'text-yellow-700', dot: 'bg-yellow-500' },
      purple: { bg: 'bg-purple-50', text: 'text-purple-700', dot: 'bg-purple-500' },
      gray: { bg: 'bg-gray-50', text: 'text-gray-700', dot: 'bg-gray-500' },
    };
    return colorMap[color] || colorMap.gray;
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setShowProfileSettings(false);
      }
    };

    if (isOpen || showStatusMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, showStatusMenu]);

  const handleLogout = () => {
    if (onLogout) {
      onLogout();
    }
    setIsOpen(false);
  };

  const handleNavigation = (screen: string) => {
    if (onNavigate) {
      onNavigate(screen);
    }
    setIsOpen(false);
    setShowProfileSettings(false);
  };

  const handleStatusChange = (status: string) => {
    setAvailability(status);
    setShowStatusMenu(false);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* User Avatar Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative w-8 h-8 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white text-xs font-medium cursor-pointer hover:ring-2 hover:ring-purple-300 transition-all"
        title="Account menu"
      >
        {user.avatar}
        {/* Status Indicator */}
        <div className={`absolute bottom-0 right-0 w-2.5 h-2.5 ${getStatusColorClass(getStatusColor(availability)).dot} rounded-full border-2 border-white`}></div>
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="fixed md:absolute right-0 md:right-0 left-0 md:left-auto top-14 md:top-auto md:mt-2 w-full md:w-80 bg-white md:rounded-lg shadow-xl border-t md:border border-gray-200 z-50 max-h-[calc(100vh-3.5rem)] md:max-h-auto overflow-y-auto">
          {!showProfileSettings ? (
            <>
              {/* User Info Header */}
              <div className="p-4 border-b border-gray-200">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white font-medium">
                    {user.avatar}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-900 truncate">{user.name}</h3>
                    <p className="text-sm text-gray-600 truncate">{user.email}</p>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs font-medium rounded">
                    {user.role}
                  </span>
                  <div className="relative">
                    <button 
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowStatusMenu(!showStatusMenu);
                      }}
                      className={`flex items-center gap-1.5 px-2 py-1 text-xs font-medium ${getStatusColorClass(getStatusColor(availability)).text} ${getStatusColorClass(getStatusColor(availability)).bg} hover:opacity-80 rounded transition-all`}
                    >
                      <div className={`w-2 h-2 ${getStatusColorClass(getStatusColor(availability)).dot} rounded-full`}></div>
                      {availability}
                    </button>
                    
                    {/* Status Menu Dropdown */}
                    {showStatusMenu && (
                      <div className="absolute top-full right-0 mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-50">
                        <div className="p-1">
                          {statusOptions.map((option) => {
                            const colors = getStatusColorClass(option.color);
                            return (
                              <button
                                key={option.value}
                                onClick={() => handleStatusChange(option.value)}
                                className={`w-full px-3 py-2 flex items-center gap-2 hover:bg-gray-50 rounded transition-colors text-left ${
                                  availability === option.value ? 'bg-gray-50' : ''
                                }`}
                              >
                                <div className={`w-2.5 h-2.5 ${colors.dot} rounded-full`}></div>
                                <span className={`text-sm ${colors.text}`}>{option.value}</span>
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Quick Status */}
              <div className="p-3 border-b border-gray-200 bg-gray-50">
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex items-center gap-2 text-gray-600">
                    <Clock className="w-3.5 h-3.5" />
                    <span>9:00 AM - 5:00 PM</span>
                  </div>
                  <div className="flex items-center gap-2 text-gray-600">
                    <Globe className="w-3.5 h-3.5" />
                    <span>EST</span>
                  </div>
                </div>
              </div>

              {/* Menu Items */}
              <div className="py-2">
                <button
                  onClick={() => setShowProfileSettings(true)}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <User className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Profile Settings</div>
                    <div className="text-xs text-gray-500">Update your photo and details</div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-gray-400" />
                </button>

                <button
                  onClick={() => handleNavigation('settings')}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <Settings className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Account Settings</div>
                    <div className="text-xs text-gray-500">Manage your account preferences</div>
                  </div>
                </button>

                <button
                  onClick={() => handleNavigation('notifications')}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <Bell className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Notifications</div>
                    <div className="text-xs text-gray-500">Configure notification preferences</div>
                  </div>
                </button>

                <button
                  onClick={() => handleNavigation('security')}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <Shield className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Security & Privacy</div>
                    <div className="text-xs text-gray-500">Password and security settings</div>
                  </div>
                </button>

                <button
                  onClick={() => handleNavigation('billing')}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <CreditCard className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Billing & Plans</div>
                    <div className="text-xs text-gray-500">Manage subscription and usage</div>
                  </div>
                </button>
              </div>

              {/* Divider */}
              <div className="border-t border-gray-200"></div>

              {/* Quick Links */}
              <div className="py-2">
                <div className="px-4 py-2">
                  <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Quick Links</span>
                </div>
                <button
                  onClick={() => handleNavigation('dashboard')}
                  className="w-full px-4 py-2 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left text-sm text-gray-700"
                >
                  <div className="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                  Dashboard
                </button>
                <button
                  onClick={() => handleNavigation('conversations')}
                  className="w-full px-4 py-2 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left text-sm text-gray-700"
                >
                  <div className="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                  Conversations
                </button>
              </div>

              {/* Divider */}
              <div className="border-t border-gray-200"></div>

              {/* Help & Logout */}
              <div className="py-2">
                <button
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors text-left"
                >
                  <HelpCircle className="w-5 h-5 text-gray-600" />
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">Help & Support</div>
                  </div>
                </button>

                <button
                  onClick={handleLogout}
                  className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-red-50 transition-colors text-left text-red-600"
                >
                  <LogOut className="w-5 h-5" />
                  <div className="flex-1">
                    <div className="text-sm font-medium">Log Out</div>
                  </div>
                  <kbd className="hidden md:inline-block px-2 py-1 text-xs text-gray-500 bg-gray-100 rounded">⌘L</kbd>
                </button>
              </div>
            </>
          ) : (
            /* Profile Settings Panel */
            <>
              {/* Header */}
              <div className="p-4 border-b border-gray-200">
                <div className="flex items-center gap-2 mb-1">
                  <button
                    onClick={() => setShowProfileSettings(false)}
                    className="p-1 hover:bg-gray-100 rounded transition-colors"
                  >
                    <ChevronRight className="w-4 h-4 text-gray-600 rotate-180" />
                  </button>
                  <h3 className="font-medium text-gray-900">Profile Settings</h3>
                </div>
                <p className="text-sm text-gray-600 ml-9">Update your personal information</p>
              </div>

              {/* Profile Form */}
              <div className="p-4 space-y-4 max-h-96 overflow-y-auto">
                {/* Profile Picture */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Profile Picture</label>
                  <div className="flex items-center gap-4">
                    <div className="w-16 h-16 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white font-medium text-xl">
                      {user.avatar}
                    </div>
                    <div className="flex-1">
                      <button className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        Upload Photo
                      </button>
                      <p className="text-xs text-gray-500 mt-1">JPG, PNG or GIF, max 2MB</p>
                    </div>
                  </div>
                </div>

                {/* Full Name */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Full Name</label>
                  <input
                    type="text"
                    defaultValue={user.name}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                {/* Email */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Email Address</label>
                  <input
                    type="email"
                    defaultValue={user.email}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                {/* Role */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Role</label>
                  <input
                    type="text"
                    defaultValue={user.role}
                    disabled
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed"
                  />
                  <p className="text-xs text-gray-500 mt-1">Contact your admin to change your role</p>
                </div>

                {/* Timezone */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    <Globe className="w-4 h-4 inline mr-1" />
                    Timezone
                  </label>
                  <select className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    <option>America/New_York (EST)</option>
                    <option>America/Chicago (CST)</option>
                    <option>America/Denver (MST)</option>
                    <option>America/Los_Angeles (PST)</option>
                    <option>Europe/London (GMT)</option>
                    <option>Europe/Paris (CET)</option>
                    <option>Asia/Tokyo (JST)</option>
                    <option>Asia/Dubai (GST)</option>
                    <option>Australia/Sydney (AEST)</option>
                  </select>
                </div>

                {/* Availability Status */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    <Clock className="w-4 h-4 inline mr-1" />
                    Availability Status
                  </label>
                  <select className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    <option>Available</option>
                    <option>Busy</option>
                    <option>Away</option>
                    <option>Do Not Disturb</option>
                  </select>
                </div>

                {/* Working Hours */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Working Hours</label>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs text-gray-600 mb-1">Start Time</label>
                      <input
                        type="time"
                        defaultValue="09:00"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-gray-600 mb-1">End Time</label>
                      <input
                        type="time"
                        defaultValue="17:00"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                  </div>
                </div>

                {/* Theme Preference */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Theme Preference</label>
                  <div className="grid grid-cols-3 gap-2">
                    <button className="px-3 py-2 border-2 border-blue-600 bg-blue-50 rounded-lg text-sm font-medium text-blue-700 flex items-center justify-center gap-2">
                      <Sun className="w-4 h-4" />
                      Light
                    </button>
                    <button className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 flex items-center justify-center gap-2">
                      <Moon className="w-4 h-4" />
                      Dark
                    </button>
                    <button className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">
                      Auto
                    </button>
                  </div>
                </div>

                {/* Language */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Language</label>
                  <select className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    <option>English (US)</option>
                    <option>English (UK)</option>
                    <option>Spanish</option>
                    <option>French</option>
                    <option>German</option>
                    <option>Japanese</option>
                    <option>Chinese (Simplified)</option>
                  </select>
                </div>
              </div>

              {/* Save Actions */}
              <div className="p-4 border-t border-gray-200 bg-gray-50 flex items-center justify-end gap-3">
                <button
                  onClick={() => setShowProfileSettings(false)}
                  className="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => setShowProfileSettings(false)}
                  className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  Save Changes
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
