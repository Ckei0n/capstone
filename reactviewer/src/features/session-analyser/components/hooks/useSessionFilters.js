
export const useSessionFilters = (sessions) => {
  // Just return the sessions as-is with safety check
  if (!sessions || !Array.isArray(sessions)) {
    console.warn("useSessionFilters: sessions is not an array", sessions);
    return [];
  }
  return sessions;
};