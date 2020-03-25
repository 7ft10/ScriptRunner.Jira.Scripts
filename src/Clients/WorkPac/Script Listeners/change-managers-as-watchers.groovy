logger.trace("Event -> ${issue_event_type_name}")

def currentWatchers = (Unirest.get("/rest/api/latest/issue/${issue.key}/watchers")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body).watchers?.collect({ it.accountId })

def watchers = []
def changeManagementGroup = io.github.openunirest.http.utils.URLParamEncoder.encode("Change Management")
def changeManagers = Unirest.get("/rest/api/latest/group?expand=users&groupname=${changeManagementGroup}")
    .asObject(Map)
    .body
changeManagers?.users.items.each { Map u ->
    if (!currentWatchers.contains(u.accountId)) {
        watchers.push(u.accountId)
    }
}
watchers.unique().each { String watcher ->
    try {
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