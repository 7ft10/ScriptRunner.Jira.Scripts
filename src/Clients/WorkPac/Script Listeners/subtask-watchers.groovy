// ********************************
// This groovy script ass the watchers of the parent as watchers of the sub-tasks
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

if (!issue.fields.issuetype.subtask) {
    return
}

def watchers = [user.accountId]
if (issue.fields.reporter) {
    watchers.push(issue.fields.reporter.accountId)
}
if (issue.fields.assignee) {
    watchers.push(issue.fields.assignee.accountId)
}

watchers.unique().each { String watcher ->
    try {
        def result = Unirest.post("/rest/api/2/issue/${issue.fields.parent.key}/watchers")
            .header('Content-Type', 'application/json')
            .body("\"${watcher}\"")
            .asString()
        if (result.status >= 200 && result.status < 300) {
            logger.info("Watch added -> ${watcher}")
        } else {
            logger.debug("Unable to add watch -> ${watcher}")
        }
    } catch (Exception ex) {
        logger.debug("Unable to add watch -> ${watcher}")
    }
}

logger.trace("Event -> ${issue_event_type_name} -> Completed")