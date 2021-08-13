@interface FirebaseActionsManager

+ (void)registerTalkNotificationCategoriesAndActionsForCallRequest;
+ (void)registerTalkNotificationCategoriesAndActionsForChatMessage;
+ (BOOL)isVideoAudioCategory:(NSString *)categoryIdentifier;
+ (void)handleCallRequestActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier;
+ (void)handleChatReplyAction:(NSDictionary *)mutableUserInfo userText:(NSString *)userText;
+ (void)handleMarkMessageAsReadAction:(NSDictionary *)mutableUserInfo;
+ (void)handleMuteChatAction:(NSDictionary *)mutableUserInfo;
+ (BOOL)isCallRejectActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier;

@end
