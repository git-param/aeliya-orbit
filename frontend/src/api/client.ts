export type CurrentUser = {
  id: number;
  username: string;
  role: string;
  permissions: string[];
};

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) { super(message); this.status = status; }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (init.method && !['GET', 'HEAD'].includes(init.method.toUpperCase())) {
    const csrf = await fetch('/api/auth/csrf', { credentials: 'include' });
    if (!csrf.ok) throw new ApiError(csrf.status, 'Unable to initialize request security.');
    headers.set('X-XSRF-TOKEN', (await csrf.json()).token);
  }
  const response = await fetch(path, { ...init, headers, credentials: 'include' });
  if (!response.ok) {
    if (response.status === 401 && path !== '/api/auth/login') window.dispatchEvent(new Event('auth-expired'));
    throw new ApiError(response.status, response.status === 401 ? 'Session expired or invalid credentials.' : 'Request failed.');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  me: () => request<CurrentUser>('/api/me'),
  login: (username: string, password: string) => request<void>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ username, password }),
  }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  health: () => request<{status: string; service: string}>('/api/health'),
};

export type Employee = {
  id: number; employeeCode: string; fullName: string; workEmail: string;
  designation: string | null; department: string | null; level: string | null;
  designationId: number | null; departmentId: number | null; levelId: number | null;
  reportingManagerId: number | null; active: boolean; pictureUrl: string;
  address?: string | null; phone?: string | null; dateOfBirth?: string | null; dateOfJoining: string;
};
export type WorkdaySettings = {
  timezone: string; workStart: string; workEnd: string;
  breakAllowanceMinutes: number; weeklyOffDays: string[];
};
export type BreakStatus = {
  consumedSeconds: number; remainingSeconds: number;
  allowanceMinutes: number; activeSince: string | null;
};
export type Birthday = { employeeName: string; monthDay: string; today: boolean; nextOccurrence: string };
export type Holiday = { date: string; name: string };
export type Option = { id: number; kind: string; name: string };
export type EmployeeInput = {
  employeeCode: string; fullName: string; workEmail: string; dateOfJoining: string;
  address?: string; phone?: string; dateOfBirth?: string;
  departmentId?: number | null; designationId?: number | null; levelId?: number | null;
  reportingManagerId?: number | null;
};
export const hrApi = {
  ownEmployee: () => request<Employee>('/api/employees/me'),
  employeeCount: () => request<{activeEmployees: number}>('/api/employees/count'),
  employees: (page = 0, search = '') => request<{content: Employee[]; totalPages: number; totalElements: number}>(`/api/employees?page=${page}&size=20&search=${encodeURIComponent(search)}`),
  createEmployee: (input: EmployeeInput) => request<Employee>('/api/employees', {method: 'POST', body: JSON.stringify(input)}),
  employee: (id: number) => request<Employee>(`/api/employees/${id}`),
  updateEmployee: (id: number, input: EmployeeInput) => request<Employee>(`/api/employees/${id}`, {method: 'PUT', body: JSON.stringify(input)}),
  uploadPicture: (id: number, file: File) => {const body = new FormData(); body.set('file', file); return request<void>(`/api/employees/${id}/picture`, {method: 'POST', body});},
  deactivateEmployee: (id: number) => request<void>(`/api/employees/${id}/deactivate`, {method: 'POST'}),
  options: (kind: string) => request<Option[]>(`/api/organization/options?kind=${encodeURIComponent(kind)}`),
  settings: () => request<WorkdaySettings>('/api/workday/settings'),
  holidays: () => request<Holiday[]>('/api/workday/holidays?days=45'),
  birthdays: () => request<Birthday[]>('/api/dashboard/birthdays'),
  breakToday: () => request<BreakStatus>('/api/breaks/today'),
  startBreak: () => request<BreakStatus>('/api/breaks/start', {method: 'POST'}),
  endBreak: () => request<BreakStatus>('/api/breaks/end', {method: 'POST'}),
};
