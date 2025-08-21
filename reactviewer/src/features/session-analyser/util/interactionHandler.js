//Handles all user interactions with the timeseries chart

import * as d3 from 'd3';
import { createTooltip, formatTooltipContent } from './tooltipUtils';

export class InteractionHandler {
  constructor(onDayClick, originalData) {
    this.onDayClick = onDayClick; //call when data point is clicked
    this.originalData = originalData; //original daily data from API response
    this.tooltip = createTooltip();
  }

  // Handles mouse over events on data points
  handleMouseOver(element, event, d, communityId) {
    // enlarge data point and make it more visible
    d3.select(element)
      .attr("r", 5)
      .attr("stroke-width", 2)
      .style("opacity", 1);

    // Show tooltip with format 
    this.tooltip
      .style("visibility", "visible")
      .html(formatTooltipContent(d, communityId))
      .style("left", (event.pageX + 10) + "px")
      .style("top", (event.pageY - 10) + "px");
  }

  // Handles mouse move events to update tooltip position
  handleMouseMove(event) {
    this.tooltip
      .style("left", (event.pageX + 10) + "px")
      .style("top", (event.pageY - 10) + "px");
  }

  // Handles mouse out events when user stops hovering
  handleMouseOut(element) {
    // Restore data point to normal appearance
    d3.select(element)
      .attr("r", 3)
      .attr("stroke-width", 1.5)
      .style("opacity", 0.9);

      //hide tooltip
    this.tooltip.style("visibility", "hidden");
  }

  // Handles click events on data points for drilling down into details, main interaction
  handleDayClick(dayData, communityId) {
    // Don't process clicks on data points with no hits
    if (!this.onDayClick || dayData.hitCount <= 0) return;

    // Find the original API data for this day
    const originalDayData = this.findOriginalDayData(dayData);
    
    // Create data object with filtering information
    const serializedDayData = this.createSerializedDayData(
      originalDayData, 
      dayData, 
      communityId
    );
    this.onDayClick(serializedDayData);
    
  }

  // Finds original day data from API response matching the clicked chart data
  findOriginalDayData(dayData) {
    return this.originalData.find(d => 
      (d.singaporeDate || d.date) === dayData.singaporeDate || 
      (d.singaporeDate || d.date) === dayData.dateString
    );
  }

  // Create day data object for session details display
  createSerializedDayData(originalDayData, dayData, communityId) {
    // Filter sample sessions to only show the clicked Community ID
    const filteredSampleSessions = (originalDayData.sampleSessions || [])
      .filter(session => session.communityId === communityId);
    
    return {
      ...originalDayData, // Spread all original data
      date: originalDayData.singaporeDate || originalDayData.date, // Standardized date
      hitCount: dayData.hitCount,
      sampleSessions: filteredSampleSessions, // Pre-filtered sample sessions
      selectedCommunityId: communityId, // Which Community ID was selected
      hasMoreSessions: originalDayData.hasMoreSessions //  if more sessions can be loaded
    };
  }
}