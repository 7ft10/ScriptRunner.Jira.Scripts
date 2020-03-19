logger.trace("Event -> ${issue_event_type_name}")
def remotelinks = (List) Unirest.get("/rest/api/2/issue/${issue.key}/remotelink").asObject(List).body
def hasRemotelinks = false;
if (remotelinks == null || remotelinks == []) {
    logger.debug("No remote links")
} else {
    remotelinks.each { Map link ->
        if (link.relationship == "Wiki Page") {
            hasRemotelinks = true
            return
        }
    }
    if (hasRemotelinks == false) {
        logger.debug("No remote links to wiki pages")
    }
}
def customFields = Unirest.get("/rest/api/2/field").asObject(List).body
def hasConfluenceLinkField = customFields.find { (it as Map).name == 'Has Confluence Link' } as Map
def hasConfluenceLinkFieldValue = (issue.fields[hasConfluenceLinkField.id] as List)?.get(0)?.value
if (!hasRemotelinks)  {
    if (hasConfluenceLinkFieldValue == null) {
        logger.debug("No Update Required")
        logger.trace("Event -> ${issue_event_type_name} -> Completed")
        return
    }
    def result = Unirest.put("/rest/api/2/issue/${issue.key}?notifyUsers=false")
        .header("Content-Type", "application/json")
        .body([
            fields: [
                (hasConfluenceLinkField.id): null
            ],
        ])
        .asString()
    assert result.status >= 200 && result.status < 300
    if (result.status >= 200 && result.status < 300) {
        logger.info("Updated Has Confluence Link")
    } else {
        logger.error("Failed to change Has Confluence Link")
    }
} else {
    if (!(hasConfluenceLinkFieldValue == null || hasConfluenceLinkFieldValue != "Linked")) {
        logger.debug("No Update Required")
        logger.trace("Event -> ${issue_event_type_name} -> Completed")
        return
    }
    def result = Unirest.put("/rest/api/2/issue/${issue.key}?notifyUsers=false")
        .header("Content-Type", "application/json")
        .body([
            fields: [ (hasConfluenceLinkField.id): [ [ "value" : "Linked" ] ] ],
        ])
        .asString()
    assert result.status >= 200 && result.status < 300
    if (result.status >= 200 && result.status < 300) {
        logger.info("Updated Has Confluence Link")
    } else {
        logger.error("Failed to change Has Confluence Link")
    }
}
logger.trace("Event -> ${issue_event_type_name} -> Completed")