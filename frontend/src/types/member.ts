/**
 * Type definitions for Team Members API responses
 */

export interface MemberResponse {
  userId: number;
  emailAddress: string;
  firstName: string;
  lastName: string;
  roleId: number;
  roleCode: string;
  roleName: string;
  status: 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'REMOVED';
  lastActiveAt?: string; // ISO 8601 timestamp
  twoFactorEnabled: boolean;
  invitedAt?: string; // ISO 8601 timestamp
  invitedBy?: number; // User ID
}

export interface InviteMemberRequest {
  emailAddress: string;
  roleId: number;
  firstName?: string;
  lastName?: string;
}

export interface RoleResponse {
  id: number;
  code: string;
  name: string;
  description: string;
  roleType: 'SYSTEM' | 'CUSTOM';
  isCustom: boolean;
  hierarchyLevel: number;
}

export interface UpdateMemberRequest {
  roleId?: number;
  status?: 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'REMOVED';
}






