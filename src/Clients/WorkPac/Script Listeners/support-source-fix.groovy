// ********************************
// This groovy script updates a custom field of source based on the channel.
// Jira does not have the ability to add new channels so this allows for additional ones to be created.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

def customFields = Unirest.get("/rest/api/2/field").asObject(List).body

def sourceField = customFields.find { (it as Map).name == 'Source' } as Map
if (sourceField != null) {
    def sourceValue = (issue.fields[sourceField.id] as Map)?.value

    if (sourceValue == null || sourceValue == "None") {
        sourceValue = "None";

        def tempIssue = Unirest.get("/rest/api/2/issue/${issue.key}?properties=*all")
            .header('Content-Type', 'application/json')
            .asObject(Map)
        if (tempIssue.status == 200) {

            def channel = tempIssue.body.properties["request.channel.type"]?.value;
            def source = "None"
            switch (channel) {
                case "email": source = "Email";  break;
                case "jira": source = "Phone";  break;
                case "portal": source = "Portal";  break;
            }
            logger.debug("source = ${source}")

            if (source.toLowerCase() != sourceValue.toLowerCase()) {
                def result = Unirest.put("/rest/api/2/issue/${issue.key}?notifyUsers=false")
                    .header("Content-Type", "application/json")
                    .body([ fields: [ (sourceField.id): [ value : source ] ], ])
                    .asString()
                if (result.status >= 200 && result.status < 300) {
                    logger.info("Updated source to -> ${source}")
                } else {
                    logger.debug("Failed to change source to ${source}")
                }
            }
        }
    }
}

logger.trace("Event -> ${issue_event_type_name} - Completed")