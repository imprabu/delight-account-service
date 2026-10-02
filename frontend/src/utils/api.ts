/**
 * API utility functions for making requests to the backend.
 * All API calls use relative paths to automatically use the same domain as the frontend.
 */

/**
 * Get the current host header value from window.location
 * This is used for tenant resolution in the backend
 */
export function getHostHeader(): string {
  const host = window.location.hostname;
  const port = window.location.port ? `:${window.location.port}` : '';
  return `${host}${port}`;
}

/**
 * Redirect to signin page when 401 is received
 */
function redirectToSignin() {
  // Clear any stored session data
  if (typeof window !== 'undefined' && window.localStorage) {
    // Clear session storage (matches the key used in SessionStore)
    window.localStorage.removeItem('delight_session_store');
  }
  
  // Redirect to root path which will show login page
  // Check if we're already on a signin-related page to avoid redirect loops
  const currentPath = window.location.pathname;
  const isSigninPage = currentPath.includes('/login') || 
                       currentPath.includes('/signup') || 
                       currentPath.includes('/activate');
  
  if (!isSigninPage) {
    // Redirect to root - App.tsx will show Login component when !isAuthenticated
    // Use window.location.href for full page reload to ensure session state is cleared
    window.location.href = '/';
  }
}

/**
 * Make an API request with automatic host header and credentials
 */
export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const hostHeader = getHostHeader();
  
  // Determine if body is FormData - if so, don't set Content-Type (browser will set it with boundary)
  const isFormData = options.body instanceof FormData;
  
  const headers: HeadersInit = {
    'Host': hostHeader,
    ...options.headers,
  };
  
  // Only set Content-Type for non-FormData requests
  if (!isFormData && !options.headers?.['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  
  const response = await fetch(endpoint, {
    ...options,
    headers,
    credentials: 'include', // Automatically includes cookies
  });

  if (!response.ok) {
    // Handle 401 Unauthorized - redirect to signin page
    if (response.status === 401) {
      redirectToSignin();
      let errorMessage = 'Unauthorized. Please sign in again.';
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
      } catch {
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }
    
    let errorMessage = 'Request failed';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorData.error || errorMessage;
    } catch {
      errorMessage = response.statusText || errorMessage;
    }
    throw new Error(errorMessage);
  }

  // Handle empty responses
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }
  
  return {} as T;
}

/**
 * GET request helper
 */
export async function apiGet<T = any>(endpoint: string): Promise<T> {
  return apiRequest<T>(endpoint, { method: 'GET' });
}

/**
 * POST request helper
 */
export async function apiPost<T = any>(endpoint: string, body?: any): Promise<T> {
  return apiRequest<T>(endpoint, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * PUT request helper
 */
export async function apiPut<T = any>(endpoint: string, body?: any): Promise<T> {
  return apiRequest<T>(endpoint, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * PATCH request helper
 */
export async function apiPatch<T = any>(endpoint: string, body?: any): Promise<T> {
  return apiRequest<T>(endpoint, {
    method: 'PATCH',
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * DELETE request helper
 */
export async function apiDelete<T = any>(endpoint: string): Promise<T> {
  return apiRequest<T>(endpoint, { method: 'DELETE' });
}

