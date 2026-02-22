# TDLib 更新类型分类

本文档列出了从 TDLib Java 文档中提取的所有 TdApi.Update 子类。

## 授权与连接

- **UpdateAgeVerificationParameters**: 当前用户账户的年龄验证参数已更改。
- **UpdateApplicationRecaptchaVerificationRequired**: 除非完成 reCAPTCHA 验证，否则请求无法完成；仅适用于官方移动应用程序。验证完成或失败后必须调用 setApplicationVerificationToken 方法。
- **UpdateApplicationVerificationRequired**: 除非完成应用程序验证，否则请求无法完成；仅适用于官方移动应用程序。验证完成或失败后必须调用 setApplicationVerificationToken 方法。
- **UpdateAuthorizationState**: 用户授权状态已更改。
- **UpdateConnectionState**: 连接状态已更改。此更新仅应用于显示人类可读的连接状态描述。
- **UpdateFreezeState**: 当前用户账户的冻结状态已更改。
- **UpdateServiceNotification**: 收到来自服务器的服务通知。收到后应用程序必须显示包含通知内容的弹窗。
- **UpdateSpeechRecognitionTrial**: 无 Telegram Premium 订阅的语音识别参数已更改。
- **UpdateSpeedLimitNotification**: 用户的下载或上传文件速度受到限制，但可通过订阅 Telegram Premium 恢复。通知可以延迟到用户可见的正在下载或上传的文件。使用 getOption("premium_download_speedup") 或 getOption("premium_upload_speedup") 获取订阅 Telegram Premium 后的预期加速。
- **UpdateSuggestedActions**: 向用户建议的操作列表已更改。
- **UpdateTermsOfService**: 用户必须接受新的服务条款。如果服务条款被拒绝，则必须调用 deleteAccount 方法，原因为 "Decline ToS update"。

## 商业更新

- **UpdateBusinessConnection**: 商业连接已更改；仅适用于机器人。
- **UpdateBusinessMessageEdited**: 商业账户中的消息被编辑；仅适用于机器人。
- **UpdateBusinessMessagesDeleted**: 商业账户中的消息被删除；仅适用于机器人。

## 通话更新

- **UpdateCall**: 新通话已创建或通话信息已更新。
- **UpdateGroupCall**: 群组通话信息已更新。
- **UpdateGroupCallMessageLevels**: 实时故事群组通话消息的级别已更改。
- **UpdateGroupCallMessageSendFailed**: 群组通话消息发送失败。
- **UpdateGroupCallMessagesDeleted**: 一些群组通话消息被删除。
- **UpdateGroupCallParticipant**: 群组通话参与者的信息已更改。仅在通过 getGroupCall 接收到群组通话后，并且仅当通话已加入或正在加入时，才会发送此更新。
- **UpdateGroupCallParticipants**: 可以发送和接收加密通话数据的群组通话参与者列表已更改；仅适用于未绑定到聊天的群组通话。
- **UpdateGroupCallVerificationState**: 加密群组通话的验证状态已更改；仅适用于未绑定到聊天的群组通话。

## 聊天更新

- **UpdateChatAccentColors**: 聊天强调色已更改。
- **UpdateChatAction**: 聊天中消息发送者活动已更改。
- **UpdateChatActionBar**: 聊天操作栏已更改。
- **UpdateChatActiveStories**: 特定聊天发布的活跃故事列表已更改。
- **UpdateChatAddedToList**: 聊天被添加到聊天列表。
- **UpdateChatAvailableReactions**: 聊天可用反应已更改。
- **UpdateChatBackground**: 聊天背景已更改。
- **UpdateChatBlockList**: 聊天被阻止或取消阻止。
- **UpdateChatBoost**: 聊天助力已更改；仅适用于机器人。
- **UpdateChatBusinessBotManageBar**: 聊天中管理商业机器人的栏已更改。
- **UpdateChatDefaultDisableNotification**: 发送消息到聊天时使用的默认 disableNotification 参数值已更改。
- **UpdateChatDraftMessage**: 聊天草稿已更改。请注意，更新可能会在当前打开的聊天中收到，但草稿内容可能是旧的。如果用户已更改草稿内容，则不应应用此更新。
- **UpdateChatEmojiStatus**: 聊天表情状态已更改。
- **UpdateChatFolders**: 聊天文件夹列表或聊天文件夹已更改。
- **UpdateChatHasProtectedContent**: 聊天内容被允许或限制保存。
- **UpdateChatHasScheduledMessages**: 聊天的 hasScheduledMessages 字段已更改。
- **UpdateChatIsMarkedAsUnread**: 聊天被标记为未读或已读。
- **UpdateChatIsTranslatable**: 聊天消息翻译已启用或禁用。
- **UpdateChatLastMessage**: 聊天的最后一条消息已更改。
- **UpdateChatMember**: 聊天中的用户权限已更改；仅适用于机器人。
- **UpdateChatMessageAutoDeleteTime**: 聊天的消息自动删除或自毁计时器设置已更改。
- **UpdateChatMessageSender**: 选择在聊天中发送消息的消息发送者已更改。
- **UpdateChatNotificationSettings**: 聊天的通知设置已更改。
- **UpdateChatOnlineMemberCount**: 在线群组成员数量已更改。此更新仅在当前打开的聊天中发送非零的在线群组成员数。不能保证在在线用户数量更改后立即发送。
- **UpdateChatPendingJoinRequests**: 聊天待处理的加入请求已更改。
- **UpdateChatPermissions**: 聊天权限已更改。
- **UpdateChatPhoto**: 聊天照片已更改。
- **UpdateChatPosition**: 聊天在聊天列表中的位置已更改。可能会发送 updateChatLastMessage 或 updateChatDraftMessage 更新来代替此更新。
- **UpdateChatReadInbox**: 收到消息已读或未读消息数量已更改。
- **UpdateChatReadOutbox**: 发出消息已读。
- **UpdateChatRemovedFromList**: 聊天从聊天列表中移除。
- **UpdateChatReplyMarkup**: 默认聊天回复标记已更改。可能是因为收到带有回复标记的新消息，或者用户隐藏了旧的回复标记。
- **UpdateChatRevenueAmount**: 聊天中来自赞助消息的收入已更改。如果聊天收入屏幕已打开，则可以调用 getChatRevenueTransactions 来获取新交易。
- **UpdateChatTheme**: 聊天主题已更改。
- **UpdateChatTitle**: 聊天标题已更改。
- **UpdateChatUnreadMentionCount**: 聊天未读提及计数已更改。
- **UpdateChatUnreadReactionCount**: 聊天未读反应计数已更改。
- **UpdateChatVideoChat**: 聊天视频通话状态已更改。
- **UpdateChatViewAsTopics**: 聊天默认外观已更改。

## 文件更新

- **UpdateFile**: 文件信息已更新。
- **UpdateFileAddedToDownloads**: 文件被添加到文件下载列表。仅当文件下载列表首次加载后才会发送此更新。
- **UpdateFileDownload**: 文件下载已更改。仅当文件下载列表首次加载后才会发送此更新。
- **UpdateFileDownloads**: 文件下载列表状态已更改。
- **UpdateFileGenerationStart**: 应用程序需要启动文件生成过程。使用 setFileGenerationProgress 和 finishFileGeneration 来生成文件。
- **UpdateFileGenerationStop**: 不再需要文件生成。
- **UpdateFileRemovedFromDownloads**: 文件从文件下载列表中移除。仅当文件下载列表首次加载后才会发送此更新。

## 群组与超级群组更新

- **UpdateBasicGroup**: 基本群组的某些数据已更改。此更新保证在基本群组标识符返回给应用程序之前发送。
- **UpdateBasicGroupFullInfo**: basicGroupFullInfo 中的某些数据已更改。
- **UpdateSupergroup**: 超级群组或频道的某些数据已更改。此更新保证在超级群组标识符返回给应用程序之前发送。
- **UpdateSupergroupFullInfo**: supergroupFullInfo 中的某些数据已更改。

## 消息更新

- **UpdateMessageContent**: 消息内容已更改。
- **UpdateMessageContentOpened**: 消息内容被打开。将语音笔记消息更新为“已收听”，视频笔记消息更新为“已查看”，并启动自毁计时器。
- **UpdateMessageEdited**: 消息被编辑。消息内容的变化将在单独的 updateMessageContent 中提供。
- **UpdateMessageFactCheck**: 添加到消息的事实检查已更改。
- **UpdateMessageInteractionInfo**: 与消息交互的信息已更改。
- **UpdateMessageIsPinned**: 消息固定状态已更改。
- **UpdateMessageLiveLocationViewed**: 带有实时位置的消息被查看。收到更新后，应用程序应更新实时位置。
- **UpdateMessageMentionRead**: 带有未读提及的消息已读。
- **UpdateMessageReaction**: 用户更改了其在带有公开反应的消息上的反应；仅适用于机器人。
- **UpdateMessageReactions**: 添加到带有匿名反应的消息上的反应已更改；仅适用于机器人。
- **UpdateMessageSendAcknowledged**: 发送消息的请求已到达 Telegram 服务器。这并不意味着消息将成功发送。仅当选项 "use_quick_ack" 设置为 true 时才会发送此更新。同一消息可能会多次发送此更新。
- **UpdateMessageSendFailed**: 消息发送失败。请注意，某些正在发送的消息可能会被不可恢复地删除，在这种情况下将收到 updateDeleteMessages 而不是此更新。
- **UpdateMessageSendSucceeded**: 消息已成功发送。
- **UpdateMessageSuggestedPostInfo**: 消息的建议帖子信息已更改。
- **UpdateMessageUnreadReactions**: 添加到消息的未读反应列表已更改。
- **UpdateNewBusinessCallbackQuery**: 来自商业消息的新回调查询；仅适用于机器人。
- **UpdateNewBusinessMessage**: 新消息被添加到商业账户；仅适用于机器人。
- **UpdateNewCallSignalingData**: 新的通话信令数据到达。
- **UpdateNewCallbackQuery**: 新的回调查询；仅适用于机器人。
- **UpdateNewChatJoinRequest**: 用户向聊天发送加入请求；仅适用于机器人。
- **UpdateNewChosenInlineResult**: 用户选择了内联查询的结果；仅适用于机器人。
- **UpdateNewCustomEvent**: 新的事件；仅适用于机器人。
- **UpdateNewCustomQuery**: 新的查询；仅适用于机器人。
- **UpdateNewGroupCallMessage**: 在群组通话中收到新消息。
- **UpdateNewGroupCallPaidReaction**: 在实时故事群组通话中收到新的付费反应。
- **UpdateNewInlineCallbackQuery**: 来自通过机器人发送的消息的新回调查询；仅适用于机器人。
- **UpdateNewInlineQuery**: 新的内联查询；仅适用于机器人。
- **UpdateNewMessage**: 收到新消息；也可以是发出消息。
- **UpdateNewPreCheckoutQuery**: 新的预结账查询；仅适用于机器人。包含关于结账的完整信息。
- **UpdateNewShippingQuery**: 新的运费查询；仅适用于机器人。仅适用于具有灵活价格的发票。

## 通知更新

- **UpdateActiveNotifications**: 包含在先前应用程序启动时显示的活动通知。仅在使用消息数据库时发送此更新。在这种情况下，它会在任何 updateNotification 和 updateNotificationGroup 更新之前发送一次。
- **UpdateHavePendingNotifications**: 描述是否有待处理的通知更新。可用于防止应用程序在有待处理通知时被终止。
- **UpdateNotification**: 通知已更改。
- **UpdateNotificationGroup**: 通知组中的活动通知列表已更改。
- **UpdateReactionNotificationSettings**: 反应的通知设置已更新。
- **UpdateScopeNotificationSettings**: 某些类型聊天的通知设置已更新。

## 其他

- **UpdateAccentColors**: 支持的强调色列表已更改。
- **UpdateActiveGiftAuctions**: 当前用户参与的拍卖列表已更改。
- **UpdateActiveLiveLocationMessages**: 需要应用程序更新的带有活动实时位置的消息列表已更改。仅当使用消息数据库时，列表在应用程序重启后才会持久化。
- **UpdateAnimationSearchParameters**: 通过 getOption("animation_search_bot_username") 机器人进行动画搜索的参数已更改。
- **UpdateAttachmentMenuBots**: 添加到附件或侧边菜单的机器人列表已更改。
- **UpdateAvailableMessageEffects**: 可用的消息效果列表已更改。
- **UpdateContactCloseBirthdays**: 最近或即将过生日的联系人列表已更改。
- **UpdateDefaultBackground**: 默认背景已更改。
- **UpdateDeleteMessages**: 一些消息被删除。
- **UpdateDirectMessagesChatTopic**: 由当前用户管理的频道直接消息聊天中的主题基本信息已更改。此更新保证在主题标识符返回给应用程序之前发送。
- **UpdateEmojiChatThemes**: 可用的表情聊天主题列表已更改。
- **UpdateForumTopic**: 论坛聊天中的主题信息已更改。
- **UpdateForumTopicInfo**: 论坛聊天中的主题基本信息已更改。
- **UpdateGiftAuctionState**: 礼物拍卖状态已更新。
- **UpdateLanguagePackStrings**: 一些语言包字符串已更新。
- **UpdateNewChat**: 新的聊天已加载/创建。此更新保证在聊天标识符返回给应用程序之前发送。聊天字段的变化将通过单独的更新报告。
- **UpdateOwnedStarCount**: 当前用户拥有的 Telegram Stars 数量已更改。
- **UpdateOwnedTonCount**: 当前用户拥有的 Toncoins 数量已更改。
- **UpdatePaidMediaPurchased**: 用户购买了付费媒体；仅适用于机器人。
- **UpdatePendingTextMessage**: 在机器人的聊天中收到新的待处理文本消息。该消息必须在最多 getOption("pending_text_message_period") 秒内在聊天中显示，替换具有相同 draftId 的任何其他待处理消息，并在收到来自机器人的消息线程中的任何传入消息时删除。
- **UpdatePoll**: 投票已更新；仅适用于机器人。
- **UpdatePollAnswer**: 用户更改了投票答案；仅适用于机器人。
- **UpdateProfileAccentColors**: 用户资料支持的强调色列表已更改。
- **UpdateQuickReplyShortcut**: 快速回复快捷方式的基本信息已更改。此更新保证在快速快捷方式名称返回给应用程序之前发送。
- **UpdateQuickReplyShortcutDeleted**: 快速回复快捷方式及其所有消息被删除。
- **UpdateQuickReplyShortcutMessages**: 快速回复快捷方式消息列表已更改。
- **UpdateQuickReplyShortcuts**: 快速回复快捷方式列表已更改。
- **UpdateSavedMessagesTags**: 在 Saved Messages 或 Saved Messages 主题中使用的标签已更改。
- **UpdateSavedMessagesTopic**: Saved Messages 主题的基本信息已更改。此更新保证在主题标识符返回给应用程序之前发送。
- **UpdateSavedMessagesTopicCount**: Saved Messages 主题数量已更改。
- **UpdateSavedNotificationSounds**: 保存的通知声音列表已更新。此更新可能直到首次请求通知声音信息后才发送。
- **UpdateSecretChat**: 秘密聊天的某些数据已更改。此更新保证在秘密聊天标识符返回给应用程序之前发送。
- **UpdateStakeDiceState**: 投注骰子状态已更改。
- **UpdateStarRevenueStatus**: 用户或聊天赚取的 Telegram Star 收入已更改。如果聊天的 Telegram Star 交易屏幕已打开，则可以调用 getStarTransactions 来获取新交易。
- **UpdateTonRevenueStatus**: 当前用户赚取的 Toncoin 收入已更改。如果聊天的 Toncoin 交易屏幕已打开，则可以调用 getTonTransactions 来获取新交易。
- **UpdateTopicMessageCount**: 主题中的消息数量已更改；仅适用于 Saved Messages 和频道直接消息聊天主题。
- **UpdateTrendingStickerSets**: 趋势贴纸集列表已更新或其中一些被查看。
- **UpdateTrustedMiniAppBots**: 必须允许读取剪贴板文本且必须在没有警告的情况下打开的 Mini Apps 机器人列表。
- **UpdateUnconfirmedSession**: 第一个未确认的会话已更改。
- **UpdateUnreadChatCount**: 未读聊天（即带有未读消息或标记为未读）的数量已更改。仅当使用消息数据库时才会发送此更新。
- **UpdateUnreadMessageCount**: 聊天列表中未读消息的数量已更改。仅当使用消息数据库时才会发送此更新。
- **UpdateVideoPublished**: 自动计划的视频消息在转换后已成功发送。
- **UpdateWebAppMessageSent**: 消息由打开的 Web App 发送，因此需要关闭 Web App。
- **Updates**: 包含更新列表。

## 设置与选项

- **UpdateAutosaveSettings**: 某些类型聊天的自动保存设置已更新。
- **UpdateOption**: 选项值已更改。

## 贴纸与表情更新

- **UpdateActiveEmojiReactions**: 活动表情反应列表已更改。
- **UpdateAnimatedEmojiMessageClicked**: 某些动画表情消息被点击，如果消息在屏幕上可见，则必须播放大型动画贴纸。如果播放贴纸，则需要发送带有消息文本的 chatActionWatchingAnimations。
- **UpdateDefaultPaidReactionType**: 默认付费反应类型已更改。
- **UpdateDefaultReactionType**: 默认反应类型已更改。
- **UpdateDiceEmojis**: 支持的骰子表情列表已更改。
- **UpdateFavoriteStickers**: 收藏贴纸列表已更新。
- **UpdateInstalledStickerSets**: 已安装贴纸集列表已更新。
- **UpdateRecentStickers**: 最近使用的贴纸列表已更新。
- **UpdateSavedAnimations**: 保存的动画列表已更新。
- **UpdateStickerSet**: 贴纸集已更改。

## 故事更新

- **UpdateLiveStoryTopDonors**: 实时故事群组通话中的顶级捐赠者列表已更改。
- **UpdateStory**: 故事已更改。
- **UpdateStoryDeleted**: 故事变得不可访问。
- **UpdateStoryListChatCount**: 故事列表中的聊天数量已更改。
- **UpdateStoryPostFailed**: 故事发布失败。如果故事发布被取消，则将收到 updateStoryDeleted 而不是此更新。
- **UpdateStoryPostSucceeded**: 故事已成功发布。
- **UpdateStoryStealthMode**: 故事隐身模式设置已更改。

## 用户更新

- **UpdateUser**: 用户的某些数据已更改。此更新保证在用户标识符返回给应用程序之前发送。
- **UpdateUserFullInfo**: userFullInfo 中的某些数据已更改。
- **UpdateUserPrivacySettingRules**: 某些隐私设置规则已更改。
- **UpdateUserStatus**: 用户上线或离线。