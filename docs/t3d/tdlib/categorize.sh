#!/bin/bash
# Read descriptions line by line
while IFS='|' read -r class desc; do
    category="Other"
    case $class in
        UpdateAuthorizationState|UpdateConnectionState|UpdateApplication*|UpdateTermsOfService|UpdateAgeVerificationParameters|UpdateFreezeState|UpdateSpeechRecognitionTrial|UpdateSpeedLimitNotification|UpdateServiceNotification|UpdateSuggestedActions)
            category="Authorization & Connection"
            ;;
        UpdateChat*)
            category="Chat Updates"
            ;;
        UpdateMessage*|UpdateNewMessage|UpdateNewBusinessMessage|UpdateNewChatJoinRequest|UpdateNewCallbackQuery|UpdateNewInlineQuery|UpdateNewChosenInlineResult|UpdateNewPreCheckoutQuery|UpdateNewShippingQuery|UpdateNewCustomQuery|UpdateNewCustomEvent|UpdateNewCallSignalingData|UpdateNewGroupCallMessage|UpdateNewGroupCallPaidReaction|UpdateNewInlineCallbackQuery|UpdateNewBusinessCallbackQuery)
            category="Message Updates"
            ;;
        UpdateFile*)
            category="File Updates"
            ;;
        UpdateNotification*|UpdateActiveNotifications|UpdateHavePendingNotifications|UpdateScopeNotificationSettings|UpdateReactionNotificationSettings)
            category="Notification Updates"
            ;;
        UpdateSticker*|UpdateFavoriteStickers|UpdateInstalledStickerSets|UpdateRecentStickers|UpdateSavedAnimations|UpdateActiveEmojiReactions|UpdateDefaultReactionType|UpdateDefaultPaidReactionType|UpdateAnimatedEmojiMessageClicked|UpdateDiceEmojis)
            category="Sticker & Emoji Updates"
            ;;
        UpdateBasicGroup*|UpdateSupergroup*)
            category="Group & Supergroup Updates"
            ;;
        UpdateCall*|UpdateGroupCall*)
            category="Call Updates"
            ;;
        UpdateStory*|UpdateChatActiveStories|UpdateLiveStoryTopDonors)
            category="Story Updates"
            ;;
        UpdateBusiness*)
            category="Business Updates"
            ;;
        UpdateUser*|UpdateUserFullInfo|UpdateUserStatus|UpdateUserPrivacySettingRules)
            category="User Updates"
            ;;
        UpdateOption|UpdateAutosaveSettings|UpdateChatFolders|UpdateChatBackground|UpdateChatTheme|UpdateChatVideoChat|UpdateChatViewAsTopics|UpdateChatHasProtectedContent|UpdateChatHasScheduledMessages|UpdateChatIsMarkedAsUnread|UpdateChatIsTranslatable|UpdateChatPermissions|UpdateChatMessageAutoDeleteTime|UpdateChatNotificationSettings|UpdateChatActionBar|UpdateChatAction|UpdateChatAvailableReactions|UpdateChatBlockList|UpdateChatDefaultDisableNotification|UpdateChatDraftMessage|UpdateChatEmojiStatus|UpdateChatLastMessage|UpdateChatMember|UpdateChatMessageSender|UpdateChatOnlineMemberCount|UpdateChatPendingJoinRequests|UpdateChatPhoto|UpdateChatPosition|UpdateChatReadInbox|UpdateChatReadOutbox|UpdateChatRemovedFromList|UpdateChatReplyMarkup|UpdateChatRevenueAmount|UpdateChatTitle|UpdateChatUnreadMentionCount|UpdateChatUnreadReactionCount|UpdateChatAccentColors|UpdateChatBoost|UpdateChatBusinessBotManageBar)
            category="Settings & Options"
            ;;
        *)
            category="Other"
            ;;
    esac
    echo "$category|$class|$desc"
done < descriptions.txt
