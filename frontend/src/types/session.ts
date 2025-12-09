/**
 * Type definitions for session, user, account, and privilege data
 */

export interface SessionData {
  sessionId: string;
  userId: number;
  accountId: number;
  roleId: number | null;
  roleCode: string | null;
  roleName: string | null;
  expiresAt: string | null; // ISO 8601 format
  lastAccessedAt: string | null; // ISO 8601 format
  isActive: boolean;
  token: string | null; // JWT token
}

export interface UserData {
  userId: number;
  emailAddress: string;
  firstName: string;
  lastName: string;
  companyName: string | null;
}

export interface AccountData {
  accountId: number;
  accountDomain: string;
  companyName: string | null;
}

export interface PrivilegeData {
  privileges: string[]; // List of privilege codes
}

export interface SessionStore {
  session: SessionData | null;
  user: UserData | null;
  account: AccountData | null;
  privileges: PrivilegeData | null;
}

export interface SessionResponse {
  sessionId: string;
  userId: number;
  accountId: number;
  roleId: number | null;
  roleCode: string | null;
  roleName: string | null;
  emailAddress: string;
  firstName: string;
  lastName: string;
  accountDomain: string;
  companyName: string | null;
  expiresAt: string | null;
  lastAccessedAt: string | null;
  isActive: boolean;
  token: string | null;
  privileges: string[];
}


