//mouse events and user interactions

import * as d3 from 'd3';
import { createTooltip, formatTooltipContent } from './tooltipUtils';

export class InteractionHandler {
  constructor(onDayClick, originalData) {
    this.onDayClick = onDayClick;
    this.originalData = originalData;
    this.tooltip = createTooltip();
  }

  handleMouseOver(element, event, d, communityId) {
    d3.select(element)
      .attr("r", 5)
      .attr("stroke-width", 2)
      .style("opacity", 1);

    this.tooltip
      .style("visibility", "visible")
      .html(formatTooltipContent(d, communityId))
      .style("left", (event.pageX + 10) + "px")
      .style("top", (event.pageY - 10) + "px");
  }

  handleMouseMove(event) {
    this.tooltip
      .style("left", (event.pageX + 10) + "px")
      .style("top", (event.pageY - 10) + "px");
  }

  handleMouseOut(element) {
    d3.select(element)
      .attr("r", 3)
      .attr("stroke-width", 1.5)
      .style("opacity", 0.9);

    this.tooltip.style("visibility", "hidden");
  }

  handleDayClick(dayData, communityId) {
    if (!this.onDayClick || dayData.hitCount <= 0) return;

    const originalDayData = this.findOriginalDayData(dayData);
    
    if (originalDayData) {
      const serializedDayData = this.createSerializedDayData(
        originalDayData, 
        dayData, 
        communityId
      );
      this.onDayClick(serializedDayData);
    } else {
      // Fallback
      const fallbackData = this.createFallbackDayData(dayData, communityId);
      this.onDayClick(fallbackData);
    }
  }

  findOriginalDayData(dayData) {
    return this.originalData.find(d => 
      (d.singaporeDate || d.date) === dayData.singaporeDate || 
      (d.singaporeDate || d.date) === dayData.dateString
    );
  }

  createSerializedDayData(originalDayData, dayData, communityId) {
    const filteredSampleSessions = (originalDayData.sampleSessions || [])
      .filter(session => session.communityId === communityId);
    
    return {
      ...originalDayData,
      date: originalDayData.singaporeDate || originalDayData.date,
      hitCount: dayData.hitCount,
      sampleSessions: filteredSampleSessions,
      selectedCommunityId: communityId,
      hasMoreSessions: originalDayData.hasMoreSessions
    };
  }

  createFallbackDayData(dayData, communityId) {
    return {
      date: dayData.singaporeDate || dayData.dateString,
      hitCount: dayData.hitCount,
      singaporeDate: dayData.singaporeDate || dayData.dateString,
      sampleSessions: [],
      selectedCommunityId: communityId,
      hasMoreSessions: true
    };
  }
}