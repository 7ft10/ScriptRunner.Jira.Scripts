// ********************************
// This groovy script
//
// Created By:
// Last Updated By:
//*********************************

def debug = (DEBUG_MODE == "true"); /* No access to logger levels -> workaround */ logger.metaClass.invokeMethod { name, args -> logger.metaClass.getMetaMethod(name, args)?.invoke(delegate, args); if ((name == "debug" || name == "trace") && debug == true) { def prefix = "** "; if (name == "trace") prefix = "## "; if (args.size() == 1) args[0] = prefix + args[0] else args = [prefix, *args]; logger.metaClass.getMetaMethod("info", args).invoke(logger, args); } }

def event_name = "THIS IS NOT SUPPLIED FOR SCHEDULED JOBS"
logger.trace("Event -> ${event_name}")

def query = 'JQL'

def searchReq = Unirest.get("/rest/api/2/search")
    .queryString("jql", query)
    .queryString("fields", "key")
    .asObject(Map)
assert searchReq.status == 200
Map searchResult = searchReq.body

searchResult.issues.each { Map issue ->
    logger.debug("Performed action on issue")
}

logger.info("Iterated over ${searchResult.issues.size()} issues")

logger.trace("Event -> ${event_name} - Completed")