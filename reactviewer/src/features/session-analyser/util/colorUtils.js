//Manages color generation for community IDs

const CHART_COLORS = [
  '#1f77b4', 
  '#ff7f0e', 
  '#2ca02c', 
  '#d62728', 
  '#9467bd', 
  '#8c564b', 
  '#e377c2', 
  '#7f7f7f', 
  '#bcbd22', 
  '#17becf'
];

export const generateColorForCommunityId = (communityId) => {
  // Use a hash of the community ID to generate consistent colors
  let hash = 0;
  for (let i = 0; i < communityId.length; i++) {
    const char = communityId.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  
  return CHART_COLORS[Math.abs(hash) % CHART_COLORS.length];
};