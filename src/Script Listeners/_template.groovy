// ********************************
// This groovy script
//
// Created By:
// Last Updated By:
//*********************************

// to encode url params -> io.github.openunirest.http.utils.URLParamEncoder.encode

def debug = (DEBUG_MODE == "true"); /* No access to logger levels -> workaround */ logger.metaClass.invokeMethod { name, args -> logger.metaClass.getMetaMethod(name, args)?.invoke(delegate, args); if ((name == "debug" || name == "trace") && debug == true) { def prefix = "** "; if (name == "trace") prefix = "## "; if (args.size() == 1) args[0] = prefix + args[0] else args = [prefix, *args]; logger.metaClass.getMetaMethod("info", args).invoke(logger, args); } }
logger.trace("Event -> ${issue_event_type_name}")

def customFields = Unirest.get("/rest/api/2/field")
    .asObject(List)
    .body

def priorityValue = issue.priority?.name
def approversField = customFields.find { (it as Map).name == 'Change Approvers' } as Map

logger.trace("Event -> ${issue_event_type_name} - Completed")