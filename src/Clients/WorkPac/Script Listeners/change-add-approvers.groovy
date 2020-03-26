// ********************************
// This groovy script adds a members of a group to a issues approvers
// ** Note: I don't think this will actually be used by WorkPac
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

def customFields = Unirest.get("/rest/api/2/field").asObject(List).body

def approversField = customFields.find { (it as Map).name == 'Change Approvers' } as Map

def requesttypeField = customFields.find { (it as Map).name == 'Customer Request Type' } as Map
def requesttypeValue = (issue.fields[requesttypeField.id] as Map)?.requestType?.name

// if the request is a standard request
if (approversField != null && requesttypeValue != "Standard Change Request") {
    def newApprovers = []
    def changeGroup = io.github.openunirest.http.utils.URLParamEncoder.encode("changegroup-${requesttypeValue}")
    def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${changeGroup}")
        .asObject(Map)
        .body
    groupDetails?.users.items.each { Map grpuser ->
        if (user.accountId != grpuser.accountId) { /* can't approve your own */
            newApprovers.push(["id": grpuser.accountId])
        }
    }

    if (newApprovers.size() > 0) {
        def result = Unirest.put("/rest/api/2/issue/${issue.key}?notifyUsers=false")
            .header("Content-Type", "application/json")
            .body([ fields: [ (approversField.id): newApprovers ], ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Updated approvers to -> ${newApprovers}")
        } else {
            logger.error("Failed to change approvers to ${newApprovers}")
        }
    }
}

logger.trace("Event -> ${issue_event_type_name} - Completed")