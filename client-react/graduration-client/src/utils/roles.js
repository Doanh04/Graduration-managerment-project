// Faculty leadership has the same administrative access as Admin.
export const hasRole = (roles = [], requiredRole) => {
  const normalized = roles.map((role) => String(role).replace(/^ROLE_/, ''));
  return normalized.includes(requiredRole)
    || (requiredRole === 'ADMIN' && normalized.includes('FACULTY'));
};
