// ********************************
// This groovy script automatically selects the risk level based on impact and experience
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event -> ${issue_event_type_name}")

// risk matrix
def riskCalculator = [
    [experience: "No Experience",                       impact: "Extensive / Widespread",   risk: "Critical"],
    [experience: "No Experience",                       impact: "Significant / Large",      risk: "Critical"],
    [experience: "No Experience",                       impact: "Moderate / Limited",       risk: "High"],
    [experience: "No Experience",                       impact: "Minor / Localized",        risk: "High"],
    [experience: "Previously Done With Issues",         impact: "Extensive / Widespread",   risk: "Critical"],
    [experience: "Previously Done With Issues",         impact: "Significant / Large",      risk: "Critical"],
    [experience: "Previously Done With Issues",         impact: "Moderate / Limited",       risk: "High"],
    [experience: "Previously Done With Issues",         impact: "Minor / Localized",        risk: "Medium"],
    [experience: "Limited Experience",                  impact: "Extensive / Widespread",   risk: "High"],
    [experience: "Limited Experience",                  impact: "Significant / Large",      risk: "High"],
    [experience: "Limited Experience",                  impact: "Moderate / Limited",       risk: "Medium"],
    [experience: "Limited Experience",                  impact: "Minor / Localized",        risk: "Medium"],
    [experience: "Experienced With Multiple Successes", impact: "Extensive / Widespread",   risk: "Medium"],
    [experience: "Experienced With Multiple Successes", impact: "Significant / Large",      risk: "Medium"],
    [experience: "Experienced With Multiple Successes", impact: "Moderate / Limited",       risk: "Low"],
    [experience: "Experienced With Multiple Successes", impact: "Minor / Localized",        risk: "Low"],
    [experience: "Routine",                             impact: "Extensive / Widespread",   risk: "Medium"],
    [experience: "Routine",                             impact: "Significant / Large",      risk: "Low"],
    [experience: "Routine",                             impact: "Moderate / Limited",       risk: "Low"],
    [experience: "Routine",                             impact: "Minor / Localized",        risk: "Low"]
]

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def riskLevelField = customFields.find { (it as Map).name == 'Risk' } as Map
def impactField = customFields.find { (it as Map).name == 'Impact' } as Map
def experienceField = customFields.find { (it as Map).name == 'Change Implementation Experience' } as Map

def updateRequired = null

if (riskLevelField != null && impactField != null && experienceField != null) {

    if (issue_event_type_name == "issue_created") {
        // potentially
        updateRequired = true
    } else if (issue_event_type_name != "issue_created") {
        logger.info("Change log -> ${changelog}")
        updateRequired = changelog?.items.find {
            it['fieldId'] == riskLevelField.id ||
            it['fieldId'] == impactField.id ||
            it['fieldId'] == experienceField.id
        }
    }

    if (updateRequired == null) {
        logger.info("No update required.")
        logger.trace("Event -> ${issue_event_type_name} -> Completed")
        return
    }

    def impactValue = (issue.fields[impactField.id] as Map)?.value
    def experienceValue = (issue.fields[experienceField.id] as Map)?.value

    // find the risk level based on the experience and impact - look up the risk matrix
    def calculatedRiskLevel = (riskCalculator.find { it.experience == experienceValue && it.impact == impactValue })?.risk
    logger.debug("Risk calculation -> ${impactValue} x ${experienceValue} = ${calculatedRiskLevel}")

    if (calculatedRiskLevel != null) {
        def riskLevelValue = (issue.fields[riskLevelField.id] as Map)?.value
        // if already set correctly then return
        if (riskLevelValue == calculatedRiskLevel) {
            logger.info("Risk level set correctly -> ${riskLevelValue}")
        } else {
            // otherwise update the risk level
            def result = Unirest.put("/rest/api/2/issue/${issue.key}?notifyUsers=false")
                .header("Content-Type", "application/json")
                .body([
                    fields: [
                        (riskLevelField.id): [ "value" : calculatedRiskLevel]
                    ],
                ])
                .asString()
            assert result.status >= 200 && result.status < 300
            if (result.status >= 200 && result.status < 300) {
                logger.info("Updated risk to -> ${calculatedRiskLevel}")
            } else {
                logger.error("Failed to change risk to ${calculatedRiskLevel}")
            }
        }
    }
}

logger.trace("Event -> ${issue_event_type_name} -> Completed")