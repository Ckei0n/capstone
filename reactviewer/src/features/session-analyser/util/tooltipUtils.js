//tooltip creation and formatting

import * as d3 from 'd3';

export const createTooltip = () => {
  return d3.select("body").append("div")
    .attr("class", "d3-tooltip")
    .style("position", "absolute")
    .style("visibility", "hidden")
    .style("background", "rgba(0, 0, 0, 0.8)")
    .style("color", "white")
    .style("border-radius", "5px")
    .style("padding", "10px")
    .style("font-size", "12px")
    .style("max-width", "300px")
    .style("z-index", "1000");
};

export const formatTooltipContent = (d, communityId) => {
  const dateStr = d.singaporeDate || d.dateString || 'N/A';
  
  return `
    <strong>Date:</strong> ${dateStr}<br/>
    <strong>Community ID:</strong> ${communityId}<br/>
    <strong>Snort Hits:</strong> ${d.hitCount}
  `;
};

export const cleanupTooltips = () => {
  d3.selectAll(".d3-tooltip").remove();
};