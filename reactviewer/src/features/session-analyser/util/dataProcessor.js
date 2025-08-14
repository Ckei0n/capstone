//Handles data transformation and processing

import { generateColorForCommunityId } from './colorUtils';

export const processDailyData = (dailyData) => {
  if (!dailyData || !Array.isArray(dailyData)) {
    return { processedData: [], communityData: [] };
  }
  
  // Filter for days with hits and sort by date
  const daysWithHits = dailyData
    .filter(d => d.hitCount && d.hitCount > 0)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
  
  // Process daily data
  const processedData = daysWithHits.map(d => ({
    date: new Date(d.timestamp),
    hitCount: d.hitCount,
    dateString: d.date,
    singaporeDate: d.singaporeDate || d.date,
    communityIds: d.communityIds || [],
    communityIdHitCounts: d.communityIdHitCounts || {},
    sids: d.sids || [],
    sampleSessions: d.sampleSessions || []
  }));
  
  // Create community ID data for multi-line chart
  const communityData = processCommunityDataComplete(daysWithHits);
  
  return { processedData, communityData };
};

export const processCommunityDataComplete = (dailyData) => {
  if (!dailyData || dailyData.length === 0) return [];
  
  const allDates = getAllDates(dailyData);
  const allCommunityIds = getAllCommunityIds(dailyData);
  
  return createCommunityLines(allDates, allCommunityIds, dailyData);
};

const getAllDates = (dailyData) => {
  return dailyData
    .map(day => ({
      date: new Date(day.timestamp),
      dateString: day.date,
      singaporeDate: day.singaporeDate || day.date
    }))
    .sort((a, b) => a.date - b.date);
};

const getAllCommunityIds = (dailyData) => {
  const allCommunityIds = new Set();
  
  dailyData.forEach(day => {
    if (day.communityIdHitCounts) {
      Object.keys(day.communityIdHitCounts).forEach(id => allCommunityIds.add(id));
    } else if (day.communityIds) {
      day.communityIds.forEach(id => allCommunityIds.add(id));
    }
  });
  
  return allCommunityIds;
};

const createCommunityLines = (allDates, allCommunityIds, dailyData) => {
  const communityLines = [];
  
  allCommunityIds.forEach(communityId => {
    const dataPoints = createDataPoints(allDates, communityId, dailyData);
    
    if (dataPoints.some(p => p.hitCount > 0)) {
      communityLines.push({
        communityId: communityId,
        dataPoints: dataPoints,
        color: generateColorForCommunityId(communityId)
      });
    }
  });
  
  return communityLines;
};

const createDataPoints = (allDates, communityId, dailyData) => {
  return allDates.map(dateInfo => {
    const dayData = dailyData.find(day => 
      (day.singaporeDate || day.date) === dateInfo.singaporeDate
    );
    
    let communityCount = 0;
    if (dayData) {
      communityCount = getCommunityCount(dayData, communityId);
    }
    
    return {
      date: dateInfo.date,
      hitCount: communityCount,
      singaporeDate: dateInfo.singaporeDate,
      dateString: dateInfo.dateString
    };
  });
};

const getCommunityCount = (dayData, communityId) => {
  if (dayData.communityIdHitCounts && dayData.communityIdHitCounts[communityId]) {
    return dayData.communityIdHitCounts[communityId];
  } else if (dayData.communityIds && dayData.communityIds.includes(communityId)) {
    // Fallback: estimate if exact counts not available
    return Math.ceil(dayData.hitCount / dayData.communityIds.length);
  }
  return 0;
};