import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { SessionStore, SessionResponse, SessionData, UserData, AccountData, PrivilegeData, SubscriptionInfo, FeatureInfo } from '../types/session';
import { apiGet } from '../utils/api';

interface SessionContextType {
  session: SessionData | null;
  user: UserData | null;
  account: AccountData | null;
  privileges: PrivilegeData | null;
  subscription: SubscriptionInfo | null;
  features: FeatureInfo[];
  isLoading: boolean;
  isAuthenticated: boolean;
  updateSession: (sessionResponse: SessionResponse) => void;
  clearSession: () => void;
  refreshSession: () => Promise<boolean>;
  hasPrivilege: (privilegeCode: string) => boolean;
  hasAnyPrivilege: (privilegeCodes: string[]) => boolean;
  hasAllPrivileges: (privilegeCodes: string[]) => boolean;
}

const SessionContext = createContext<SessionContextType | undefined>(undefined);

const STORAGE_KEY = 'delight_session_store';

/**
 * Load session data from localStorage
 */
function loadSessionFromStorage(): SessionStore | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Error loading session from storage:', error);
  }
  return null;
}

/**
 * Save session data to localStorage
 */
function saveSessionToStorage(store: SessionStore): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store));
  } catch (error) {
    console.error('Error saving session to storage:', error);
  }
}

/**
 * Clear session data from localStorage
 */
function clearSessionFromStorage(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    console.error('Error clearing session from storage:', error);
  }
}

/**
 * Convert SessionResponse to SessionStore format
 */
function sessionResponseToStore(response: SessionResponse): SessionStore {
  return {
    session: {
      sessionId: response.sessionId,
      userId: response.userId,
      accountId: response.accountId,
      roleId: response.roleId,
      roleCode: response.roleCode,
      roleName: response.roleName,
      expiresAt: response.expiresAt,
      lastAccessedAt: response.lastAccessedAt,
      isActive: response.isActive,
      token: response.token,
    },
    user: {
      userId: response.userId,
      emailAddress: response.emailAddress,
      firstName: response.firstName,
      lastName: response.lastName,
      companyName: response.companyName,
    },
    account: {
      accountId: response.accountId,
      accountDomain: response.accountDomain,
      companyName: response.companyName,
    },
    privileges: {
      privileges: response.privileges || [],
    },
    subscription: response.subscription || null,
    features: response.features || [],
  };
}

interface SessionProviderProps {
  children: ReactNode;
}

export function SessionProvider({ children }: SessionProviderProps) {
  const [store, setStore] = useState<SessionStore | null>(() => loadSessionFromStorage());
  const [isLoading, setIsLoading] = useState(true);

  /**
   * Update session with new data from API response
   */
  const updateSession = useCallback((sessionResponse: SessionResponse) => {
    const newStore = sessionResponseToStore(sessionResponse);
    setStore(newStore);
    saveSessionToStorage(newStore);
  }, []);

  /**
   * Clear session data
   */
  const clearSession = useCallback(() => {
    setStore(null);
    clearSessionFromStorage();
  }, []);

  /**
   * Refresh session by calling GET /accounts/session
   */
  const refreshSession = useCallback(async (): Promise<boolean> => {
    try {
      const response = await apiGet<SessionResponse>('/accounts/session');
      updateSession(response);
      return true;
    } catch (error) {
      console.error('Error refreshing session:', error);
      clearSession();
      return false;
    }
  }, [updateSession, clearSession]);

  /**
   * Check if user has a specific privilege
   */
  const hasPrivilege = useCallback((privilegeCode: string): boolean => {
    if (!store?.privileges) {
      return false;
    }
    return store.privileges.privileges.includes(privilegeCode);
  }, [store]);

  /**
   * Check if user has any of the specified privileges
   */
  const hasAnyPrivilege = useCallback((privilegeCodes: string[]): boolean => {
    if (!store?.privileges) {
      return false;
    }
    return privilegeCodes.some(code => store.privileges!.privileges.includes(code));
  }, [store]);

  /**
   * Check if user has all of the specified privileges
   */
  const hasAllPrivileges = useCallback((privilegeCodes: string[]): boolean => {
    if (!store?.privileges) {
      return false;
    }
    return privilegeCodes.every(code => store.privileges!.privileges.includes(code));
  }, [store]);

  /**
   * Load session from API on mount if we have stored data
   */
  useEffect(() => {
    const initializeSession = async () => {
      // If we have stored session data, try to refresh it from the API
      if (store) {
        await refreshSession();
      }
      setIsLoading(false);
    };

    initializeSession();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run on mount

  const value: SessionContextType = {
    session: store?.session || null,
    user: store?.user || null,
    account: store?.account || null,
    privileges: store?.privileges || null,
    subscription: store?.subscription || null,
    features: store?.features || [],
    isLoading,
    isAuthenticated: !!store?.session?.isActive,
    updateSession,
    clearSession,
    refreshSession,
    hasPrivilege,
    hasAnyPrivilege,
    hasAllPrivileges,
  };

  return (
    <SessionContext.Provider value={value}>
      {children}
    </SessionContext.Provider>
  );
}

/**
 * Hook to access session store
 * @throws Error if used outside SessionProvider
 */
export function useSession(): SessionContextType {
  const context = useContext(SessionContext);
  if (context === undefined) {
    throw new Error('useSession must be used within a SessionProvider');
  }
  return context;
}

