// ********************************
// This groovy script adds all the users from a value stream group as watchers to the item
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

def customField = Unirest.get("/rest/api/2/field").asObject(List).body
    .find { (it as Map).name == 'Value Stream' } as Map
if (customField == null) return

if (issue_event_type_name != "issue_created") {
    def changed = changelog?.items.find { it['fieldId'] == customField.id }
    logger.debug("Changed item -> ${changed}")
    if (changed == null) return
}

def customfieldValues = (issue.fields[customField.id] as Map)
if (customfieldValues == null) return

customfieldValues.flatten().each { Map item ->
    def customfieldValue = item.value
    if (customfieldValue == "Unsure" || customfieldValue == null) return

    // find the value stream group - prefixed with tech-valuestream-
    def notificationGroup = "tech-valuestream-${customfieldValue.toLowerCase().replace(" ", "_").replace(",", "").replace("&", "")}".replace("__", "_")
    def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${notificationGroup}")
        .asObject(Map)
        .body

    groupDetails?.users.items.each { Map user ->
        def result = Unirest.post("/rest/api/2/issue/${issue.key}/watchers")
            .header('Content-Type', 'application/json')
            .body("\"${user.accountId}\"")
            .asString()
        if (result.status >= 200 && result.status < 300) {
            logger.info("Watch added -> ${user.displayName}")
        } else {
            logger.error("Unable to add watch -> ${user.displayName}")
        }
    }
}

logger.trace("Event -> ${issue_event_type_name} -> Completed")