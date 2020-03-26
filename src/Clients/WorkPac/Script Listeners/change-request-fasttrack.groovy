// ********************************
// This groovy script automatically transitions the request to the next stage
// if the risk level and request types are low
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def riskLevelField = customFields.find { (it as Map).name == 'Risk' } as Map
def requesttypeField = customFields.find { (it as Map).name == 'Customer Request Type' } as Map

if (riskLevelField != null && requesttypeField != null) {

    def riskLevelValue = (issue.fields[riskLevelField.id] as Map)?.value
    def requesttypeValue = (issue.fields[requesttypeField.id] as Map)?.requestType?.name

    // if the change a standard request with low or medium risk and currently in backlog or to do?
    def changeRequired = (
                            requesttypeValue == "Standard Change Request"
                            && ["Backlog", "To Do"].contains(issue.fields.status.name)
                            && ["Low", "Medium"].contains(riskLevelValue)
                        )

    // if no change required return
    if (!changeRequired) {
        logger.info("No transition required -> ${requesttypeValue}, ${issue.fields.status.name}, ${riskLevelValue}")
        logger.trace("Event -> ${issue_event_type_name} -> Completed")
        return
    }

    // otherwise find the next transition to move to
    def trans = (Unirest.get("/rest/api/2/issue/${issue.key}/transitions")
        .asObject(Map)
        .body)
        .transitions
        .find { (issue.fields.status.name == "Backlog" && it.to.name == "To Do") || (issue.fields.status.name == "To Do" && it.to.name == "Awaiting approval") }

    // if there is a valid transition
    if (trans != null) {
        // post a request to move to the next transition
        def result = Unirest.post("/rest/api/2/issue/${issue.key}/transitions")
            .header("Content-Type", "application/json")
            .body([
                "transition": [
                    "id": trans.id
                ]
            ])
            .asString()
        assert result.status >= 200 && result.status < 300
        if (result.status >= 200 && result.status < 300) {
            logger.info("Transitioned to -> ${trans}")
        } else {
            logger.error("Failed to transition to ${trans}")
        }

        try {
            if (issue.fields.status.name == "Backlog") {
                // if in the backlog then write a note saying that this has been fast-tracked automatically
                def comment = Unirest.post("/rest/servicedeskapi/request/${issue.key}/comment?notifyUsers=false")
                    .header("Content-Type", "application/json")
                    .body([
                        public: true,
                        body: "Fast tracked low risk standard change request. Please ensure that all relevant documentation is attached."
                    ])
                    .asString()
                if (comment.status >= 200 && comment.status < 300) {
                    logger.info("Created comment")
                }
            }
        } catch (Exception ex) {
            logger.info("Unable to create comment")
        }
    }
}

logger.trace("Event -> ${issue_event_type_name} -> Completed")