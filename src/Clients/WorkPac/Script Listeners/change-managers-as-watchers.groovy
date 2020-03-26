// ********************************
// This groovy script adds all change management users as watchers to the change requests
// this allows them to be notified of any changes.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

// get all the current watchers
def currentWatchers = (Unirest.get("/rest/api/latest/issue/${issue.key}/watchers")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body).watchers?.collect({ it.accountId })

def watchers = []

// get all the change management group members
def changeManagementGroup = io.github.openunirest.http.utils.URLParamEncoder.encode("Change Management")
def changeManagers = Unirest.get("/rest/api/latest/group?expand=users&groupname=${changeManagementGroup}")
    .asObject(Map)
    .body

// get a list of watchers that aren't already on them
changeManagers?.users.items.each { Map u ->
    if (!currentWatchers.contains(u.accountId)) {
        watchers.push(u.accountId)
    }
}

// if there are any watchers not currently assigned
watchers.unique().each { String watcher ->
    try {
        // add each watcher individually to the issue
        def result = Unirest.post("/rest/api/latest/issue/${issue.key}/watchers")
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