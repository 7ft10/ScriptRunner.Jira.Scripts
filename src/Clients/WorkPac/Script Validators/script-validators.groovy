// ********************************
// Change Request Priority Validator
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************
issue.priority.name.match('^(Emergency|Major|Normal|Standard)$') != null

// ********************************
// Support Priority Validator
// Created By: Mike Burns
// Last Updated By: Mike Burns
//*********************************
issue.priority.name.match('^(Must|Should|Could|Not Set|)$') == null