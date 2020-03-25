logger.trace("Event -> ${issue_event_type_name}")

def defaultSupportTeam = "Level 1"
def msteams = [
    "Level 3 - Apps": "https://outlook.office.com/webhook/9e4ef040-6277-46ec-b9ca-8d7123b910b2@2f2894cf-8f13-49bc-85d6-4104d2c1e255/IncomingWebhook/73fdb409a5ed411bbb138257378f72e2/1cb04481-1946-4319-ab1c-c623d2938b4b",
]

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body
def customField = customFields.find { (it as Map).name == 'Support Team' } as Map
if (customField == null) {
    logger.debug("Custom field not found")
    logger.trace("Event -> ${issue_event_type_name} - Completed")
    return
}

if (issue_event_type_name != "issue_created") {
    def teamChanged = changelog?.items.find { it['fieldId'] == customField.id }
    logger.info("Change item -> ${teamChanged}")
    if (teamChanged == null) {
        logger.debug("Support team was not updated")
        logger.trace("Event -> ${issue_event_type_name} - Completed")
        return
    }
    if (teamChanged.fromString == null && teamChanged.toString == defaultSupportTeam) {
        logger.debug("Support team was defaulting to ${defaultSupportTeam} no update required")
        logger.trace("Event -> ${issue_event_type_name} - Completed")
        return
    }
}

def supportTeamValue = (issue.fields[customField.id] as Map)?.value
if (supportTeamValue == null) {
    logger.debug("Support team has no value")
    logger.trace("Event -> ${issue_event_type_name} - Completed")
    return
}
if (issue_event_type_name == "issue_created" && supportTeamValue == defaultSupportTeam) {
    logger.debug("Default support team selected")
    logger.trace("Event -> ${issue_event_type_name} - Completed")
    return
}

def webhookUrl = msteams[supportTeamValue]
if (webhookUrl != null) {
    def valueSteamField = customFields.find { (it as Map).name == 'Value Stream' } as Map
    def valueStream = (issue.fields[valueSteamField.id] as Map)?.value
    if (valueStream == null) {
        valueStream = "Not set"
    }
    def hookresult = Unirest.post(webhookUrl)
        .header("Content-Type", "application/json")
        .body([
            "@context": "https://schema.org/extensions",
            "@type": "MessageCard",
            "themeColor": "066862",
            "title": "${issue.key} has been escalated to " + supportTeamValue,
            "text": "<pre>${issue.key} - ${issue.fields.summary}\n<b>${issue.fields.priority?.name}</b>\nReported by ${issue.fields.reporter?.displayName}\nValue Stream: ${valueStream}</pre>",
            "potentialAction": [
                [
                    "@type": "OpenUri",
                    "name": "Open item in Jira",
                    "targets": [
                        [ "os": "default", "uri": "https://workpactech.atlassian.net/browse/${issue.key}" ]
                    ]
                ]
            ]
        ])
        .asString()
    if (hookresult.status >= 200 && hookresult.status < 300) {
        logger.info("Notification sent to ms teams")
    }
}
else
{
    def comment = "'*** Automated message ***  \r\n This issue has been escalated to ${supportTeamValue}."
    try {
        def notificationGroup = io.github.openunirest.http.utils.URLParamEncoder.encode("supportlevel-${supportTeamValue}")
        def groupDetails = Unirest.get("/rest/api/2/group?expand=users&groupname=${notificationGroup}")
            .asObject(Map)
            .body
        if (groupDetails?.users.items.size() > 0) {
            comment += " FYI"
            groupDetails?.users.items.each { Map user ->
                comment += " [${user.displayName}|~accountid:${user.accountId}] "
            }
        }
    } catch (Exception ex) {
        logger.debug("Unable to get support-level group details. ${ex}")
    }
    try {
        def result = Unirest.post("/rest/servicedeskapi/request/${issue.key}/comment?notifyUsers=false")
            .header("Content-Type", "application/json")
            .body([
                public: false,
                body: comment
            ])
            .asString()
        if (result.status >= 200 && result.status < 300) {
            logger.info("Created comment -> ${comment}")
        }
    } catch (Exception ex) {
        logger.debug("Cannot create - most likely the item is still being updated. ${ex}")
    }
}

logger.trace("Event -> ${issue_event_type_name} - Completed")