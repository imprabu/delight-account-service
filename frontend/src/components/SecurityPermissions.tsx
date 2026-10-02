import { useState } from 'react';
import { Users, Plus, Search, Mail, Shield, Crown, User, Settings, Trash2, X, Check, Edit2, Copy, ChevronDown, AlertCircle, Lock, Unlock, Key, Clock, Globe, FileText, Eye, Activity, Smartphone } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface SecurityPermissionsProps {
  currentPlan?: 'free' | 'professional' | 'enterprise';
  onNavigate?: (screen: string) => void;
}

interface Role {
  id: string;
  name: string;
  description: string;
  isCustom: boolean;
  basedOn?: string;
  permissions: {
    [key: string]: boolean;
  };
}

interface TeamMember {
  id: string;
  name: string;
  email: string;
  role: string;
  status: 'active' | 'invited' | 'inactive';
  avatar?: string;
  lastActive?: string;
  invitedAt?: string;
  twoFactorEnabled?: boolean;
}

interface AuditLog {
  id: string;
  user: string;
  action: string;
  resource: string;
  timestamp: string;
  ipAddress: string;
  status: 'success' | 'failed';
}

const DEFAULT_PERMISSIONS = {
  // Conversations
  view_conversations: { label: 'View conversations', category: 'Conversations' },
  reply_conversations: { label: 'Reply to conversations', category: 'Conversations' },
  assign_conversations: { label: 'Assign conversations', category: 'Conversations' },
  delete_conversations: { label: 'Delete conversations', category: 'Conversations' },
  
  // Contacts
  view_contacts: { label: 'View contacts', category: 'Contacts' },
  edit_contacts: { label: 'Edit contacts', category: 'Contacts' },
  delete_contacts: { label: 'Delete contacts', category: 'Contacts' },
  export_contacts: { label: 'Export contacts', category: 'Contacts' },
  
  // Analytics & Reports
  view_analytics: { label: 'View analytics', category: 'Analytics' },
  create_reports: { label: 'Create custom reports', category: 'Analytics' },
  export_reports: { label: 'Export reports', category: 'Analytics' },
  
  // AI Assistant
  view_bot_settings: { label: 'View assistant settings', category: 'AI Assistant' },
  edit_bot_settings: { label: 'Edit assistant settings', category: 'AI Assistant' },
  manage_knowledge_base: { label: 'Manage knowledge base', category: 'AI Assistant' },
  manage_intents: { label: 'Manage intents & flows', category: 'AI Assistant' },
  
  // Channels & Integrations
  view_channels: { label: 'View channels', category: 'Channels' },
  manage_channels: { label: 'Manage channels', category: 'Channels' },
  manage_integrations: { label: 'Manage integrations', category: 'Integrations' },
  
  // Settings & Admin
  manage_team: { label: 'Manage team members', category: 'Administration' },
  manage_roles: { label: 'Manage roles & permissions', category: 'Administration' },
  manage_billing: { label: 'Manage billing', category: 'Administration' },
  view_settings: { label: 'View settings', category: 'Administration' },
  edit_settings: { label: 'Edit settings', category: 'Administration' },
};

export function SecurityPermissions({ currentPlan = 'professional', onNavigate }: SecurityPermissionsProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showCustomRoleModal, setShowCustomRoleModal] = useState(false);
  const [selectedTab, setSelectedTab] = useState<'members' | 'roles' | 'security' | 'audit'>('members');
  const [editingRole, setEditingRole] = useState<Role | null>(null);

  // Security settings state
  const [twoFactorRequired, setTwoFactorRequired] = useState(false);
  const [passwordMinLength, setPasswordMinLength] = useState(8);
  const [passwordRequireSpecialChars, setPasswordRequireSpecialChars] = useState(true);
  const [sessionTimeout, setSessionTimeout] = useState(480); // minutes
  const [ipWhitelistEnabled, setIpWhitelistEnabled] = useState(false);
  const [ipWhitelist, setIpWhitelist] = useState<string[]>(['192.168.1.0/24']);

  // Check plan capabilities
  const canManageUsers = currentPlan !== 'free';
  const canCreateCustomRoles = currentPlan === 'enterprise';
  const hasAdvancedSecurity = currentPlan === 'enterprise';

  // Default roles based on plan
  const [roles, setRoles] = useState<Role[]>([
    {
      id: 'account_admin',
      name: 'Account Admin',
      description: 'Full access to all features and settings',
      isCustom: false,
      permissions: Object.keys(DEFAULT_PERMISSIONS).reduce((acc, key) => {
        acc[key] = true;
        return acc;
      }, {} as { [key: string]: boolean })
    },
    ...(currentPlan !== 'free' ? [{
      id: 'agent',
      name: 'Agent',
      description: 'Can handle conversations and view contacts',
      isCustom: false,
      permissions: {
        view_conversations: true,
        reply_conversations: true,
        assign_conversations: true,
        view_contacts: true,
        edit_contacts: true,
        view_analytics: true,
        view_bot_settings: true,
        view_channels: true,
        view_settings: true,
      }
    }] : []),
  ]);

  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([
    {
      id: '1',
      name: 'John Doe',
      email: 'john@company.com',
      role: 'account_admin',
      status: 'active',
      lastActive: '2 minutes ago',
      twoFactorEnabled: true
    },
    {
      id: '2',
      name: 'Sarah Smith',
      email: 'sarah@company.com',
      role: 'agent',
      status: 'active',
      lastActive: '1 hour ago',
      twoFactorEnabled: true
    },
    {
      id: '3',
      name: 'Mike Johnson',
      email: 'mike@company.com',
      role: 'agent',
      status: 'invited',
      invitedAt: '2 days ago',
      twoFactorEnabled: false
    },
  ]);

  const [auditLogs] = useState<AuditLog[]>([
    {
      id: '1',
      user: 'john@company.com',
      action: 'Updated role permissions',
      resource: 'Agent Role',
      timestamp: '2 minutes ago',
      ipAddress: '192.168.1.100',
      status: 'success'
    },
    {
      id: '2',
      user: 'sarah@company.com',
      action: 'Logged in',
      resource: 'Authentication',
      timestamp: '1 hour ago',
      ipAddress: '192.168.1.105',
      status: 'success'
    },
    {
      id: '3',
      user: 'unknown@hacker.com',
      action: 'Failed login attempt',
      resource: 'Authentication',
      timestamp: '2 hours ago',
      ipAddress: '45.123.45.67',
      status: 'failed'
    },
    {
      id: '4',
      user: 'john@company.com',
      action: 'Invited team member',
      resource: 'mike@company.com',
      timestamp: '2 days ago',
      ipAddress: '192.168.1.100',
      status: 'success'
    },
    {
      id: '5',
      user: 'sarah@company.com',
      action: 'Exported contacts',
      resource: 'Contacts Database',
      timestamp: '3 days ago',
      ipAddress: '192.168.1.105',
      status: 'success'
    },
  ]);

  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('agent');
  const [newRoleName, setNewRoleName] = useState('');
  const [newRoleDescription, setNewRoleDescription] = useState('');
  const [newRoleBasedOn, setNewRoleBasedOn] = useState('agent');
  const [newRolePermissions, setNewRolePermissions] = useState<{ [key: string]: boolean }>({});

  const handleInvite = () => {
    const newMember: TeamMember = {
      id: Date.now().toString(),
      name: inviteEmail.split('@')[0],
      email: inviteEmail,
      role: inviteRole,
      status: 'invited',
      invitedAt: 'Just now',
      twoFactorEnabled: false
    };
    setTeamMembers([...teamMembers, newMember]);
    setInviteEmail('');
    setInviteRole('agent');
    setShowInviteModal(false);
  };

  const handleCreateCustomRole = () => {
    const baseRole = roles.find(r => r.id === newRoleBasedOn);
    const newRole: Role = {
      id: `custom_${Date.now()}`,
      name: newRoleName,
      description: newRoleDescription,
      isCustom: true,
      basedOn: newRoleBasedOn,
      permissions: { ...baseRole?.permissions, ...newRolePermissions }
    };
    setRoles([...roles, newRole]);
    setShowCustomRoleModal(false);
    setNewRoleName('');
    setNewRoleDescription('');
    setNewRolePermissions({});
  };

  const handleDeleteRole = (roleId: string) => {
    if (confirm('Are you sure you want to delete this role?')) {
      setRoles(roles.filter(r => r.id !== roleId));
    }
  };

  const handleDeleteMember = (memberId: string) => {
    if (confirm('Are you sure you want to remove this team member?')) {
      setTeamMembers(teamMembers.filter(m => m.id !== memberId));
    }
  };

  const getRoleById = (roleId: string) => {
    return roles.find(r => r.id === roleId);
  };

  const filteredMembers = teamMembers.filter(member =>
    member.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    member.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Group permissions by category
  const groupedPermissions = Object.entries(DEFAULT_PERMISSIONS).reduce((acc, [key, value]) => {
    if (!acc[value.category]) {
      acc[value.category] = [];
    }
    acc[value.category].push({ key, ...value });
    return acc;
  }, {} as { [key: string]: Array<{ key: string; label: string; category: string }> });

  if (!canManageUsers) {
    return (
      <div className="p-4 md:p-6 lg:p-8">
        {onNavigate && <BackToSettings onNavigate={onNavigate} />}
        <DemoBanner />
        <div className="mb-8">
          <h1 className="text-gray-900 mb-2">Security & Permissions</h1>
          <p className="text-gray-600">Manage team access, roles, and security settings</p>
        </div>

        <div className="max-w-2xl mx-auto text-center py-12">
          <div className="w-16 h-16 bg-gradient-to-br from-purple-100 to-pink-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Crown className="w-8 h-8 text-purple-600" />
          </div>
          <h2 className="text-gray-900 mb-2">Upgrade to Manage Your Team</h2>
          <p className="text-gray-600 mb-6">
            Team management and advanced security features are available on Professional and Enterprise plans.
          </p>
          <button className="px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all">
            Upgrade Now
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 lg:p-8">
      {onNavigate && <BackToSettings onNavigate={onNavigate} />}
      <DemoBanner />
      {/* Header */}
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Security & Permissions</h1>
        <p className="text-gray-600 text-sm md:text-base">Manage team members, roles, and security settings</p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 md:gap-4 mb-6 border-b border-gray-200 overflow-x-auto">
        <button
          onClick={() => setSelectedTab('members')}
          className={`pb-3 px-1 border-b-2 transition-colors text-sm md:text-base whitespace-nowrap ${
            selectedTab === 'members'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-600 hover:text-gray-900'
          }`}
        >
          Team Members
        </button>
        <button
          onClick={() => setSelectedTab('roles')}
          className={`pb-3 px-1 border-b-2 transition-colors text-sm md:text-base whitespace-nowrap ${
            selectedTab === 'roles'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-600 hover:text-gray-900'
          }`}
        >
          Roles & Permissions
        </button>
        <button
          onClick={() => setSelectedTab('security')}
          className={`pb-3 px-1 border-b-2 transition-colors text-sm md:text-base whitespace-nowrap ${
            selectedTab === 'security'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-600 hover:text-gray-900'
          }`}
        >
          Security Settings
        </button>
        <button
          onClick={() => setSelectedTab('audit')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            selectedTab === 'audit'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-gray-600 hover:text-gray-900'
          }`}
        >
          Audit Logs
          {!hasAdvancedSecurity && (
            <Crown className="w-3 h-3 text-purple-600" />
          )}
        </button>
      </div>

      {/* Team Members Tab */}
      {selectedTab === 'members' && (
        <>
          {/* Actions Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search team members..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <button
              onClick={() => setShowInviteModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 w-full sm:w-auto"
            >
              <Plus className="w-4 h-4" />
              Invite Member
            </button>
          </div>

          {/* Team Members List */}
          <div className="bg-white rounded-xl border border-gray-200">
            {/* Desktop Table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-6 text-sm text-gray-700">Member</th>
                    <th className="text-left py-3 px-6 text-sm text-gray-700">Role</th>
                    <th className="text-left py-3 px-6 text-sm text-gray-700">Status</th>
                    <th className="text-left py-3 px-6 text-sm text-gray-700">2FA</th>
                    <th className="text-left py-3 px-6 text-sm text-gray-700">Last Active</th>
                    <th className="text-right py-3 px-6 text-sm text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredMembers.map((member) => {
                    const role = getRoleById(member.role);
                    return (
                      <tr key={member.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-4 px-6">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-500 rounded-full flex items-center justify-center text-white">
                              {member.name.charAt(0)}
                            </div>
                            <div>
                              <div className="text-gray-900 font-medium">{member.name}</div>
                              <div className="text-sm text-gray-500">{member.email}</div>
                            </div>
                          </div>
                        </td>
                        <td className="py-4 px-6">
                          <div className="flex items-center gap-2">
                            {member.role === 'account_admin' ? (
                              <Crown className="w-4 h-4 text-yellow-600" />
                            ) : role?.isCustom ? (
                              <Settings className="w-4 h-4 text-purple-600" />
                            ) : (
                              <User className="w-4 h-4 text-blue-600" />
                            )}
                            <span className="text-gray-700">{role?.name}</span>
                          </div>
                        </td>
                        <td className="py-4 px-6">
                          <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs ${
                            member.status === 'active'
                              ? 'bg-green-100 text-green-700'
                              : member.status === 'invited'
                              ? 'bg-yellow-100 text-yellow-700'
                              : 'bg-gray-100 text-gray-700'
                          }`}>
                            {member.status === 'active' && <Check className="w-3 h-3" />}
                            {member.status === 'invited' && <Mail className="w-3 h-3" />}
                            {member.status.charAt(0).toUpperCase() + member.status.slice(1)}
                          </span>
                        </td>
                        <td className="py-4 px-6">
                          {member.twoFactorEnabled ? (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs bg-green-100 text-green-700">
                              <Shield className="w-3 h-3" />
                              Enabled
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs bg-gray-100 text-gray-600">
                              <AlertCircle className="w-3 h-3" />
                              Disabled
                            </span>
                          )}
                        </td>
                        <td className="py-4 px-6 text-sm text-gray-600">
                          {member.status === 'active' ? member.lastActive : `Invited ${member.invitedAt}`}
                        </td>
                        <td className="py-4 px-6">
                          <div className="flex items-center justify-end gap-2">
                            <button className="p-2 hover:bg-gray-100 rounded transition-colors" title="Edit role">
                              <Edit2 className="w-4 h-4 text-gray-600" />
                            </button>
                            {member.role !== 'account_admin' && (
                              <button
                                onClick={() => handleDeleteMember(member.id)}
                                className="p-2 hover:bg-red-50 rounded transition-colors"
                                title="Remove member"
                              >
                                <Trash2 className="w-4 h-4 text-red-600" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Card View */}
            <div className="md:hidden divide-y divide-gray-200">
              {filteredMembers.map((member) => {
                const role = getRoleById(member.role);
                return (
                  <div key={member.id} className="p-4">
                    <div className="flex items-start gap-3 mb-3">
                      <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-500 rounded-full flex items-center justify-center text-white flex-shrink-0">
                        {member.name.charAt(0)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-gray-900 font-medium">{member.name}</div>
                        <div className="text-sm text-gray-500 truncate">{member.email}</div>
                      </div>
                      <div className="flex items-center gap-1">
                        <button className="p-2 hover:bg-gray-100 rounded transition-colors" title="Edit role">
                          <Edit2 className="w-4 h-4 text-gray-600" />
                        </button>
                        {member.role !== 'account_admin' && (
                          <button
                            onClick={() => handleDeleteMember(member.id)}
                            className="p-2 hover:bg-red-50 rounded transition-colors"
                            title="Remove member"
                          >
                            <Trash2 className="w-4 h-4 text-red-600" />
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Role</span>
                        <div className="flex items-center gap-2">
                          {member.role === 'account_admin' ? (
                            <Crown className="w-4 h-4 text-yellow-600" />
                          ) : role?.isCustom ? (
                            <Settings className="w-4 h-4 text-purple-600" />
                          ) : (
                            <User className="w-4 h-4 text-blue-600" />
                          )}
                          <span className="text-sm text-gray-900">{role?.name}</span>
                        </div>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Status</span>
                        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs ${
                          member.status === 'active'
                            ? 'bg-green-100 text-green-700'
                            : member.status === 'invited'
                            ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}>
                          {member.status === 'active' && <Check className="w-3 h-3" />}
                          {member.status === 'invited' && <Mail className="w-3 h-3" />}
                          {member.status.charAt(0).toUpperCase() + member.status.slice(1)}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">2FA</span>
                        {member.twoFactorEnabled ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs bg-green-100 text-green-700">
                            <Shield className="w-3 h-3" />
                            Enabled
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs bg-gray-100 text-gray-600">
                            <AlertCircle className="w-3 h-3" />
                            Disabled
                          </span>
                        )}
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Last Active</span>
                        <span className="text-sm text-gray-900">
                          {member.status === 'active' ? member.lastActive : `Invited ${member.invitedAt}`}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Team Stats */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
            <div className="bg-white border border-gray-200 rounded-xl p-4">
              <div className="text-sm text-gray-600 mb-1">Total Members</div>
              <div className="text-2xl text-gray-900">{teamMembers.length}</div>
            </div>
            <div className="bg-white border border-gray-200 rounded-xl p-4">
              <div className="text-sm text-gray-600 mb-1">Active Now</div>
              <div className="text-2xl text-green-600">
                {teamMembers.filter(m => m.status === 'active').length}
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded-xl p-4">
              <div className="text-sm text-gray-600 mb-1">Pending Invites</div>
              <div className="text-2xl text-yellow-600">
                {teamMembers.filter(m => m.status === 'invited').length}
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded-xl p-4">
              <div className="text-sm text-gray-600 mb-1">2FA Enabled</div>
              <div className="text-2xl text-blue-600">
                {teamMembers.filter(m => m.twoFactorEnabled).length}
              </div>
            </div>
          </div>
        </>
      )}

      {/* Roles Tab */}
      {selectedTab === 'roles' && (
        <>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <p className="text-gray-600">
              {canCreateCustomRoles
                ? 'Manage user roles and create custom roles with specific permissions'
                : 'View available roles. Upgrade to Enterprise to create custom roles.'}
            </p>
            {canCreateCustomRoles && (
              <button
                onClick={() => setShowCustomRoleModal(true)}
                className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center justify-center gap-2 w-full sm:w-auto flex-shrink-0"
              >
                <Plus className="w-4 h-4" />
                Create Custom Role
              </button>
            )}
          </div>

          {/* Roles Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {roles.map((role) => (
              <div key={role.id} className="bg-white border border-gray-200 rounded-xl p-4 md:p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${
                      role.id === 'account_admin'
                        ? 'bg-yellow-100'
                        : role.isCustom
                        ? 'bg-purple-100'
                        : 'bg-blue-100'
                    }`}>
                      {role.id === 'account_admin' ? (
                        <Crown className="w-6 h-6 text-yellow-600" />
                      ) : role.isCustom ? (
                        <Settings className="w-6 h-6 text-purple-600" />
                      ) : (
                        <User className="w-6 h-6 text-blue-600" />
                      )}
                    </div>
                    <div>
                      <h3 className="text-gray-900 font-medium">{role.name}</h3>
                      <p className="text-sm text-gray-600">{role.description}</p>
                    </div>
                  </div>
                  {role.isCustom && (
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => {
                          setEditingRole(role);
                          setShowCustomRoleModal(true);
                        }}
                        className="p-1.5 hover:bg-gray-100 rounded transition-colors"
                      >
                        <Edit2 className="w-4 h-4 text-gray-600" />
                      </button>
                      <button
                        onClick={() => handleDeleteRole(role.id)}
                        className="p-1.5 hover:bg-red-50 rounded transition-colors"
                      >
                        <Trash2 className="w-4 h-4 text-red-600" />
                      </button>
                    </div>
                  )}
                </div>

                {role.isCustom && role.basedOn && (
                  <div className="mb-3 text-xs text-gray-500 flex items-center gap-1">
                    <Copy className="w-3 h-3" />
                    Based on {roles.find(r => r.id === role.basedOn)?.name}
                  </div>
                )}

                <div className="space-y-2">
                  <div className="text-xs text-gray-600 font-medium">Permissions:</div>
                  <div className="flex flex-wrap gap-1">
                    {Object.entries(role.permissions)
                      .filter(([_, enabled]) => enabled)
                      .slice(0, 4)
                      .map(([key]) => (
                        <span key={key} className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">
                          {DEFAULT_PERMISSIONS[key]?.label}
                        </span>
                      ))}
                    {Object.values(role.permissions).filter(Boolean).length > 4 && (
                      <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs">
                        +{Object.values(role.permissions).filter(Boolean).length - 4} more
                      </span>
                    )}
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-gray-200">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-600">
                      {teamMembers.filter(m => m.role === role.id).length} members
                    </span>
                    <button
                      onClick={() => {
                        setEditingRole(role);
                        setShowRoleModal(true);
                      }}
                      className="text-blue-600 hover:text-blue-700"
                    >
                      View Details
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {!canCreateCustomRoles && (
            <div className="mt-6 p-4 bg-purple-50 border border-purple-200 rounded-lg flex items-start gap-3">
              <Crown className="w-5 h-5 text-purple-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <div className="text-purple-900 font-medium mb-1">Enterprise Feature</div>
                <p className="text-sm text-purple-700">
                  Create custom roles with tailored permissions for your team. Upgrade to Enterprise to unlock this feature.
                </p>
              </div>
              <button className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors whitespace-nowrap">
                Upgrade
              </button>
            </div>
          )}
        </>
      )}

      {/* Security Settings Tab */}
      {selectedTab === 'security' && (
        <div className="space-y-6">
          {/* Authentication */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <Key className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <h3 className="text-gray-900 font-medium">Authentication</h3>
                <p className="text-sm text-gray-600">Configure authentication and access requirements</p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-gray-900">Require Two-Factor Authentication</span>
                    {!hasAdvancedSecurity && (
                      <Crown className="w-4 h-4 text-purple-600" />
                    )}
                  </div>
                  <p className="text-sm text-gray-600">Force all team members to enable 2FA</p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={twoFactorRequired}
                    onChange={(e) => setTwoFactorRequired(e.target.checked)}
                    disabled={!hasAdvancedSecurity}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </label>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <span className="text-gray-900">Session Timeout</span>
                  <p className="text-sm text-gray-600">Automatically log out inactive users</p>
                </div>
                <select
                  value={sessionTimeout}
                  onChange={(e) => setSessionTimeout(Number(e.target.value))}
                  className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value={30}>30 minutes</option>
                  <option value={60}>1 hour</option>
                  <option value={120}>2 hours</option>
                  <option value={480}>8 hours</option>
                  <option value={1440}>24 hours</option>
                </select>
              </div>
            </div>
          </div>

          {/* Password Policy */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                <Lock className="w-5 h-5 text-green-600" />
              </div>
              <div>
                <h3 className="text-gray-900 font-medium">Password Policy</h3>
                <p className="text-sm text-gray-600">Set password requirements for your team</p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <span className="text-gray-900">Minimum Password Length</span>
                  <p className="text-sm text-gray-600">Require passwords to be at least this long</p>
                </div>
                <select
                  value={passwordMinLength}
                  onChange={(e) => setPasswordMinLength(Number(e.target.value))}
                  className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value={6}>6 characters</option>
                  <option value={8}>8 characters</option>
                  <option value={10}>10 characters</option>
                  <option value={12}>12 characters</option>
                </select>
              </div>

              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <span className="text-gray-900">Require Special Characters</span>
                  <p className="text-sm text-gray-600">Include symbols like @, #, $ in passwords</p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={passwordRequireSpecialChars}
                    onChange={(e) => setPasswordRequireSpecialChars(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </label>
              </div>
            </div>
          </div>

          {/* IP Whitelist */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                <Globe className="w-5 h-5 text-purple-600" />
              </div>
              <div className="flex-1">
                <h3 className="text-gray-900 font-medium">IP Whitelist</h3>
                <p className="text-sm text-gray-600">Restrict access to specific IP addresses</p>
              </div>
              {!hasAdvancedSecurity && (
                <Crown className="w-5 h-5 text-purple-600" />
              )}
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg mb-4">
              <div className="flex-1">
                <span className="text-gray-900">Enable IP Whitelist</span>
                <p className="text-sm text-gray-600">Only allow access from approved IPs</p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={ipWhitelistEnabled}
                  onChange={(e) => setIpWhitelistEnabled(e.target.checked)}
                  disabled={!hasAdvancedSecurity}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600 disabled:opacity-50"></div>
              </label>
            </div>

            {ipWhitelistEnabled && hasAdvancedSecurity && (
              <div className="space-y-2">
                {ipWhitelist.map((ip, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <input
                      type="text"
                      value={ip}
                      readOnly
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg bg-gray-50"
                    />
                    <button className="p-2 hover:bg-red-50 rounded transition-colors">
                      <Trash2 className="w-4 h-4 text-red-600" />
                    </button>
                  </div>
                ))}
                <button className="w-full px-4 py-2 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-gray-400 hover:text-gray-700 transition-colors">
                  + Add IP Address
                </button>
              </div>
            )}

            {!hasAdvancedSecurity && (
              <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg text-sm text-purple-700">
                IP whitelisting is an Enterprise feature. Upgrade to restrict access by location.
              </div>
            )}
          </div>

          {/* Save Button */}
          <div className="flex items-center justify-end gap-3">
            <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
              Reset to Defaults
            </button>
            <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
              Save Security Settings
            </button>
          </div>
        </div>
      )}

      {/* Audit Logs Tab */}
      {selectedTab === 'audit' && (
        <>
          {hasAdvancedSecurity ? (
            <>
              <div className="flex items-center justify-between mb-6">
                <p className="text-gray-600">
                  Track all security-related activities and user actions
                </p>
                <div className="flex items-center gap-2">
                  <select className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    <option>All Actions</option>
                    <option>Authentication</option>
                    <option>Permissions</option>
                    <option>Data Access</option>
                  </select>
                  <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    Export
                  </button>
                </div>
              </div>

              <div className="bg-white rounded-xl border border-gray-200">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-gray-200">
                        <th className="text-left py-3 px-6 text-sm text-gray-700">Timestamp</th>
                        <th className="text-left py-3 px-6 text-sm text-gray-700">User</th>
                        <th className="text-left py-3 px-6 text-sm text-gray-700">Action</th>
                        <th className="text-left py-3 px-6 text-sm text-gray-700">Resource</th>
                        <th className="text-left py-3 px-6 text-sm text-gray-700">IP Address</th>
                        <th className="text-left py-3 px-6 text-sm text-gray-700">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auditLogs.map((log) => (
                        <tr key={log.id} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="py-4 px-6 text-sm text-gray-600">
                            <div className="flex items-center gap-2">
                              <Clock className="w-4 h-4 text-gray-400" />
                              {log.timestamp}
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm text-gray-900">{log.user}</td>
                          <td className="py-4 px-6 text-sm text-gray-700">{log.action}</td>
                          <td className="py-4 px-6 text-sm text-gray-600">{log.resource}</td>
                          <td className="py-4 px-6 text-sm text-gray-600 font-mono">{log.ipAddress}</td>
                          <td className="py-4 px-6">
                            <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs ${
                              log.status === 'success'
                                ? 'bg-green-100 text-green-700'
                                : 'bg-red-100 text-red-700'
                            }`}>
                              {log.status === 'success' ? (
                                <Check className="w-3 h-3" />
                              ) : (
                                <X className="w-3 h-3" />
                              )}
                              {log.status.charAt(0).toUpperCase() + log.status.slice(1)}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Audit Stats */}
              <div className="grid grid-cols-3 gap-4 mt-6">
                <div className="bg-white border border-gray-200 rounded-xl p-4">
                  <div className="text-sm text-gray-600 mb-1">Total Events</div>
                  <div className="text-2xl text-gray-900">{auditLogs.length}</div>
                </div>
                <div className="bg-white border border-gray-200 rounded-xl p-4">
                  <div className="text-sm text-gray-600 mb-1">Successful</div>
                  <div className="text-2xl text-green-600">
                    {auditLogs.filter(l => l.status === 'success').length}
                  </div>
                </div>
                <div className="bg-white border border-gray-200 rounded-xl p-4">
                  <div className="text-sm text-gray-600 mb-1">Failed</div>
                  <div className="text-2xl text-red-600">
                    {auditLogs.filter(l => l.status === 'failed').length}
                  </div>
                </div>
              </div>
            </>
          ) : (
            <div className="max-w-2xl mx-auto text-center py-12">
              <div className="w-16 h-16 bg-gradient-to-br from-purple-100 to-pink-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Activity className="w-8 h-8 text-purple-600" />
              </div>
              <h2 className="text-gray-900 mb-2">Audit Logs Available on Enterprise</h2>
              <p className="text-gray-600 mb-6">
                Track all security events, user actions, and system changes with comprehensive audit logging. Available on Enterprise plan.
              </p>
              <button className="px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all">
                Upgrade to Enterprise
              </button>
            </div>
          )}
        </>
      )}

      {/* Invite Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-gray-900">Invite Team Member</h3>
              <button onClick={() => setShowInviteModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Email Address</label>
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="colleague@company.com"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">Role</label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {roles.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name}
                    </option>
                  ))}
                </select>
                <p className="text-xs text-gray-500 mt-1">
                  {roles.find(r => r.id === inviteRole)?.description}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 mt-6">
              <button
                onClick={() => setShowInviteModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleInvite}
                disabled={!inviteEmail}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Send Invite
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Role Modal */}
      {showCustomRoleModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto">
          <div className="bg-white rounded-xl max-w-2xl w-full p-6 my-8">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-gray-900">
                {editingRole ? 'Edit Custom Role' : 'Create Custom Role'}
              </h3>
              <button
                onClick={() => {
                  setShowCustomRoleModal(false);
                  setEditingRole(null);
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Role Name</label>
                <input
                  type="text"
                  value={newRoleName}
                  onChange={(e) => setNewRoleName(e.target.value)}
                  placeholder="e.g., Customer Support Lead"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">Description</label>
                <input
                  type="text"
                  value={newRoleDescription}
                  onChange={(e) => setNewRoleDescription(e.target.value)}
                  placeholder="Brief description of this role"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">Based On</label>
                <select
                  value={newRoleBasedOn}
                  onChange={(e) => {
                    setNewRoleBasedOn(e.target.value);
                    const baseRole = roles.find(r => r.id === e.target.value);
                    setNewRolePermissions(baseRole?.permissions || {});
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                >
                  {roles.filter(r => !r.isCustom).map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-3">Customize Permissions</label>
                <div className="border border-gray-200 rounded-lg p-4 max-h-96 overflow-y-auto">
                  {Object.entries(groupedPermissions).map(([category, permissions]) => (
                    <div key={category} className="mb-4 last:mb-0">
                      <div className="text-sm font-medium text-gray-900 mb-2">{category}</div>
                      <div className="space-y-2 ml-2">
                        {permissions.map((perm) => {
                          const baseRole = roles.find(r => r.id === newRoleBasedOn);
                          const isEnabled = newRolePermissions[perm.key] ?? baseRole?.permissions[perm.key] ?? false;
                          
                          return (
                            <label key={perm.key} className="flex items-center gap-2 cursor-pointer group">
                              <input
                                type="checkbox"
                                checked={isEnabled}
                                onChange={(e) => {
                                  setNewRolePermissions({
                                    ...newRolePermissions,
                                    [perm.key]: e.target.checked
                                  });
                                }}
                                className="w-4 h-4 border-gray-300 rounded text-purple-600 focus:ring-2 focus:ring-purple-500"
                              />
                              <span className="text-sm text-gray-700 group-hover:text-gray-900">
                                {perm.label}
                              </span>
                            </label>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 mt-6">
              <button
                onClick={() => {
                  setShowCustomRoleModal(false);
                  setEditingRole(null);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateCustomRole}
                disabled={!newRoleName || !newRoleDescription}
                className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {editingRole ? 'Save Changes' : 'Create Role'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Role Details Modal */}
      {showRoleModal && editingRole && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto">
          <div className="bg-white rounded-xl max-w-2xl w-full p-6 my-8">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${
                  editingRole.id === 'account_admin'
                    ? 'bg-yellow-100'
                    : editingRole.isCustom
                    ? 'bg-purple-100'
                    : 'bg-blue-100'
                }`}>
                  {editingRole.id === 'account_admin' ? (
                    <Crown className="w-6 h-6 text-yellow-600" />
                  ) : editingRole.isCustom ? (
                    <Settings className="w-6 h-6 text-purple-600" />
                  ) : (
                    <User className="w-6 h-6 text-blue-600" />
                  )}
                </div>
                <div>
                  <h3 className="text-gray-900">{editingRole.name}</h3>
                  <p className="text-sm text-gray-600">{editingRole.description}</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowRoleModal(false);
                  setEditingRole(null);
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="p-3 bg-gray-50 rounded-lg">
                <div className="text-sm text-gray-600 mb-1">Team Members</div>
                <div className="text-xl text-gray-900">
                  {teamMembers.filter(m => m.role === editingRole.id).length} users
                </div>
              </div>

              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Permissions</h4>
                <div className="space-y-3">
                  {Object.entries(groupedPermissions).map(([category, permissions]) => {
                    const enabledPerms = permissions.filter(p => editingRole.permissions[p.key]);
                    if (enabledPerms.length === 0) return null;
                    
                    return (
                      <div key={category} className="border border-gray-200 rounded-lg p-3">
                        <div className="text-sm font-medium text-gray-900 mb-2">{category}</div>
                        <div className="grid grid-cols-2 gap-2">
                          {enabledPerms.map((perm) => (
                            <div key={perm.key} className="flex items-center gap-2 text-sm text-gray-700">
                              <Check className="w-4 h-4 text-green-600" />
                              {perm.label}
                            </div>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 mt-6">
              <button
                onClick={() => {
                  setShowRoleModal(false);
                  setEditingRole(null);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
