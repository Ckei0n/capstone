# **Project Details**

This application implements a SPA (single page app) to allow users to import GZIP folders with raw json data into an OpenSearch node and display a summary of network logs from a dashboard. The dashboard uses D3.js to allow users to specify a date of interest, show the total number of sessions and the number of snort hits. The dashboard will visualize if there is a trend of snort hits in a period of time based on a particular network session. An Nginx reverse proxy and Keycloak helps provide basic security with authentication and authorization. 

# **Requirements**

Delivery 1:  
develop a tool that reads the NEMOS network logs, and insert the data into Opensearch.  

Delivery 2:  
write a simple SPA (single page app) that displays the summary of the network logs found in the Opensearch. This SPA dashboard should: allow user to specify a date range of interest; show the number of total sessions; show the number of hits (i.e. with snort sid)  

Bonus 1:  
Secure the SPA with basic authentication or keycloak using spring security  

Bonus 2:  
Able to use javascript library (e.g. D3.js) to give visual display (line, pie chart, heat map, visual alerts) of example snort hits over time  

(Internal)
1) have proper documentation
2) have unit tests
3) have CI
4) properly git controlled


# **Extra**

1) Implemented HTTPS on most routes.
2) Using Cypress to showcase e2e testing.
3) Using domain names instead of localhost to mirror prod env.
4) Implemented nginx reverse proxy



