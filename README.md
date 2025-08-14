# **Project Details**

This application implements a SPA (single page app) to allow users to import GZIP folders with raw json data into an OpenSearch node and display a summary of network logs from a dashboard. The dashboard uses D3.js to allow users to specify a date of interest, show the total number of sessions and the number of snort hits. The dashboard will visualize if there is a trend of snort hits in a period of time based on a particular network session. An Nginx reverse proxy and Keycloak helps provide basic security with authentication and authorization. 
