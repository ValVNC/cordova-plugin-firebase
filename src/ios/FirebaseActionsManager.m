#import <UserNotifications/UserNotifications.h>
@import UserNotifications;

#import "FirebaseActionsManager.h"

@implementation FirebaseActionsManager

+ (void)registerTalkNotificationCategoriesAndActionsForCallRequest {
    // https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/SupportingNotificationsinYourApp.html#//apple_ref/doc/uid/TP40008194-CH4-SW26
    UNNotificationAction *acceptAction = [UNNotificationAction
          actionWithIdentifier:@"ACCEPT_CALL_ACTION"
          title:@"Accept"
          options:UNNotificationActionOptionForeground];

    UNNotificationAction *rejectAction = [UNNotificationAction
          actionWithIdentifier:@"REJECT_CALL_ACTION"
          title:@"Reject"
          options:UNNotificationActionOptionDestructive];

    UNNotificationCategory *videoCallCategory = [UNNotificationCategory
         categoryWithIdentifier:@"VIDEO"
         actions:@[acceptAction, rejectAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];

    UNNotificationCategory *audioCallCategory = [UNNotificationCategory
         categoryWithIdentifier:@"AUDIO"
         actions:@[acceptAction, rejectAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];

    // Register the notification categories.
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    [center setNotificationCategories:[NSSet setWithObjects:videoCallCategory, audioCallCategory, nil]];
}

+ (void)registerTalkNotificationCategoriesAndActionsForChatMessage {
    // https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/SupportingNotificationsinYourApp.html#//apple_ref/doc/uid/TP40008194-CH4-SW26
    UNTextInputNotificationAction *replyAction = [UNTextInputNotificationAction
        actionWithIdentifier:@"REPLY_MSG_ACTION"
        title:[self getLocalizedString: @"REPLY" defaultValue: @"Reply"]
        options:UNNotificationActionOptionAuthenticationRequired
        textInputButtonTitle:[self getLocalizedString:@"REPLY" defaultValue:@"Reply"]
        textInputPlaceholder:[self getLocalizedString:@"TYPE_MESSAGE" defaultValue:@"Type a message"]];
    
    UNNotificationAction *markAsReadAction = [UNNotificationAction
          actionWithIdentifier:@"MARK_AS_READ_ACTION"
          title:[self getLocalizedString:@"MARK_AS_READ" defaultValue:@"Mark as read"]
          options:UNNotificationActionOptionAuthenticationRequired];
    
    UNNotificationAction *muteChatAction = [UNNotificationAction
          actionWithIdentifier:@"MUTE_CHAT_ACTION"
          title:[self getLocalizedString:@"MUTE" defaultValue: @"Mute chat"]
          options:UNNotificationActionOptionAuthenticationRequired];
    
    UNNotificationCategory *broadcasrCategory = [UNNotificationCategory
         categoryWithIdentifier:@"BROADCAST"
         actions:@[markAsReadAction, muteChatAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];
    
    UNNotificationCategory *groupChatCategory = [UNNotificationCategory
         categoryWithIdentifier:@"GROUPCHAT"
         actions:@[replyAction, markAsReadAction, muteChatAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];
    
    UNNotificationCategory *chatCategory = [UNNotificationCategory
         categoryWithIdentifier:@"CHAT"
         actions:@[replyAction, markAsReadAction, muteChatAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];

    // Register the notification categories.
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    [center setNotificationCategories:[NSSet setWithObjects:groupChatCategory, chatCategory, broadcasrCategory, nil]];
}

+ (BOOL)isVideoAudioCategory:(NSString *)categoryIdentifier {
    return [categoryIdentifier isEqualToString:@"VIDEO"] || [categoryIdentifier isEqualToString:@"AUDIO"];
}

// handle Call actions
// https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/SchedulingandHandlingLocalNotifications.html#//apple_ref/doc/uid/TP40008194-CH5-SW2
//
+ (void)handleCallRequestActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier {

//    aft = "";
//    aps =     {
//        alert =         {
//            body = audio;
//            title = George;
//        };
//        badge = 13;
//        category = AUDIO;
//        sound = default;
//    };
//    body = audio;
//    callSignal = 1;
//    eType = invite;
//    "gcm.message_id" = 1576063852084600;
//    "google.c.a.e" = 1;
//    gt = George;
//    jid = "test__group__calls@conference.vnc.biz";  // jid = "bob@dev2.zimbra-vnc.de";
//    lang = en;
//    mention = "[]";
//    nType = "local_notification";
//    name = George;
//    nsound = nomute;
//    nto = "bob.bobson@vnc.biz";
//    tap = 1;

    NSString *eType = mutableUserInfo[@"eType"];
    if ([eType isEqualToString:@"invite"]) {
        if ([actionIdentifier isEqualToString:@"ACCEPT_CALL_ACTION"] || [actionIdentifier isEqualToString: UNNotificationDefaultActionIdentifier]) {
            [self markCallRequestAsProccessed:mutableUserInfo[@"msgid"]];

        } else if ([actionIdentifier isEqualToString:@"REJECT_CALL_ACTION"]) {
            [self markCallRequestAsProccessed:mutableUserInfo[@"msgid"]];

            BOOL isGroupCall = [mutableUserInfo[@"jid"] rangeOfString:@"@conference"].location != NSNotFound;
            NSString *callType = [mutableUserInfo[@"aps"][@"category"] lowercaseString];
            NSString *callId = mutableUserInfo[@"jid"];
            NSString *callReceiver = mutableUserInfo[@"nto"];
            NSString *confid = isGroupCall ?
                            callId :
            [[[callReceiver stringByReplacingOccurrencesOfString:@"@" withString:@"#"] stringByAppendingString:@","] stringByAppendingString:[callId stringByReplacingOccurrencesOfString:@"@" withString:@"#"]];
//            NSString *target = isGroupCall ? mutableUserInfo[@"jid"] : callId;
            [self rejectCall:callType confid:confid target:callId];
        }
    }
}

+ (void)handleChatReplyAction:(NSDictionary *)mutableUserInfo userText:(NSString *)userText {
    NSString *target = mutableUserInfo[@"jid"];
    
    NSDictionary *params = @{
        @"messagetext": userText,
        @"target": target
    };
    [self postRequestWithSubUrl:@"xmpp-rest" params:params];
}

+ (void)handleMarkMessageAsReadAction:(NSDictionary *)mutableUserInfo {
    NSString *target = mutableUserInfo[@"jid"];
    
    NSNumber *currentDate = [NSNumber numberWithDouble:[[NSDate date] timeIntervalSince1970]];
    
    NSDictionary *params = @{
        target: currentDate
    };
    
    [self postRequestWithSubUrl:@"markConversationsRead" params:params];
}

+ (void)handleMuteChatAction:(NSDictionary *)mutableUserInfo {
    NSString *target = mutableUserInfo[@"jid"];
    
    NSString *subUrl = [NSString stringWithFormat:@"notify/%@/2", target];
    
    NSDictionary *params = @{
        @"type": @2,
        @"target": target
    };
    
    [self putRequestWithSubUrl:subUrl params:params];
}

+ (BOOL)isCallRejectActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier {
    NSString *eType = mutableUserInfo[@"eType"];
    return [eType isEqualToString:@"invite"] && [actionIdentifier isEqualToString:@"REJECT_CALL_ACTION"];
}

+ (void)markCallRequestAsProccessed:(NSString *)mid {
    NSString *processedCallsIds = [[NSUserDefaults standardUserDefaults] stringForKey:@"processedCallsIds"];
    processedCallsIds = [NSString stringWithFormat:@"%@,%@", processedCallsIds, mid];

    [[NSUserDefaults standardUserDefaults] setObject:processedCallsIds forKey:@"processedCallsIds"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    NSLog(@"[FirebaseActionsManager][markCallRequestAsProccessed] mid: %@, processedCallsIds %@", mid, processedCallsIds);
}

///

+ (void)rejectCall:(NSString *)callType confid:(NSString *)confid target:(NSString *)target {
    NSDictionary *params = @{
        @"messagetext": @"REJECTED_CALL",
        @"reject": callType,
        @"confid": confid,
        @"target": target
    };
    [self postRequestWithSubUrl:@"xmpp-rest" params:params];

    // 1-1
// {"messagetext":"REJECTED_CALL","reject":"video","confid":"bob#dev2.zimbra-vnc.de,ihor.khomenko#vnc.biz","target":"ihor.khomenko@vnc.biz"}

    // group
//    {“messagetext”:“REJECTED_CALL”,“reject”:“video”,“confid”:“grvall@conference.dev2.zimbra-vnc.de”,“target”:“grvall@conference.dev2.zimbra-vnc.de”}
}

+ (void)postRequestWithSubUrl:(NSString *)suburl params:(NSDictionary *)params {
    NSLog(@"[FirebaseActionsManager][postRequestWithSubUrl] suburl: %@, params: %@", suburl, params);
    [self requestWithSubUrl:@"POST" suburl:suburl params:params];
}

+ (void)putRequestWithSubUrl:(NSString *)suburl params:(NSDictionary *)params {
    NSLog(@"[FirebaseActionsManager][putRequestWithSubUrl] suburl: %@, params: %@", suburl, params);
    [self requestWithSubUrl:@"PUT" suburl:suburl params:params];
}

+ (void)requestWithSubUrl:(NSString *)httpMethod  suburl:(NSString *)suburl params:(NSDictionary *)params {
    UIBackgroundTaskIdentifier bgTask = UIBackgroundTaskInvalid;
    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        NSLog(@"[FirebaseActionsManager][requestWithSubUrl] beginBackgroundTaskWithExpirationHandler expired");
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
    }];


    NSString *baseUrl = [[NSUserDefaults standardUserDefaults] stringForKey:@"apiUrl"];
    NSString *token = [[NSUserDefaults standardUserDefaults] stringForKey:@"auth-token"];

    NSLog(@"[FirebaseActionsManager][requestWithSubUrl] httpMethod: %@, baseUrl: %@, token: %@, params: %@", httpMethod, baseUrl, token, params);

    NSString *targetUrl = [NSString stringWithFormat:@"%@/%@", baseUrl, suburl];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];

    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:params options:0 error:&error];

    [request setHTTPBody:postData];
    [request setHTTPMethod:httpMethod];
    [request setURL:[NSURL URLWithString:targetUrl]];

    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request setValue:token forHTTPHeaderField: @"Authorization"];

    [[[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:
      ^(NSData * _Nullable data,
        NSURLResponse * _Nullable response,
        NSError * _Nullable error) {

            NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;

            NSString *responseStr = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            NSLog(@"[FirebaseActionsManager][requestWithSubUrl] response: %@, error %@, status code %ld", responseStr, error, (long)[httpResponse statusCode]);

            // AFTER ALL THE UPDATES, close the task
            if (bgTask != UIBackgroundTaskInvalid) {
               [[UIApplication sharedApplication] endBackgroundTask:bgTask];
            }
    }] resume];
}

- (NSString *)getLocalizedString:(NSString *)key defaultValue:(NSString *)defaultValue {
    NSDictionary *deStrings = @{
        @"REPLY": @"Antworten",
        @"MARK_AS_READ": @"Als gelesen markieren",
        @"MUTE": @"Chat stummschalten",
        @"TYPE_MESSAGE": @"Geben Sie eine Nachricht ein"
    };
    
    NSDictionary *enStrings = @{
        @"REPLY": @"Reply",
        @"MARK_AS_READ": @"Mark as read",
        @"MUTE": @"Mute chat",
        @"TYPE_MESSAGE": @"Type a message"
    };
    
    NSString *language = [[[NSLocale preferredLanguages] firstObject] substringWithRange:NSMakeRange(0, 2)];
    NSLog(@"[FirebaseActionsManager][getLocalizedString] language: %@", language);
    
    NSString *result = nil;
    
    if([language isEqualToString:@"de"]) {
        result = deStrings[key];
    } else {
        result = enStrings[key];
    }
    
    if(result == nil){
        result = defaultValue;
    }
    
    return  result;
}

@end
