// ********************************
// This python script keeps components from the master project
// in sync with the tech projects.
//
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************

logger.trace("Event info: Scheduled Job - Component Sync")

def projectCategories = ((List<Map>) Unirest.get("/rest/api/2/projectCategory")
    .header('Content-Type', 'application/json')
    .asObject(List)
    .body)
    .collect {
        [
            "id": it.id.toString(),
            "name": it.name.toString()?: "",
        ]}

def masterCategoryId = (projectCategories.find { (it as Map).name == 'Technology Portfolio' } as Map).id

def masterProjectKey = ((Map)get("/rest/api/2/project/search?orderBy=category&expand=projectKeys&categoryId=${masterCategoryId}")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body)
    .values
    .collect  { it.key.toString() }[0]
logger.debug("Master project: ${masterProjectKey}")

def masterComponents = ((List<Map<String, Map>>) Unirest.get("/rest/api/2/project/${masterProjectKey}/components")
    .header('Content-Type', 'application/json')
    .asObject(List)
    .body)
    .collect {
    [
        "id": it.id.toString(),
        "name": it.name.toString()?: "",
        "description" : it.description.toString()?: ""
    ]}

def squadCategoryId = (projectCategories.find { (it as Map).name == 'Technology Squad' } as Map).id
def targetProjectCodes = ((Map)get("/rest/api/2/project/search?orderBy=category&expand=projectKeys&categoryId=${squadCategoryId}")
    .header('Content-Type', 'application/json')
    .asObject(Map)
    .body)
    .values
    .collect { it.key.toString() }

logger.debug("Target Project Codes: ${targetProjectCodes}")

targetProjectCodes.each { String projectCode ->
    def projectComponents = ((List<Map<String, Map>>) Unirest.get("/rest/api/2/project/${projectCode}/components")
        .header('Content-Type', 'application/json')
        .asObject(List)
        .body)
        .collect {
        [
            "id": it.id.toString(),
            "name": it.name.toString()?: "",
            "description" : it.description.toString()?: ""
        ]}

    masterComponents.each { Map masterComponent ->
        def projectComponent = (Map) projectComponents.find { it.name == masterComponent.name }
        def descriptionPrefix = "Please see master component list for details -> id: ${masterComponent.id}"
        if (projectComponent == null) {
            // create
            def newComponent = (Map) masterComponent.clone()
            newComponent.put("project", projectCode)
            newComponent.put("description", descriptionPrefix)
            def result = Unirest.post("/rest/api/2/component")
                .header("Content-Type", "application/json")
                .body(newComponent)
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Created component: ${masterComponent.name}")
        } else {
            if (projectComponent.description == descriptionPrefix) {
                // ok
                logger.info("Component: ${masterComponent.name} ok")
            } else {
                // update
                def newComponent = (Map) projectComponent.clone()
                newComponent.put("description", descriptionPrefix)
                def result = Unirest.put("/rest/api/2/component/${projectComponent.id}")
                    .header("Content-Type", "application/json")
                    .body(newComponent)
                    .asString()
                assert result.status >= 200 && result.status < 300
                logger.info("Updated component: ${masterComponent.name}")
            }
            projectComponents.remove(projectComponent)
        }
    }

    projectComponents.each { Map pc ->
        def related = (Map) Unirest.get("/rest/api/3/component/${pc.id}/relatedIssueCounts")
            .header('Content-Type', 'application/json')
            .asObject(Map)
            .body

        if (related.issueCount == 0) {
            // delete
            def result = Unirest.delete("/rest/api/2/component/${pc.id}")
                .header("Content-Type", "application/json")
                .asString()
            assert result.status >= 200 && result.status < 300
            logger.info("Deleted Component: ${pc.name}")
        } else {
            if (pc.name.endsWith("- NOT IN MASTER - DELETE")) {
                // cannot delete issues attached
                logger.warn("Unable to delete component ${pc.name} -> name already updated")
            } else {
                // update name
                def newName = pc.name + "- NOT IN MASTER - DELETE"
                def result = Unirest.put("/rest/api/2/component/${pc.id}")
                    .header("Content-Type", "application/json")
                    .body(["name": newName])
                    .asString()
                assert result.status >= 200 && result.status < 300
                logger.info("Updated Component -> ${newName}")
            }
        }
    }
}

logger.trace("Event info: Scheduled Job - Component Sync - Completed")