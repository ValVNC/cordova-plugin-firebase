var exec = require('cordova/exec');

exports.initCrashlytics = function (success, error) {
  exec(success, error, "FirebasePlugin", "initCrashlytics", []);
};

exports.initAnalytics = function (success, error) {
  exec(success, error, "FirebasePlugin", "initAnalytics", []);
};

exports.initPerformance = function (success, error) {
  exec(success, error, "FirebasePlugin", "initPerformance", []);
};

exports.getVerificationID = function (number, success, error) {
  exec(success, error, "FirebasePlugin", "getVerificationID", [number]);
};

exports.getInstanceId = function (success, error) {
  exec(success, error, "FirebasePlugin", "getInstanceId", []);
};

exports.getId = function (success, error) {
  exec(success, error, "FirebasePlugin", "getId", []);
};

exports.getToken = function (success, error) {
  exec(success, error, "FirebasePlugin", "getToken", []);
};

exports.onAPNSToken = function (success, error) {
  exec(success, error, "FirebasePlugin", "onAPNSToken", []);
};

exports.onNotificationOpen = function (success, error) {
  exec(success, error, "FirebasePlugin", "onNotificationOpen", []);
};

exports.onNotificationReceived = function (success, error) {
  exec(success, error, "FirebasePlugin", "onNotificationReceived", []);
};

exports.onNotificationMarkAsRead = function (success, error) {
  exec(success, error, "FirebasePlugin", "onNotificationMarkAsRead", []);
};

exports.onTokenRefresh = function (success, error) {
  exec(success, error, "FirebasePlugin", "onTokenRefresh", []);
};

exports.grantPermission = function (success, error) {
  exec(success, error, "FirebasePlugin", "grantPermission", []);
};

exports.hasPermission = function (success, error) {
  exec(success, error, "FirebasePlugin", "hasPermission", []);
};

exports.setBadgeNumber = function (number, success, error) {
  exec(success, error, "FirebasePlugin", "setBadgeNumber", [number]);
};

exports.getBadgeNumber = function (success, error) {
  exec(success, error, "FirebasePlugin", "getBadgeNumber", []);
};

exports.subscribe = function (topic, success, error) {
  exec(success, error, "FirebasePlugin", "subscribe", [topic]);
};

exports.unsubscribe = function (topic, success, error) {
  exec(success, error, "FirebasePlugin", "unsubscribe", [topic]);
};

exports.unregister = function (success, error) {
  exec(success, error, "FirebasePlugin", "unregister", []);
};

exports.logEvent = function (name, params, success, error) {
  exec(success, error, "FirebasePlugin", "logEvent", [name, params]);
};

exports.logError = function (message, success, error) {
  exec(success, error, "FirebasePlugin", "logError", [message]);
};

exports.setScreenName = function (name, success, error) {
  exec(success, error, "FirebasePlugin", "setScreenName", [name]);
};

exports.setUserId = function (id, success, error) {
  exec(success, error, "FirebasePlugin", "setUserId", [id]);
};

exports.setUserProperty = function (name, value, success, error) {
  exec(success, error, "FirebasePlugin", "setUserProperty", [name, value]);
};

exports.activateFetched = function (success, error) {
  exec(success, error, "FirebasePlugin", "activateFetched", []);
};

exports.fetch = function (cacheExpirationSeconds, success, error) {
  var args = [];
  if (typeof cacheExpirationSeconds === 'number') {
    args.push(cacheExpirationSeconds);
  } else {
    error = success;
    success = cacheExpirationSeconds;
  }
  exec(success, error, "FirebasePlugin", "fetch", args);
};

exports.getByteArray = function (key, namespace, success, error) {
  var args = [key];
  if (typeof namespace === 'string') {
    args.push(namespace);
  } else {
    error = success;
    success = namespace;
  }
  exec(success, error, "FirebasePlugin", "getByteArray", args);
};

exports.getValue = function (key, namespace, success, error) {
  var args = [key];
  if (typeof namespace === 'string') {
    args.push(namespace);
  } else {
    error = success;
    success = namespace;
  }
  exec(success, error, "FirebasePlugin", "getValue", args);
};

exports.getInfo = function (success, error) {
  exec(success, error, "FirebasePlugin", "getInfo", []);
};

exports.setConfigSettings = function (settings, success, error) {
  exec(success, error, "FirebasePlugin", "setConfigSettings", [settings]);
};

exports.setDefaults = function (defaults, namespace, success, error) {
  var args = [defaults];
  if (typeof namespace === 'string') {
    args.push(namespace);
  } else {
    error = success;
    success = namespace;
  }
  exec(success, error, "FirebasePlugin", "setDefaults", args);
};

exports.startTrace = function (name, success, error) {
  exec(success, error, "FirebasePlugin", "startTrace", [name]);
};

exports.incrementCounter = function (name, counterNamed, success, error) {
  exec(success, error, "FirebasePlugin", "incrementCounter", [name, counterNamed]);
};

exports.stopTrace = function (name, success, error) {
  exec(success, error, "FirebasePlugin", "stopTrace", [name]);
};

exports.setAnalyticsCollectionEnabled = function (enabled, success, error) {
  exec(success, error, "FirebasePlugin", "setAnalyticsCollectionEnabled", [enabled]);
};

exports.setPerformanceCollectionEnabled = function (enabled, success, error) {
  exec(success, error, "FirebasePlugin", "setPerformanceCollectionEnabled", [enabled]);
};

exports.verifyPhoneNumber = function (number, timeOutDuration, success, error) {
  if (typeof timeOutDuration === 'function') {
    // method being called with old signature: function(number, success, error)
    // timeOutDuration is the success callback, success is the error callback
    exec(timeOutDuration, success, "FirebasePlugin", "verifyPhoneNumber", [number]);
  } else {
    // method being called with new signature: function(number, timeOutDuration, success, error)
    // callbacks are correctly named
    exec(success, error, "FirebasePlugin", "verifyPhoneNumber", [number, timeOutDuration]);
  }
};

exports.clearAllNotifications = function (success, error) {
  exec(success, error, "FirebasePlugin", "clearAllNotifications", []);
};

exports.clear = function (id, success, error) {
    exec(success, error, "FirebasePlugin", "clear", [id]);
};

exports.clearNotifications = function (ids, success, error) {
  exec(success, error, "FirebasePlugin", "clear", ids);
};

exports.scheduleLocalNotification = function (params, success, error) {
  exec(success, error, "FirebasePlugin", "scheduleLocalNotification", [params]);
};

exports.getActiveIdsByTarget = function (target, success, error) {
  exec(success, error, "FirebasePlugin", "getActiveIdsByTarget", [target]);
};

exports.clearNotificationsByTarget = function (target, success, error) {
  exec(success, error, "FirebasePlugin", "clearNotificationsByTarget", [target]);
};

exports.clearTalkNotificationsExceptTargets = function (targets, success, error) {
  exec(success, error, "FirebasePlugin", "clearTalkNotificationsExceptTargets", [targets]);
};

exports.clearTalkNotificationsExceptTargetsAndMissedCalls = function (targets, success, error) {
  exec(success, error, "FirebasePlugin", "clearTalkNotificationsExceptTargetsAndMissedCalls", [targets]);
};

exports.scheduleLocalMailNotification = function (params, success, error) {
    exec(success, error, "FirebasePlugin", "scheduleLocalMailNotification", [params]);
};

exports.scheduleCallNotification = function (params, success, error) {
  exec(success, error, "FirebasePlugin", "scheduleCallNotification", [params]);
};

exports.clearMailNotification = function (mid, success, error) {
  exec(success, error, "FirebasePlugin", "clearMailNotification", [mid]);
};

exports.clearMailNotificationsExceptMids = function (mids, success, error) {
  exec(success, error, "FirebasePlugin", "clearMailNotificationsExceptMids", [mids]);
};

exports.clearAllMailNotificationsForConv = function (cid, success, error) {
  exec(success, error, "FirebasePlugin", "clearAllMailNotificationsForConv", [cid]);
};

exports.clearMailNotificationsExceptCids = function (cids, success, error) {
  exec(success, error, "FirebasePlugin", "clearMailNotificationsExceptCids", [cids]);
};

exports.enableLockScreenVisibility = function (enable, success, error) {
  exec(success, error, "FirebasePlugin", "enableLockScreenVisibility", [enable]);
};

exports.hideIncomingCallNotification = function (params, success, error) {
  exec(success, error, "FirebasePlugin", "hideIncomingCallNotification", [params]);
};

exports.displayMissedCallNotification = function (params, success, error) {
  exec(success, error, "FirebasePlugin", "displayMissedCallNotification", [params]);
};

exports.scheduleCalendarNotification = function (params, success, error) {
  exec(success, error, "FirebasePlugin", "scheduleCalendarNotification", [params]);
};

exports.clearNotificationByAppointmentId = function (appointmentId, success, error) {
  exec(success, error, "FirebasePlugin", "clearNotificationByAppointmentId", [appointmentId]);
};
