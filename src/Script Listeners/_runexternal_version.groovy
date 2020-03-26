// ********************************
// This groovy script
//
// Created By:
// Last Updated By:
//*********************************

def debug = (DEBUG_MODE == "true"); /* No access to logger levels -> workaround */ logger.metaClass.invokeMethod { name, args -> logger.metaClass.getMetaMethod(name, args)?.invoke(delegate, args); if ((name == "debug" || name == "trace") && debug == true) { def prefix = "** "; if (name == "trace") prefix = "## "; if (args.size() == 1) args[0] = prefix + args[0] else args = [prefix, *args]; logger.metaClass.getMetaMethod("info", args).invoke(logger, args); } }

def script = "https://raw.githubusercontent.com/SMExDigital/ScriptRunner-For-Jira-Cloud/master/WorkPac/Script%20Listeners/version_sync.groovy"

this.metaClass.mixin (new GroovyScriptEngine( '.' ).with {
    logger.debug("Loading script from ${script}")
    loadScriptByName(script)
})

def uni = new Expando(get : Unirest.&get, post : Unirest.&post, put : Unirest.&put, patch : Unirest.&patch, delete : Unirest.&delete, options : Unirest.&options, head : Unirest.&head )
run(uni, logger, webhookEvent, version)