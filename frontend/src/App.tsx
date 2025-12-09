import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { Dashboard } from './components/Dashboard';
import { Analytics } from './components/Analytics';
import { ConversationsEnhanced } from './components/ConversationsEnhanced';
import { ContactsSimple } from './components/ContactsSimple';
import { BotSettings } from './components/BotSettings';
import { KnowledgeBase } from './components/KnowledgeBase';
import { IntentsFlows } from './components/IntentsFlows';
import { Integrations } from './components/Integrations';
import { Channels } from './components/Channels';
import { Testing } from './components/Testing';
import { Settings } from './components/Settings';
import { ContactSettings } from './components/ContactSettings';
import { BillingSettings } from './components/BillingSettings';
import { SecurityPermissions } from './components/SecurityPermissions';
import { NotificationSettings } from './components/NotificationSettings';
import { NotificationCenter } from './components/NotificationCenter';
import { UserProfileMenu } from './components/UserProfileMenu';
import { Login } from './components/Login';
import { Signup } from './components/Signup';
import { Activate } from './components/Activate';
import { PlanSwitcher } from './components/PlanSwitcher';
import { useSession } from './store/SessionStore';
import { apiPost } from './utils/api';
import { Bot, ChevronLeft, ChevronRight, LayoutDashboard, MessageSquare, Users, Settings as SettingsIcon, BarChart3, Search, Menu, X } from 'lucide-react';

/**
 * User Profile Button Component for Sidebar
 */
function UserProfileButton({ sidebarCollapsed, onNavigate }: { sidebarCollapsed: boolean; onNavigate: (screen: string) => void }) {
  const { user, session } = useSession();
  
  const displayName = user ? `${user.firstName} ${user.lastName}` : 'Guest';
  const initials = user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}` : 'GU';
  const role = session?.roleName || session?.roleCode || 'User';

  if (!sidebarCollapsed) {
    return (
      <button 
        onClick={() => onNavigate('settings')}
        className="w-full flex items-center gap-3 hover:bg-gray-50 rounded-lg p-2 -m-2 transition-colors"
      >
        <div className="relative w-9 h-9 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white text-sm">
          {initials}
          <div className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white"></div>
        </div>
        <div className="flex-1 min-w-0 text-left">
          <div className="text-sm text-gray-900 truncate">{displayName}</div>
          <div className="text-xs text-gray-500">{role}</div>
        </div>
      </button>
    );
  }

  return (
    <button 
      onClick={() => onNavigate('settings')}
      className="w-full flex items-center justify-center hover:bg-gray-50 rounded-lg p-2 transition-colors"
      title="Account Settings"
    >
      <div className="relative w-9 h-9 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white text-sm">
        {initials}
        <div className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white"></div>
      </div>
    </button>
  );
}

type Screen = 'dashboard' | 'analytics' | 'conversations' | 'contacts' | 'bot' | 'knowledge' | 'intents' | 'integrations' | 'channels' | 'testing' | 'settings' | 'contactSettings' | 'billing' | 'security' | 'notifications';

// Route to screen mapping
const routeToScreen: Record<string, Screen> = {
  '/': 'dashboard',
  '/dashboard': 'dashboard',
  '/analytics': 'analytics',
  '/conversations': 'conversations',
  '/contacts': 'contacts',
  '/bot': 'bot',
  '/knowledge': 'knowledge',
  '/intents': 'intents',
  '/integrations': 'integrations',
  '/channels': 'channels',
  '/testing': 'testing',
  '/settings': 'settings',
  '/contact-settings': 'contactSettings',
  '/billing': 'billing',
  '/security': 'security',
  '/notifications': 'notifications',
};

// Screen to route mapping
const screenToRoute: Record<Screen, string> = {
  dashboard: '/dashboard',
  analytics: '/analytics',
  conversations: '/conversations',
  contacts: '/contacts',
  bot: '/bot',
  knowledge: '/knowledge',
  intents: '/intents',
  integrations: '/integrations',
  channels: '/channels',
  testing: '/testing',
  settings: '/settings',
  contactSettings: '/contact-settings',
  billing: '/billing',
  security: '/security',
  notifications: '/notifications',
};

function AppContent() {
  const { hasAnyPrivilege, clearSession } = useSession();
  const navigate = useNavigate();
  const location = useLocation();
  const [currentPlan, setCurrentPlan] = useState<'free' | 'professional' | 'enterprise'>('professional');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [selectedContactEmail, setSelectedContactEmail] = useState<string | null>(null);

  // Get current screen from URL
  const activeScreen = routeToScreen[location.pathname] || 'dashboard';

  // Settings navigation - only show if user has system.admin or system.config privilege
  // Calculate this BEFORE any conditional returns to maintain hook order
  const hasSettingsAccess = hasAnyPrivilege(['system.admin', 'system.config', 'account.settings']);

  // Redirect to dashboard if user tries to access settings without required privileges
  // This must be called before any conditional returns
  useEffect(() => {
    if (activeScreen === 'settings' && !hasSettingsAccess) {
      navigate('/dashboard', { replace: true });
    }
  }, [activeScreen, hasSettingsAccess, navigate]);

  const mainNavigation = [
    { id: 'dashboard' as Screen, name: 'Dashboard', icon: LayoutDashboard },
    { id: 'analytics' as Screen, name: 'Analytics', icon: BarChart3 },
    { id: 'conversations' as Screen, name: 'Conversations', icon: MessageSquare },
    { id: 'contacts' as Screen, name: 'Contacts', icon: Users },
  ];

  const settingsNavigation = hasSettingsAccess
    ? [{ id: 'settings' as Screen, name: 'Settings', icon: SettingsIcon }]
    : [];

  const handleNavigateToContact = (email: string) => {
    setSelectedContactEmail(email);
    navigate('/contacts');
  };

  const getPageTitle = (screen: Screen): string => {
    const titles: Record<Screen, string> = {
      dashboard: 'Dashboard',
      analytics: 'Analytics',
      conversations: 'Conversations',
      contacts: 'Contacts',
      bot: 'Assistant Settings',
      knowledge: 'Knowledge Base',
      intents: 'Intents & Flows',
      integrations: 'Integrations',
      channels: 'Channels',
      testing: 'Testing Lab',
      settings: 'Settings',
      contactSettings: 'Contact Settings',
      billing: 'Billing & Subscription',
      security: 'Security & Permissions',
      notifications: 'Notifications',
    };
    return titles[screen] || screen;
  };

  // Navigation helper function
  const handleNavigate = (screen: Screen) => {
    const route = screenToRoute[screen];
    if (route) {
      navigate(route);
    }
  };

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        ></div>
      )}

      {/* Left Sidebar */}
      <aside
        className={`${
          sidebarCollapsed ? 'w-20' : 'w-64'
        } bg-white border-r border-gray-200 transition-all duration-300 flex flex-col fixed lg:relative inset-y-0 left-0 z-50 transform ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:translate-x-0`}
      >
        {/* Logo & Toggle */}
        <div className="h-16 px-4 border-b border-gray-200 flex items-center justify-between">
          {!sidebarCollapsed && (
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
              <span className="text-gray-900">DelightChat</span>
            </div>
          )}
          {sidebarCollapsed && (
            <div className="w-8 h-8 bg-gradient-to-br from-blue-600 to-purple-600 rounded-lg flex items-center justify-center mx-auto">
              <Bot className="w-5 h-5 text-white" />
            </div>
          )}
          {!sidebarCollapsed && (
            <button
              onClick={() => setSidebarCollapsed(true)}
              className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors text-gray-500 hover:text-gray-700"
              title="Collapse sidebar"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-3 overflow-y-auto">
          {/* Main Navigation */}
          <div className="space-y-1 mb-6">
            {mainNavigation.map((item) => {
              const Icon = item.icon;
              const isActive = activeScreen === item.id;
              const route = screenToRoute[item.id];
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    navigate(route);
                    setMobileMenuOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all ${
                    isActive
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                  title={sidebarCollapsed ? item.name : undefined}
                >
                  <Icon className="w-5 h-5 flex-shrink-0" />
                  {!sidebarCollapsed && <span>{item.name}</span>}
                </button>
              );
            })}
          </div>

          {/* Settings Section - Only show if user has required privileges */}
          {hasSettingsAccess && (
            <div>
              {!sidebarCollapsed && (
                <div className="px-3 mb-2 text-xs text-gray-500 uppercase tracking-wider">
                  Settings
                </div>
              )}
              {sidebarCollapsed && (
                <div className="h-px bg-gray-200 my-3" />
              )}
              <div className="space-y-1">
                {settingsNavigation.map((item) => {
                  const Icon = item.icon;
                  const isActive = activeScreen === item.id;
                  const route = screenToRoute[item.id];
                  return (
                    <button
                      key={item.id}
                      onClick={() => {
                        navigate(route);
                        setMobileMenuOpen(false);
                      }}
                      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all ${
                        isActive
                          ? 'bg-blue-600 text-white'
                          : 'text-gray-700 hover:bg-gray-100'
                      }`}
                      title={sidebarCollapsed ? item.name : undefined}
                    >
                      <Icon className="w-5 h-5 flex-shrink-0" />
                      {!sidebarCollapsed && <span>{item.name}</span>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </nav>

        {/* Expand Button (when collapsed) */}
        {sidebarCollapsed && (
          <div className="p-3 border-t border-gray-200">
            <button
              onClick={() => setSidebarCollapsed(false)}
              className="w-full p-2.5 hover:bg-gray-100 rounded-lg transition-colors text-gray-500 hover:text-gray-700 flex items-center justify-center"
              title="Expand sidebar"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        )}

        {/* User Profile */}
        <div className="p-4 border-t border-gray-200">
          <UserProfileButton 
            sidebarCollapsed={sidebarCollapsed}
            onNavigate={(screen) => {
              const route = screenToRoute[screen as Screen];
              if (route) {
                navigate(route);
              }
              setMobileMenuOpen(false);
            }}
          />
        </div>
      </aside>

      {/* Main Content Area with Header */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Header Bar */}
        <header className="h-14 md:h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 md:px-6 flex-shrink-0">
          <div className="flex items-center gap-3 md:gap-6 flex-1 min-w-0">
            {/* Mobile Menu Button */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>

            <h2 className="text-gray-900 font-medium whitespace-nowrap text-sm md:text-base">
              {getPageTitle(activeScreen)}
            </h2>
            
            {/* Quick Search - Hidden on small mobile */}
            <div className="relative max-w-md flex-1 hidden sm:block">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Quick search..."
                className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
              />
            </div>
          </div>
          
          {/* Right side - Notifications & User */}
          <div className="flex items-center gap-2 md:gap-3">
            {/* Search icon for mobile */}
            <button className="sm:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors">
              <Search className="w-5 h-5 text-gray-600" />
            </button>
            <NotificationCenter onNavigate={(screen) => {
              const route = screenToRoute[screen as Screen];
              if (route) {
                navigate(route);
              }
            }} />
            <UserProfileMenu 
              onNavigate={(screen) => {
                const route = screenToRoute[screen as Screen];
                if (route) {
                  navigate(route);
                }
              }}
              onLogout={async () => {
                // Call logout endpoint to invalidate session
                try {
                  await apiPost('/accounts/logout');
                } catch (error) {
                  console.error('Error during logout:', error);
                }
                // Clear session store regardless of API call result
                clearSession();
              }}
            />
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 overflow-auto bg-gray-50">
          <div className="h-full">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/analytics" element={<Analytics currentPlan={currentPlan} onNavigate={handleNavigate} />} />
              <Route path="/conversations" element={<ConversationsEnhanced onNavigateToContact={handleNavigateToContact} />} />
              <Route path="/contacts" element={<ContactsSimple initialContactEmail={selectedContactEmail} onClearInitial={() => setSelectedContactEmail(null)} />} />
              <Route path="/bot" element={<BotSettings onNavigate={handleNavigate} />} />
              <Route path="/knowledge" element={<KnowledgeBase onNavigate={handleNavigate} />} />
              <Route path="/intents" element={<IntentsFlows onNavigate={handleNavigate} />} />
              <Route path="/integrations" element={<Integrations onNavigate={handleNavigate} />} />
              <Route path="/channels" element={<Channels onNavigate={handleNavigate} />} />
              <Route path="/testing" element={<Testing onNavigate={handleNavigate} />} />
              <Route 
                path="/settings" 
                element={
                  hasSettingsAccess ? (
                    <Settings onNavigate={handleNavigate} currentPlan={currentPlan} />
                  ) : (
                    <Navigate to="/dashboard" replace />
                  )
                } 
              />
              <Route path="/contact-settings" element={<ContactSettings onNavigate={handleNavigate} />} />
              <Route path="/billing" element={<BillingSettings onNavigate={handleNavigate} />} />
              <Route path="/security" element={<SecurityPermissions currentPlan={currentPlan} onNavigate={handleNavigate} />} />
              <Route path="/notifications" element={<NotificationSettings onNavigate={handleNavigate} />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </div>
        </main>
      </div>

      {/* Plan Switcher (Demo Only) */}
      <PlanSwitcher currentPlan={currentPlan} onPlanChange={setCurrentPlan} />
    </div>
  );
}

export default function App() {
  const { isAuthenticated, isLoading, refreshSession, clearSession } = useSession();
  const navigate = useNavigate();
  const [showSignup, setShowSignup] = useState(false);

  /**
   * Check session on app mount
   */
  useEffect(() => {
    const checkSession = async () => {
      // SessionProvider will handle initial session loading
      // If we have a stored session, try to refresh it
      if (!isLoading) {
        await refreshSession();
      }
    };

    checkSession();
  }, []); // Only run on mount

  /**
   * Handle login - after successful signin, refresh session
   */
  const handleLogin = async (email: string, password: string) => {
    // After successful signin (handled in Login component), refresh session
    console.log('Refreshing session after login...');
    const success = await refreshSession();
    if (!success) {
      console.error('Session refresh failed after login');
    }
  };

  const handleForgotPassword = () => {
    alert('Password reset link would be sent to your email');
  };

  const handleSignUp = () => {
    setShowSignup(true);
  };

  const handleBackToLogin = () => {
    setShowSignup(false);
  };

  const handleSignUpSubmit = (name: string, companyName: string, email: string, password: string) => {
    // In a real app, you would create the account here
    console.log('Signup attempt:', { name, companyName, email });
    // Note: This is a mock signup - in real app, this would call signup API
    setShowSignup(false);
  };

  // Show loading state while checking session
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Checking session...</p>
        </div>
      </div>
    );
  }

  // Check if we're on the activation page (should be accessible without auth)
  const location = window.location.pathname;
  if (location === '/activate' || location.startsWith('/activate?')) {
    return <Activate />;
  }

  // Show signup page if showSignup is true
  if (!isAuthenticated && showSignup) {
    return (
      <Signup 
        onSignUp={handleSignUpSubmit}
        onBackToLogin={handleBackToLogin}
      />
    );
  }

  // Show login page if not logged in
  if (!isAuthenticated) {
    return (
      <Login 
        onLogin={handleLogin}
        onForgotPassword={handleForgotPassword}
        onSignUp={handleSignUp}
      />
    );
  }

  // Show authenticated app content
  return <AppContent />;
}