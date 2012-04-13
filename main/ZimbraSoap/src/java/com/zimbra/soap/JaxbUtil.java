/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010-2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Document;
import org.dom4j.Namespace;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.util.JaxbInfo;

public final class JaxbUtil {

    private static final Log LOG = ZimbraLog.soap;
    private static final Class<?>[] MESSAGE_CLASSES;
    private static final String ACCOUNT_JAXB_PACKAGE = "com.zimbra.soap.account.message";
    private static final String ADMIN_JAXB_PACKAGE = "com.zimbra.soap.admin.message";
    private static final String MAIL_JAXB_PACKAGE = "com.zimbra.soap.mail.message";
    private static JAXBContext JAXB_CONTEXT;
    private static Map <Class<?>,JAXBContext> classJaxbContexts;

    static {
        MESSAGE_CLASSES = new Class<?>[] {
            // zimbraAccount
            com.zimbra.soap.account.message.AuthRequest.class,
            com.zimbra.soap.account.message.AuthResponse.class,
            com.zimbra.soap.account.message.AutoCompleteGalRequest.class,
            com.zimbra.soap.account.message.AutoCompleteGalResponse.class,
            com.zimbra.soap.account.message.ChangePasswordRequest.class,
            com.zimbra.soap.account.message.ChangePasswordResponse.class,
            com.zimbra.soap.account.message.CheckLicenseRequest.class,
            com.zimbra.soap.account.message.CheckLicenseResponse.class,
            com.zimbra.soap.account.message.CheckRightsRequest.class,
            com.zimbra.soap.account.message.CheckRightsResponse.class,
            com.zimbra.soap.account.message.CreateDistributionListRequest.class,
            com.zimbra.soap.account.message.CreateDistributionListResponse.class,
            com.zimbra.soap.account.message.CreateIdentityRequest.class,
            com.zimbra.soap.account.message.CreateIdentityResponse.class,
            com.zimbra.soap.account.message.CreateSignatureRequest.class,
            com.zimbra.soap.account.message.CreateSignatureResponse.class,
            com.zimbra.soap.account.message.DeleteIdentityRequest.class,
            com.zimbra.soap.account.message.DeleteIdentityResponse.class,
            com.zimbra.soap.account.message.DeleteSignatureRequest.class,
            com.zimbra.soap.account.message.DeleteSignatureResponse.class,
            com.zimbra.soap.account.message.DiscoverRightsRequest.class,
            com.zimbra.soap.account.message.DiscoverRightsResponse.class,
            com.zimbra.soap.account.message.DistributionListActionRequest.class,
            com.zimbra.soap.account.message.DistributionListActionResponse.class,
            com.zimbra.soap.account.message.EndSessionRequest.class,
            com.zimbra.soap.account.message.EndSessionResponse.class,
            com.zimbra.soap.account.message.GetAccountInfoRequest.class,
            com.zimbra.soap.account.message.GetAccountInfoResponse.class,
            com.zimbra.soap.account.message.GetAllLocalesRequest.class,
            com.zimbra.soap.account.message.GetAccountDistributionListsRequest.class,
            com.zimbra.soap.account.message.GetAccountDistributionListsResponse.class,
            com.zimbra.soap.account.message.GetAllLocalesResponse.class,
            com.zimbra.soap.account.message.GetAvailableCsvFormatsRequest.class,
            com.zimbra.soap.account.message.GetAvailableCsvFormatsResponse.class,
            com.zimbra.soap.account.message.GetAvailableLocalesRequest.class,
            com.zimbra.soap.account.message.GetAvailableLocalesResponse.class,
            com.zimbra.soap.account.message.GetAvailableSkinsRequest.class,
            com.zimbra.soap.account.message.GetAvailableSkinsResponse.class,
            com.zimbra.soap.account.message.GetDistributionListRequest.class,
            com.zimbra.soap.account.message.GetDistributionListResponse.class,
            com.zimbra.soap.account.message.GetDistributionListMembersRequest.class,
            com.zimbra.soap.account.message.GetDistributionListMembersResponse.class,
            com.zimbra.soap.account.message.GetIdentitiesRequest.class,
            com.zimbra.soap.account.message.GetIdentitiesResponse.class,
            com.zimbra.soap.account.message.GetInfoRequest.class,
            com.zimbra.soap.account.message.GetInfoResponse.class,
            com.zimbra.soap.account.message.GetPrefsRequest.class,
            com.zimbra.soap.account.message.GetPrefsResponse.class,
            com.zimbra.soap.account.message.GetRightsRequest.class,
            com.zimbra.soap.account.message.GetRightsResponse.class,
            com.zimbra.soap.account.message.GetSMIMEPublicCertsRequest.class,
            com.zimbra.soap.account.message.GetSMIMEPublicCertsResponse.class,
            com.zimbra.soap.account.message.GetShareInfoRequest.class,
            com.zimbra.soap.account.message.GetShareInfoResponse.class,
            com.zimbra.soap.account.message.GetSignaturesRequest.class,
            com.zimbra.soap.account.message.GetSignaturesResponse.class,
            com.zimbra.soap.account.message.GetVersionInfoRequest.class,
            com.zimbra.soap.account.message.GetVersionInfoResponse.class,
            com.zimbra.soap.account.message.GetWhiteBlackListRequest.class,
            com.zimbra.soap.account.message.GetWhiteBlackListResponse.class,
            com.zimbra.soap.account.message.GrantRightsRequest.class,
            com.zimbra.soap.account.message.GrantRightsResponse.class,
            com.zimbra.soap.account.message.ModifyIdentityRequest.class,
            com.zimbra.soap.account.message.ModifyIdentityResponse.class,
            com.zimbra.soap.account.message.ModifyPrefsRequest.class,
            com.zimbra.soap.account.message.ModifyPrefsResponse.class,
            com.zimbra.soap.account.message.ModifyPropertiesRequest.class,
            com.zimbra.soap.account.message.ModifyPropertiesResponse.class,
            com.zimbra.soap.account.message.ModifySignatureRequest.class,
            com.zimbra.soap.account.message.ModifySignatureResponse.class,
            com.zimbra.soap.account.message.ModifyWhiteBlackListRequest.class,
            com.zimbra.soap.account.message.ModifyWhiteBlackListResponse.class,
            com.zimbra.soap.account.message.ModifyZimletPrefsRequest.class,
            com.zimbra.soap.account.message.ModifyZimletPrefsResponse.class,
            com.zimbra.soap.account.message.RevokeRightsRequest.class,
            com.zimbra.soap.account.message.RevokeRightsResponse.class,
            com.zimbra.soap.account.message.SearchCalendarResourcesRequest.class,
            com.zimbra.soap.account.message.SearchCalendarResourcesResponse.class,
            com.zimbra.soap.account.message.SearchGalRequest.class,
            com.zimbra.soap.account.message.SearchGalResponse.class,
            com.zimbra.soap.account.message.SyncGalRequest.class,
            com.zimbra.soap.account.message.SyncGalResponse.class,
            com.zimbra.soap.account.message.SubscribeDistributionListRequest.class,
            com.zimbra.soap.account.message.SubscribeDistributionListResponse.class,
            com.zimbra.soap.account.message.UpdateProfileRequest.class,
            com.zimbra.soap.account.message.UpdateProfileResponse.class,

            // zimbraMail
            com.zimbra.soap.mail.message.AddAppointmentInviteRequest.class,
            com.zimbra.soap.mail.message.AddAppointmentInviteResponse.class,
            com.zimbra.soap.mail.message.AddCommentRequest.class,
            com.zimbra.soap.mail.message.AddCommentResponse.class,
            com.zimbra.soap.mail.message.AddMsgRequest.class,
            com.zimbra.soap.mail.message.AddMsgResponse.class,
            com.zimbra.soap.mail.message.AddTaskInviteRequest.class,
            com.zimbra.soap.mail.message.AddTaskInviteResponse.class,
            com.zimbra.soap.mail.message.AnnounceOrganizerChangeRequest.class,
            com.zimbra.soap.mail.message.AnnounceOrganizerChangeResponse.class,
            com.zimbra.soap.mail.message.ApplyFilterRulesRequest.class,
            com.zimbra.soap.mail.message.ApplyFilterRulesResponse.class,
            com.zimbra.soap.mail.message.ApplyOutgoingFilterRulesRequest.class,
            com.zimbra.soap.mail.message.ApplyOutgoingFilterRulesResponse.class,
            com.zimbra.soap.mail.message.AutoCompleteRequest.class,
            com.zimbra.soap.mail.message.AutoCompleteResponse.class,
            com.zimbra.soap.mail.message.BounceMsgRequest.class,
            com.zimbra.soap.mail.message.BounceMsgResponse.class,
            com.zimbra.soap.mail.message.BrowseRequest.class,
            com.zimbra.soap.mail.message.BrowseResponse.class,
            com.zimbra.soap.mail.message.CancelAppointmentRequest.class,
            com.zimbra.soap.mail.message.CancelAppointmentResponse.class,
            com.zimbra.soap.mail.message.CancelTaskRequest.class,
            com.zimbra.soap.mail.message.CancelTaskResponse.class,
            com.zimbra.soap.mail.message.CheckDeviceStatusRequest.class,
            com.zimbra.soap.mail.message.CheckDeviceStatusResponse.class,
            com.zimbra.soap.mail.message.CheckPermissionRequest.class,
            com.zimbra.soap.mail.message.CheckPermissionResponse.class,
            com.zimbra.soap.mail.message.CheckRecurConflictsRequest.class,
            com.zimbra.soap.mail.message.CheckRecurConflictsResponse.class,
            com.zimbra.soap.mail.message.CheckSpellingRequest.class,
            com.zimbra.soap.mail.message.CheckSpellingResponse.class,
            com.zimbra.soap.mail.message.CompleteTaskInstanceRequest.class,
            com.zimbra.soap.mail.message.CompleteTaskInstanceResponse.class,
            com.zimbra.soap.mail.message.ContactActionRequest.class,
            com.zimbra.soap.mail.message.ContactActionResponse.class,
            com.zimbra.soap.mail.message.ConvActionRequest.class,
            com.zimbra.soap.mail.message.ConvActionResponse.class,
            com.zimbra.soap.mail.message.CounterAppointmentRequest.class,
            com.zimbra.soap.mail.message.CounterAppointmentResponse.class,
            com.zimbra.soap.mail.message.CreateAppointmentExceptionRequest.class,
            com.zimbra.soap.mail.message.CreateAppointmentExceptionResponse.class,
            com.zimbra.soap.mail.message.CreateAppointmentRequest.class,
            com.zimbra.soap.mail.message.CreateAppointmentResponse.class,
            com.zimbra.soap.mail.message.CreateContactRequest.class,
            com.zimbra.soap.mail.message.CreateContactResponse.class,
            com.zimbra.soap.mail.message.CreateDataSourceRequest.class,
            com.zimbra.soap.mail.message.CreateDataSourceResponse.class,
            com.zimbra.soap.mail.message.CreateFolderRequest.class,
            com.zimbra.soap.mail.message.CreateFolderResponse.class,
            com.zimbra.soap.mail.message.CreateMountpointRequest.class,
            com.zimbra.soap.mail.message.CreateMountpointResponse.class,
            com.zimbra.soap.mail.message.CreateNoteRequest.class,
            com.zimbra.soap.mail.message.CreateNoteResponse.class,
            com.zimbra.soap.mail.message.CreateSearchFolderRequest.class,
            com.zimbra.soap.mail.message.CreateSearchFolderResponse.class,
            com.zimbra.soap.mail.message.CreateTagRequest.class,
            com.zimbra.soap.mail.message.CreateTagResponse.class,
            com.zimbra.soap.mail.message.CreateTaskExceptionRequest.class,
            com.zimbra.soap.mail.message.CreateTaskExceptionResponse.class,
            com.zimbra.soap.mail.message.CreateTaskRequest.class,
            com.zimbra.soap.mail.message.CreateTaskResponse.class,
            com.zimbra.soap.mail.message.CreateWaitSetRequest.class,
            com.zimbra.soap.mail.message.CreateWaitSetResponse.class,
            com.zimbra.soap.mail.message.DeclineCounterAppointmentRequest.class,
            com.zimbra.soap.mail.message.DeclineCounterAppointmentResponse.class,
            com.zimbra.soap.mail.message.DeleteDataSourceRequest.class,
            com.zimbra.soap.mail.message.DeleteDataSourceResponse.class,
            com.zimbra.soap.mail.message.DeleteDeviceRequest.class,
            com.zimbra.soap.mail.message.DeleteDeviceResponse.class,
            com.zimbra.soap.mail.message.DestroyWaitSetRequest.class,
            com.zimbra.soap.mail.message.DestroyWaitSetResponse.class,
            com.zimbra.soap.mail.message.DiffDocumentRequest.class,
            com.zimbra.soap.mail.message.DiffDocumentResponse.class,
            com.zimbra.soap.mail.message.DismissCalendarItemAlarmRequest.class,
            com.zimbra.soap.mail.message.DismissCalendarItemAlarmResponse.class,
            com.zimbra.soap.mail.message.DocumentActionRequest.class,
            com.zimbra.soap.mail.message.DocumentActionResponse.class,
            com.zimbra.soap.mail.message.EmptyDumpsterRequest.class,
            com.zimbra.soap.mail.message.EmptyDumpsterResponse.class,
            com.zimbra.soap.mail.message.EnableSharedReminderRequest.class,
            com.zimbra.soap.mail.message.EnableSharedReminderResponse.class,
            com.zimbra.soap.mail.message.ExpandRecurRequest.class,
            com.zimbra.soap.mail.message.ExpandRecurResponse.class,
            com.zimbra.soap.mail.message.ExportContactsRequest.class,
            com.zimbra.soap.mail.message.ExportContactsResponse.class,
            com.zimbra.soap.mail.message.FolderActionRequest.class,
            com.zimbra.soap.mail.message.FolderActionResponse.class,
            com.zimbra.soap.mail.message.ForwardAppointmentInviteRequest.class,
            com.zimbra.soap.mail.message.ForwardAppointmentInviteResponse.class,
            com.zimbra.soap.mail.message.ForwardAppointmentRequest.class,
            com.zimbra.soap.mail.message.ForwardAppointmentResponse.class,
            com.zimbra.soap.mail.message.GenerateUUIDRequest.class,
            com.zimbra.soap.mail.message.GenerateUUIDResponse.class,
            com.zimbra.soap.mail.message.GetActivityStreamRequest.class,
            com.zimbra.soap.mail.message.GetActivityStreamResponse.class,
            com.zimbra.soap.mail.message.GetAllDevicesRequest.class,
            com.zimbra.soap.mail.message.GetAllDevicesResponse.class,
            com.zimbra.soap.mail.message.GetAppointmentRequest.class,
            com.zimbra.soap.mail.message.GetAppointmentResponse.class,
            com.zimbra.soap.mail.message.GetApptSummariesRequest.class,
            com.zimbra.soap.mail.message.GetApptSummariesResponse.class,
            com.zimbra.soap.mail.message.GetCalendarItemSummariesRequest.class,
            com.zimbra.soap.mail.message.GetCalendarItemSummariesResponse.class,
            com.zimbra.soap.mail.message.GetCommentsRequest.class,
            com.zimbra.soap.mail.message.GetCommentsResponse.class,
            com.zimbra.soap.mail.message.GetContactsRequest.class,
            com.zimbra.soap.mail.message.GetContactsResponse.class,
            com.zimbra.soap.mail.message.GetConvRequest.class,
            com.zimbra.soap.mail.message.GetConvResponse.class,
            com.zimbra.soap.mail.message.GetCustomMetadataRequest.class,
            com.zimbra.soap.mail.message.GetCustomMetadataResponse.class,
            com.zimbra.soap.mail.message.GetDataSourcesRequest.class,
            com.zimbra.soap.mail.message.GetDataSourcesResponse.class,
            com.zimbra.soap.mail.message.GetEffectiveFolderPermsRequest.class,
            com.zimbra.soap.mail.message.GetEffectiveFolderPermsResponse.class,
            com.zimbra.soap.mail.message.GetFilterRulesRequest.class,
            com.zimbra.soap.mail.message.GetFilterRulesResponse.class,
            com.zimbra.soap.mail.message.GetFolderRequest.class,
            com.zimbra.soap.mail.message.GetFolderResponse.class,
            com.zimbra.soap.mail.message.GetFreeBusyRequest.class,
            com.zimbra.soap.mail.message.GetFreeBusyResponse.class,
            com.zimbra.soap.mail.message.GetICalRequest.class,
            com.zimbra.soap.mail.message.GetICalResponse.class,
            com.zimbra.soap.mail.message.GetImportStatusRequest.class,
            com.zimbra.soap.mail.message.GetImportStatusResponse.class,
            com.zimbra.soap.mail.message.GetItemRequest.class,
            com.zimbra.soap.mail.message.GetItemResponse.class,
            com.zimbra.soap.mail.message.GetMailboxMetadataRequest.class,
            com.zimbra.soap.mail.message.GetMailboxMetadataResponse.class,
            com.zimbra.soap.mail.message.GetMiniCalRequest.class,
            com.zimbra.soap.mail.message.GetMiniCalResponse.class,
            com.zimbra.soap.mail.message.GetMsgMetadataRequest.class,
            com.zimbra.soap.mail.message.GetMsgMetadataResponse.class,
            com.zimbra.soap.mail.message.GetMsgRequest.class,
            com.zimbra.soap.mail.message.GetMsgResponse.class,
            com.zimbra.soap.mail.message.GetNoteRequest.class,
            com.zimbra.soap.mail.message.GetNoteResponse.class,
            com.zimbra.soap.mail.message.GetNotificationsRequest.class,
            com.zimbra.soap.mail.message.GetNotificationsResponse.class,
            com.zimbra.soap.mail.message.GetOutgoingFilterRulesRequest.class,
            com.zimbra.soap.mail.message.GetOutgoingFilterRulesResponse.class,
            com.zimbra.soap.mail.message.GetPermissionRequest.class,
            com.zimbra.soap.mail.message.GetPermissionResponse.class,
            com.zimbra.soap.mail.message.GetRecurRequest.class,
            com.zimbra.soap.mail.message.GetRecurResponse.class,
            com.zimbra.soap.mail.message.GetSearchFolderRequest.class,
            com.zimbra.soap.mail.message.GetSearchFolderResponse.class,
            com.zimbra.soap.mail.message.GetShareNotificationsRequest.class,
            com.zimbra.soap.mail.message.GetShareNotificationsResponse.class,
            com.zimbra.soap.mail.message.GetSpellDictionariesRequest.class,
            com.zimbra.soap.mail.message.GetSpellDictionariesResponse.class,
            com.zimbra.soap.mail.message.GetSystemRetentionPolicyRequest.class,
            com.zimbra.soap.mail.message.GetSystemRetentionPolicyResponse.class,
            com.zimbra.soap.mail.message.GetTagRequest.class,
            com.zimbra.soap.mail.message.GetTagResponse.class,
            com.zimbra.soap.mail.message.GetTaskRequest.class,
            com.zimbra.soap.mail.message.GetTaskResponse.class,
            com.zimbra.soap.mail.message.GetTaskSummariesRequest.class,
            com.zimbra.soap.mail.message.GetTaskSummariesResponse.class,
            com.zimbra.soap.mail.message.GetWatchersRequest.class,
            com.zimbra.soap.mail.message.GetWatchersResponse.class,
            com.zimbra.soap.mail.message.GetWatchingItemsRequest.class,
            com.zimbra.soap.mail.message.GetWatchingItemsResponse.class,
            com.zimbra.soap.mail.message.GetWorkingHoursRequest.class,
            com.zimbra.soap.mail.message.GetWorkingHoursResponse.class,
            com.zimbra.soap.mail.message.GetYahooAuthTokenRequest.class,
            com.zimbra.soap.mail.message.GetYahooAuthTokenResponse.class,
            com.zimbra.soap.mail.message.GetYahooCookieRequest.class,
            com.zimbra.soap.mail.message.GetYahooCookieResponse.class,
            com.zimbra.soap.mail.message.GlobalSearchRequest.class,
            com.zimbra.soap.mail.message.GlobalSearchResponse.class,
            com.zimbra.soap.mail.message.GrantPermissionRequest.class,
            com.zimbra.soap.mail.message.GrantPermissionResponse.class,
            com.zimbra.soap.mail.message.ICalReplyRequest.class,
            com.zimbra.soap.mail.message.ICalReplyResponse.class,
            com.zimbra.soap.mail.message.ImportAppointmentsRequest.class,
            com.zimbra.soap.mail.message.ImportAppointmentsResponse.class,
            com.zimbra.soap.mail.message.ImportContactsRequest.class,
            com.zimbra.soap.mail.message.ImportContactsResponse.class,
            com.zimbra.soap.mail.message.ImportDataRequest.class,
            com.zimbra.soap.mail.message.ImportDataResponse.class,
            com.zimbra.soap.mail.message.InvalidateReminderDeviceRequest.class,
            com.zimbra.soap.mail.message.InvalidateReminderDeviceResponse.class,
            com.zimbra.soap.mail.message.ItemActionRequest.class,
            com.zimbra.soap.mail.message.ItemActionResponse.class,
            com.zimbra.soap.mail.message.ListDocumentRevisionsRequest.class,
            com.zimbra.soap.mail.message.ListDocumentRevisionsResponse.class,
            com.zimbra.soap.mail.message.ModifyAppointmentRequest.class,
            com.zimbra.soap.mail.message.ModifyAppointmentResponse.class,
            com.zimbra.soap.mail.message.ModifyContactRequest.class,
            com.zimbra.soap.mail.message.ModifyContactResponse.class,
            com.zimbra.soap.mail.message.ModifyDataSourceRequest.class,
            com.zimbra.soap.mail.message.ModifyDataSourceResponse.class,
            com.zimbra.soap.mail.message.ModifyFilterRulesRequest.class,
            com.zimbra.soap.mail.message.ModifyFilterRulesResponse.class,
            com.zimbra.soap.mail.message.ModifyMailboxMetadataRequest.class,
            com.zimbra.soap.mail.message.ModifyMailboxMetadataResponse.class,
            com.zimbra.soap.mail.message.ModifyOutgoingFilterRulesRequest.class,
            com.zimbra.soap.mail.message.ModifyOutgoingFilterRulesResponse.class,
            com.zimbra.soap.mail.message.ModifySearchFolderRequest.class,
            com.zimbra.soap.mail.message.ModifySearchFolderResponse.class,
            com.zimbra.soap.mail.message.ModifyTaskRequest.class,
            com.zimbra.soap.mail.message.ModifyTaskResponse.class,
            com.zimbra.soap.mail.message.MsgActionRequest.class,
            com.zimbra.soap.mail.message.MsgActionResponse.class,
            com.zimbra.soap.mail.message.NoOpRequest.class,
            com.zimbra.soap.mail.message.NoOpResponse.class,
            com.zimbra.soap.mail.message.NoteActionRequest.class,
            com.zimbra.soap.mail.message.NoteActionResponse.class,
            com.zimbra.soap.mail.message.PurgeRevisionRequest.class,
            com.zimbra.soap.mail.message.PurgeRevisionResponse.class,
            com.zimbra.soap.mail.message.RankingActionRequest.class,
            com.zimbra.soap.mail.message.RankingActionResponse.class,
            com.zimbra.soap.mail.message.RegisterDeviceRequest.class,
            com.zimbra.soap.mail.message.RegisterDeviceResponse.class,
            com.zimbra.soap.mail.message.RemoveAttachmentsRequest.class,
            com.zimbra.soap.mail.message.RemoveAttachmentsResponse.class,
            com.zimbra.soap.mail.message.RevokePermissionRequest.class,
            com.zimbra.soap.mail.message.RevokePermissionResponse.class,
            com.zimbra.soap.mail.message.SaveDocumentRequest.class,
            com.zimbra.soap.mail.message.SaveDocumentResponse.class,
            com.zimbra.soap.mail.message.SaveDraftRequest.class,
            com.zimbra.soap.mail.message.SaveDraftResponse.class,
            com.zimbra.soap.mail.message.SearchConvRequest.class,
            com.zimbra.soap.mail.message.SearchConvResponse.class,
            com.zimbra.soap.mail.message.SearchRequest.class,
            com.zimbra.soap.mail.message.SearchResponse.class,
            com.zimbra.soap.mail.message.SendDeliveryReportRequest.class,
            com.zimbra.soap.mail.message.SendDeliveryReportResponse.class,
            com.zimbra.soap.mail.message.SendInviteReplyRequest.class,
            com.zimbra.soap.mail.message.SendInviteReplyResponse.class,
            com.zimbra.soap.mail.message.SendMsgRequest.class,
            com.zimbra.soap.mail.message.SendMsgResponse.class,
            com.zimbra.soap.mail.message.SendShareNotificationRequest.class,
            com.zimbra.soap.mail.message.SendShareNotificationResponse.class,
            com.zimbra.soap.mail.message.SendVerificationCodeRequest.class,
            com.zimbra.soap.mail.message.SendVerificationCodeResponse.class,
            com.zimbra.soap.mail.message.SetAppointmentRequest.class,
            com.zimbra.soap.mail.message.SetAppointmentResponse.class,
            com.zimbra.soap.mail.message.SetCustomMetadataRequest.class,
            com.zimbra.soap.mail.message.SetCustomMetadataResponse.class,
            com.zimbra.soap.mail.message.SetMailboxMetadataRequest.class,
            com.zimbra.soap.mail.message.SetMailboxMetadataResponse.class,
            com.zimbra.soap.mail.message.SetTaskRequest.class,
            com.zimbra.soap.mail.message.SetTaskResponse.class,
            com.zimbra.soap.mail.message.SnoozeCalendarItemAlarmRequest.class,
            com.zimbra.soap.mail.message.SnoozeCalendarItemAlarmResponse.class,
            com.zimbra.soap.mail.message.SyncRequest.class,
            com.zimbra.soap.mail.message.SyncResponse.class,
            com.zimbra.soap.mail.message.TagActionRequest.class,
            com.zimbra.soap.mail.message.TagActionResponse.class,
            com.zimbra.soap.mail.message.TestDataSourceRequest.class,
            com.zimbra.soap.mail.message.TestDataSourceResponse.class,
            com.zimbra.soap.mail.message.UpdateDeviceStatusRequest.class,
            com.zimbra.soap.mail.message.UpdateDeviceStatusResponse.class,
            com.zimbra.soap.mail.message.VerifyCodeRequest.class,
            com.zimbra.soap.mail.message.VerifyCodeResponse.class,
            com.zimbra.soap.mail.message.WaitSetRequest.class,
            com.zimbra.soap.mail.message.WaitSetResponse.class,

            // zimbraAdmin
            com.zimbra.soap.admin.message.AbortHsmRequest.class,
            com.zimbra.soap.admin.message.AbortHsmResponse.class,
            com.zimbra.soap.admin.message.AbortXMbxSearchRequest.class,
            com.zimbra.soap.admin.message.AbortXMbxSearchResponse.class,
            com.zimbra.soap.admin.message.ActivateLicenseRequest.class,
            com.zimbra.soap.admin.message.ActivateLicenseResponse.class,
            com.zimbra.soap.admin.message.AddAccountAliasRequest.class,
            com.zimbra.soap.admin.message.AddAccountAliasResponse.class,
            com.zimbra.soap.admin.message.AddAccountLoggerRequest.class,
            com.zimbra.soap.admin.message.AddAccountLoggerResponse.class,
            com.zimbra.soap.admin.message.AddDistributionListAliasRequest.class,
            com.zimbra.soap.admin.message.AddDistributionListAliasResponse.class,
            com.zimbra.soap.admin.message.AddDistributionListMemberRequest.class,
            com.zimbra.soap.admin.message.AddDistributionListMemberResponse.class,
            com.zimbra.soap.admin.message.AddGalSyncDataSourceRequest.class,
            com.zimbra.soap.admin.message.AddGalSyncDataSourceResponse.class,
            com.zimbra.soap.admin.message.AdminCreateWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminCreateWaitSetResponse.class,
            com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminDestroyWaitSetResponse.class,
            com.zimbra.soap.admin.message.AdminWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminWaitSetResponse.class,
            com.zimbra.soap.admin.message.AuthRequest.class,
            com.zimbra.soap.admin.message.AuthResponse.class,
            com.zimbra.soap.admin.message.AutoCompleteGalRequest.class,
            com.zimbra.soap.admin.message.AutoCompleteGalResponse.class,
            com.zimbra.soap.admin.message.AutoProvAccountRequest.class,
            com.zimbra.soap.admin.message.AutoProvAccountResponse.class,
            com.zimbra.soap.admin.message.AutoProvTaskControlRequest.class,
            com.zimbra.soap.admin.message.AutoProvTaskControlResponse.class,
            com.zimbra.soap.admin.message.BackupAccountQueryRequest.class,
            com.zimbra.soap.admin.message.BackupAccountQueryResponse.class,
            com.zimbra.soap.admin.message.BackupQueryRequest.class,
            com.zimbra.soap.admin.message.BackupQueryResponse.class,
            com.zimbra.soap.admin.message.BackupRequest.class,
            com.zimbra.soap.admin.message.BackupResponse.class,
            com.zimbra.soap.admin.message.CancelPendingRemoteWipeRequest.class,
            com.zimbra.soap.admin.message.CancelPendingRemoteWipeResponse.class,
            com.zimbra.soap.admin.message.CheckAuthConfigRequest.class,
            com.zimbra.soap.admin.message.CheckAuthConfigResponse.class,
            com.zimbra.soap.admin.message.CheckBlobConsistencyRequest.class,
            com.zimbra.soap.admin.message.CheckBlobConsistencyResponse.class,
            com.zimbra.soap.admin.message.CheckDirectoryRequest.class,
            com.zimbra.soap.admin.message.CheckDirectoryResponse.class,
            com.zimbra.soap.admin.message.CheckDomainMXRecordRequest.class,
            com.zimbra.soap.admin.message.CheckDomainMXRecordResponse.class,
            com.zimbra.soap.admin.message.CheckExchangeAuthRequest.class,
            com.zimbra.soap.admin.message.CheckExchangeAuthResponse.class,
            com.zimbra.soap.admin.message.CheckGalConfigRequest.class,
            com.zimbra.soap.admin.message.CheckGalConfigResponse.class,
            com.zimbra.soap.admin.message.CheckHealthRequest.class,
            com.zimbra.soap.admin.message.CheckHealthResponse.class,
            com.zimbra.soap.admin.message.CheckHostnameResolveRequest.class,
            com.zimbra.soap.admin.message.CheckHostnameResolveResponse.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthRequest.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthResponse.class,
            com.zimbra.soap.admin.message.CheckRightRequest.class,
            com.zimbra.soap.admin.message.CheckRightResponse.class,
            com.zimbra.soap.admin.message.ClearCookieRequest.class,
            com.zimbra.soap.admin.message.ClearCookieResponse.class,
            com.zimbra.soap.admin.message.ComputeAggregateQuotaUsageRequest.class,
            com.zimbra.soap.admin.message.ComputeAggregateQuotaUsageResponse.class,
            com.zimbra.soap.admin.message.ConfigureZimletRequest.class,
            com.zimbra.soap.admin.message.ConfigureZimletResponse.class,
            com.zimbra.soap.admin.message.CopyCosRequest.class,
            com.zimbra.soap.admin.message.CopyCosResponse.class,
            com.zimbra.soap.admin.message.CountAccountRequest.class,
            com.zimbra.soap.admin.message.CountAccountResponse.class,
            com.zimbra.soap.admin.message.CountObjectsRequest.class,
            com.zimbra.soap.admin.message.CountObjectsResponse.class,
            com.zimbra.soap.admin.message.CreateAccountRequest.class,
            com.zimbra.soap.admin.message.CreateAccountResponse.class,
            com.zimbra.soap.admin.message.CreateArchiveRequest.class,
            com.zimbra.soap.admin.message.CreateArchiveResponse.class,
            com.zimbra.soap.admin.message.CreateCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.CreateCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.CreateCosRequest.class,
            com.zimbra.soap.admin.message.CreateCosResponse.class,
            com.zimbra.soap.admin.message.CreateDataSourceRequest.class,
            com.zimbra.soap.admin.message.CreateDataSourceResponse.class,
            com.zimbra.soap.admin.message.CreateDistributionListRequest.class,
            com.zimbra.soap.admin.message.CreateDistributionListResponse.class,
            com.zimbra.soap.admin.message.CreateDomainRequest.class,
            com.zimbra.soap.admin.message.CreateDomainResponse.class,
            com.zimbra.soap.admin.message.CreateGalSyncAccountRequest.class,
            com.zimbra.soap.admin.message.CreateGalSyncAccountResponse.class,
            com.zimbra.soap.admin.message.CreateLDAPEntryRequest.class,
            com.zimbra.soap.admin.message.CreateLDAPEntryResponse.class,
            com.zimbra.soap.admin.message.CreateServerRequest.class,
            com.zimbra.soap.admin.message.CreateServerResponse.class,
            com.zimbra.soap.admin.message.CreateSystemRetentionPolicyRequest.class,
            com.zimbra.soap.admin.message.CreateSystemRetentionPolicyResponse.class,
            com.zimbra.soap.admin.message.CreateVolumeRequest.class,
            com.zimbra.soap.admin.message.CreateVolumeResponse.class,
            com.zimbra.soap.admin.message.CreateXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.CreateXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.CreateXMbxSearchRequest.class,
            com.zimbra.soap.admin.message.CreateXMbxSearchResponse.class,
            com.zimbra.soap.admin.message.CreateZimletRequest.class,
            com.zimbra.soap.admin.message.CreateZimletResponse.class,
            com.zimbra.soap.admin.message.DelegateAuthRequest.class,
            com.zimbra.soap.admin.message.DelegateAuthResponse.class,
            com.zimbra.soap.admin.message.DeleteAccountRequest.class,
            com.zimbra.soap.admin.message.DeleteAccountResponse.class,
            com.zimbra.soap.admin.message.DeleteCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.DeleteCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.DeleteCosRequest.class,
            com.zimbra.soap.admin.message.DeleteCosResponse.class,
            com.zimbra.soap.admin.message.DeleteDataSourceRequest.class,
            com.zimbra.soap.admin.message.DeleteDataSourceResponse.class,
            com.zimbra.soap.admin.message.DeleteDistributionListRequest.class,
            com.zimbra.soap.admin.message.DeleteDistributionListResponse.class,
            com.zimbra.soap.admin.message.DeleteDomainRequest.class,
            com.zimbra.soap.admin.message.DeleteDomainResponse.class,
            com.zimbra.soap.admin.message.DeleteGalSyncAccountRequest.class,
            com.zimbra.soap.admin.message.DeleteGalSyncAccountResponse.class,
            com.zimbra.soap.admin.message.DeleteLDAPEntryRequest.class,
            com.zimbra.soap.admin.message.DeleteLDAPEntryResponse.class,
            com.zimbra.soap.admin.message.DeleteMailboxRequest.class,
            com.zimbra.soap.admin.message.DeleteMailboxResponse.class,
            com.zimbra.soap.admin.message.DeleteServerRequest.class,
            com.zimbra.soap.admin.message.DeleteServerResponse.class,
            com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyRequest.class,
            com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyResponse.class,
            com.zimbra.soap.admin.message.DeleteVolumeRequest.class,
            com.zimbra.soap.admin.message.DeleteVolumeResponse.class,
            com.zimbra.soap.admin.message.DeleteXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.DeleteXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.DeleteXMbxSearchRequest.class,
            com.zimbra.soap.admin.message.DeleteXMbxSearchResponse.class,
            com.zimbra.soap.admin.message.DeleteZimletRequest.class,
            com.zimbra.soap.admin.message.DeleteZimletResponse.class,
            com.zimbra.soap.admin.message.DeployZimletRequest.class,
            com.zimbra.soap.admin.message.DeployZimletResponse.class,
            com.zimbra.soap.admin.message.DisableArchiveRequest.class,
            com.zimbra.soap.admin.message.DisableArchiveResponse.class,
            com.zimbra.soap.admin.message.DumpSessionsRequest.class,
            com.zimbra.soap.admin.message.DumpSessionsResponse.class,
            com.zimbra.soap.admin.message.EnableArchiveRequest.class,
            com.zimbra.soap.admin.message.EnableArchiveResponse.class,
            com.zimbra.soap.admin.message.ExportAndDeleteItemsRequest.class,
            com.zimbra.soap.admin.message.ExportAndDeleteItemsResponse.class,
            com.zimbra.soap.admin.message.ExportMailboxRequest.class,
            com.zimbra.soap.admin.message.ExportMailboxResponse.class,
            com.zimbra.soap.admin.message.FailoverClusterServiceRequest.class,
            com.zimbra.soap.admin.message.FailoverClusterServiceResponse.class,
            com.zimbra.soap.admin.message.FixCalendarEndTimeRequest.class,
            com.zimbra.soap.admin.message.FixCalendarEndTimeResponse.class,
            com.zimbra.soap.admin.message.FixCalendarPriorityRequest.class,
            com.zimbra.soap.admin.message.FixCalendarPriorityResponse.class,
            com.zimbra.soap.admin.message.FixCalendarTZRequest.class,
            com.zimbra.soap.admin.message.FixCalendarTZResponse.class,
            com.zimbra.soap.admin.message.FlushCacheRequest.class,
            com.zimbra.soap.admin.message.FlushCacheResponse.class,
            com.zimbra.soap.admin.message.GenCSRRequest.class,
            com.zimbra.soap.admin.message.GenCSRResponse.class,
            com.zimbra.soap.admin.message.GetAccountInfoRequest.class,
            com.zimbra.soap.admin.message.GetAccountInfoResponse.class,
            com.zimbra.soap.admin.message.GetAccountLoggersRequest.class,
            com.zimbra.soap.admin.message.GetAccountLoggersResponse.class,
            com.zimbra.soap.admin.message.GetAccountMembershipRequest.class,
            com.zimbra.soap.admin.message.GetAccountMembershipResponse.class,
            com.zimbra.soap.admin.message.GetAccountRequest.class,
            com.zimbra.soap.admin.message.GetAccountResponse.class,
            com.zimbra.soap.admin.message.GetAdminConsoleUICompRequest.class,
            com.zimbra.soap.admin.message.GetAdminConsoleUICompResponse.class,
            com.zimbra.soap.admin.message.GetAdminExtensionZimletsRequest.class,
            com.zimbra.soap.admin.message.GetAdminExtensionZimletsResponse.class,
            com.zimbra.soap.admin.message.GetAdminSavedSearchesRequest.class,
            com.zimbra.soap.admin.message.GetAdminSavedSearchesResponse.class,
            com.zimbra.soap.admin.message.GetAggregateQuotaUsageOnServerRequest.class,
            com.zimbra.soap.admin.message.GetAggregateQuotaUsageOnServerResponse.class,
            com.zimbra.soap.admin.message.GetAllAccountLoggersRequest.class,
            com.zimbra.soap.admin.message.GetAllAccountLoggersResponse.class,
            com.zimbra.soap.admin.message.GetAllAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllCalendarResourcesRequest.class,
            com.zimbra.soap.admin.message.GetAllCalendarResourcesResponse.class,
            com.zimbra.soap.admin.message.GetAllConfigRequest.class,
            com.zimbra.soap.admin.message.GetAllConfigResponse.class,
            com.zimbra.soap.admin.message.GetAllCosRequest.class,
            com.zimbra.soap.admin.message.GetAllCosResponse.class,
            com.zimbra.soap.admin.message.GetAllDistributionListsRequest.class,
            com.zimbra.soap.admin.message.GetAllDistributionListsResponse.class,
            com.zimbra.soap.admin.message.GetAllDomainsRequest.class,
            com.zimbra.soap.admin.message.GetAllDomainsResponse.class,
            com.zimbra.soap.admin.message.GetAllEffectiveRightsRequest.class,
            com.zimbra.soap.admin.message.GetAllEffectiveRightsResponse.class,
            com.zimbra.soap.admin.message.GetAllFreeBusyProvidersRequest.class,
            com.zimbra.soap.admin.message.GetAllFreeBusyProvidersResponse.class,
            com.zimbra.soap.admin.message.GetAllLocalesRequest.class,
            com.zimbra.soap.admin.message.GetAllLocalesResponse.class,
            com.zimbra.soap.admin.message.GetAllMailboxesRequest.class,
            com.zimbra.soap.admin.message.GetAllMailboxesResponse.class,
            com.zimbra.soap.admin.message.GetAllRightsRequest.class,
            com.zimbra.soap.admin.message.GetAllRightsResponse.class,
            com.zimbra.soap.admin.message.GetAllServersRequest.class,
            com.zimbra.soap.admin.message.GetAllServersResponse.class,
            com.zimbra.soap.admin.message.GetAllVolumesRequest.class,
            com.zimbra.soap.admin.message.GetAllVolumesResponse.class,
            com.zimbra.soap.admin.message.GetAllXMPPComponentsRequest.class,
            com.zimbra.soap.admin.message.GetAllXMPPComponentsResponse.class,
            com.zimbra.soap.admin.message.GetAllZimletsRequest.class,
            com.zimbra.soap.admin.message.GetAllZimletsResponse.class,
            com.zimbra.soap.admin.message.GetApplianceHSMFSRequest.class,
            com.zimbra.soap.admin.message.GetApplianceHSMFSResponse.class,
            com.zimbra.soap.admin.message.GetAttributeInfoRequest.class,
            com.zimbra.soap.admin.message.GetAttributeInfoResponse.class,
            com.zimbra.soap.admin.message.GetCSRRequest.class,
            com.zimbra.soap.admin.message.GetCSRResponse.class,
            com.zimbra.soap.admin.message.GetCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.GetCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.GetCertRequest.class,
            com.zimbra.soap.admin.message.GetCertResponse.class,
            com.zimbra.soap.admin.message.GetClusterStatusRequest.class,
            com.zimbra.soap.admin.message.GetClusterStatusResponse.class,
            com.zimbra.soap.admin.message.GetConfigRequest.class,
            com.zimbra.soap.admin.message.GetConfigResponse.class,
            com.zimbra.soap.admin.message.GetCosRequest.class,
            com.zimbra.soap.admin.message.GetCosResponse.class,
            com.zimbra.soap.admin.message.GetCreateObjectAttrsRequest.class,
            com.zimbra.soap.admin.message.GetCreateObjectAttrsResponse.class,
            com.zimbra.soap.admin.message.GetCurrentVolumesRequest.class,
            com.zimbra.soap.admin.message.GetCurrentVolumesResponse.class,
            com.zimbra.soap.admin.message.GetDataSourcesRequest.class,
            com.zimbra.soap.admin.message.GetDataSourcesResponse.class,
            com.zimbra.soap.admin.message.GetDelegatedAdminConstraintsRequest.class,
            com.zimbra.soap.admin.message.GetDelegatedAdminConstraintsResponse.class,
            com.zimbra.soap.admin.message.GetDevicesCountRequest.class,
            com.zimbra.soap.admin.message.GetDevicesCountResponse.class,
            com.zimbra.soap.admin.message.GetDevicesCountSinceLastUsedRequest.class,
            com.zimbra.soap.admin.message.GetDevicesCountSinceLastUsedResponse.class,
            com.zimbra.soap.admin.message.GetDevicesCountUsedTodayRequest.class,
            com.zimbra.soap.admin.message.GetDevicesCountUsedTodayResponse.class,
            com.zimbra.soap.admin.message.GetDevicesRequest.class,
            com.zimbra.soap.admin.message.GetDevicesResponse.class,
            com.zimbra.soap.admin.message.GetDeviceStatusRequest.class,
            com.zimbra.soap.admin.message.GetDeviceStatusResponse.class,
            com.zimbra.soap.admin.message.GetDistributionListMembershipRequest.class,
            com.zimbra.soap.admin.message.GetDistributionListMembershipResponse.class,
            com.zimbra.soap.admin.message.GetDistributionListRequest.class,
            com.zimbra.soap.admin.message.GetDistributionListResponse.class,
            com.zimbra.soap.admin.message.GetDomainInfoRequest.class,
            com.zimbra.soap.admin.message.GetDomainInfoResponse.class,
            com.zimbra.soap.admin.message.GetDomainRequest.class,
            com.zimbra.soap.admin.message.GetDomainResponse.class,
            com.zimbra.soap.admin.message.GetEffectiveRightsRequest.class,
            com.zimbra.soap.admin.message.GetEffectiveRightsResponse.class,
            com.zimbra.soap.admin.message.GetFreeBusyQueueInfoRequest.class,
            com.zimbra.soap.admin.message.GetFreeBusyQueueInfoResponse.class,
            com.zimbra.soap.admin.message.GetGrantsRequest.class,
            com.zimbra.soap.admin.message.GetGrantsResponse.class,
            com.zimbra.soap.admin.message.GetHsmStatusRequest.class,
            com.zimbra.soap.admin.message.GetHsmStatusResponse.class,
            com.zimbra.soap.admin.message.GetLDAPEntriesRequest.class,
            com.zimbra.soap.admin.message.GetLDAPEntriesResponse.class,
            com.zimbra.soap.admin.message.GetLicenseInfoRequest.class,
            com.zimbra.soap.admin.message.GetLicenseInfoResponse.class,
            com.zimbra.soap.admin.message.GetLicenseRequest.class,
            com.zimbra.soap.admin.message.GetLicenseResponse.class,
            com.zimbra.soap.admin.message.GetLoggerStatsRequest.class,
            com.zimbra.soap.admin.message.GetLoggerStatsResponse.class,
            com.zimbra.soap.admin.message.GetMailQueueInfoRequest.class,
            com.zimbra.soap.admin.message.GetMailQueueInfoResponse.class,
            com.zimbra.soap.admin.message.GetMailQueueRequest.class,
            com.zimbra.soap.admin.message.GetMailQueueResponse.class,
            com.zimbra.soap.admin.message.GetMailboxRequest.class,
            com.zimbra.soap.admin.message.GetMailboxResponse.class,
            com.zimbra.soap.admin.message.GetMailboxStatsRequest.class,
            com.zimbra.soap.admin.message.GetMailboxStatsResponse.class,
            com.zimbra.soap.admin.message.GetMailboxVersionRequest.class,
            com.zimbra.soap.admin.message.GetMailboxVersionResponse.class,
            com.zimbra.soap.admin.message.GetMailboxVolumesRequest.class,
            com.zimbra.soap.admin.message.GetMailboxVolumesResponse.class,
            com.zimbra.soap.admin.message.GetMemcachedClientConfigRequest.class,
            com.zimbra.soap.admin.message.GetMemcachedClientConfigResponse.class,
            com.zimbra.soap.admin.message.GetQuotaUsageRequest.class,
            com.zimbra.soap.admin.message.GetQuotaUsageResponse.class,
            com.zimbra.soap.admin.message.GetRightRequest.class,
            com.zimbra.soap.admin.message.GetRightResponse.class,
            com.zimbra.soap.admin.message.GetRightsDocRequest.class,
            com.zimbra.soap.admin.message.GetRightsDocResponse.class,
            com.zimbra.soap.admin.message.GetSMIMEConfigRequest.class,
            com.zimbra.soap.admin.message.GetSMIMEConfigResponse.class,
            com.zimbra.soap.admin.message.GetServerNIfsRequest.class,
            com.zimbra.soap.admin.message.GetServerNIfsResponse.class,
            com.zimbra.soap.admin.message.GetServerRequest.class,
            com.zimbra.soap.admin.message.GetServerResponse.class,
            com.zimbra.soap.admin.message.GetServerStatsRequest.class,
            com.zimbra.soap.admin.message.GetServerStatsResponse.class,
            com.zimbra.soap.admin.message.GetServiceStatusRequest.class,
            com.zimbra.soap.admin.message.GetServiceStatusResponse.class,
            com.zimbra.soap.admin.message.GetSessionsRequest.class,
            com.zimbra.soap.admin.message.GetSessionsResponse.class,
            com.zimbra.soap.admin.message.GetShareInfoRequest.class,
            com.zimbra.soap.admin.message.GetShareInfoResponse.class,
            com.zimbra.soap.admin.message.GetSystemRetentionPolicyRequest.class,
            com.zimbra.soap.admin.message.GetSystemRetentionPolicyResponse.class,
            com.zimbra.soap.admin.message.GetVersionInfoRequest.class,
            com.zimbra.soap.admin.message.GetVersionInfoResponse.class,
            com.zimbra.soap.admin.message.GetVolumeRequest.class,
            com.zimbra.soap.admin.message.GetVolumeResponse.class,
            com.zimbra.soap.admin.message.GetXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.GetXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.GetXMbxSearchesListRequest.class,
            com.zimbra.soap.admin.message.GetXMbxSearchesListResponse.class,
            com.zimbra.soap.admin.message.GetZimletRequest.class,
            com.zimbra.soap.admin.message.GetZimletResponse.class,
            com.zimbra.soap.admin.message.GetZimletStatusRequest.class,
            com.zimbra.soap.admin.message.GetZimletStatusResponse.class,
            com.zimbra.soap.admin.message.GrantRightRequest.class,
            com.zimbra.soap.admin.message.GrantRightResponse.class,
            com.zimbra.soap.admin.message.HsmRequest.class,
            com.zimbra.soap.admin.message.HsmResponse.class,
            com.zimbra.soap.admin.message.InjectStaticFilesRequest.class,
            com.zimbra.soap.admin.message.InjectStaticFilesResponse.class,
            com.zimbra.soap.admin.message.InstallCertRequest.class,
            com.zimbra.soap.admin.message.InstallCertResponse.class,
            com.zimbra.soap.admin.message.InstallLicenseRequest.class,
            com.zimbra.soap.admin.message.InstallLicenseResponse.class,
            com.zimbra.soap.admin.message.MailQueueActionRequest.class,
            com.zimbra.soap.admin.message.MailQueueActionResponse.class,
            com.zimbra.soap.admin.message.MailQueueFlushRequest.class,
            com.zimbra.soap.admin.message.MailQueueFlushResponse.class,
            com.zimbra.soap.admin.message.MigrateAccountRequest.class,
            com.zimbra.soap.admin.message.MigrateAccountResponse.class,
            com.zimbra.soap.admin.message.ModifyAccountRequest.class,
            com.zimbra.soap.admin.message.ModifyAccountResponse.class,
            com.zimbra.soap.admin.message.ModifyAdminSavedSearchesRequest.class,
            com.zimbra.soap.admin.message.ModifyAdminSavedSearchesResponse.class,
            com.zimbra.soap.admin.message.ModifyCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.ModifyCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.ModifyConfigRequest.class,
            com.zimbra.soap.admin.message.ModifyConfigResponse.class,
            com.zimbra.soap.admin.message.ModifyCosRequest.class,
            com.zimbra.soap.admin.message.ModifyCosResponse.class,
            com.zimbra.soap.admin.message.ModifyDataSourceRequest.class,
            com.zimbra.soap.admin.message.ModifyDataSourceResponse.class,
            com.zimbra.soap.admin.message.ModifyDelegatedAdminConstraintsRequest.class,
            com.zimbra.soap.admin.message.ModifyDelegatedAdminConstraintsResponse.class,
            com.zimbra.soap.admin.message.ModifyDistributionListRequest.class,
            com.zimbra.soap.admin.message.ModifyDistributionListResponse.class,
            com.zimbra.soap.admin.message.ModifyDomainRequest.class,
            com.zimbra.soap.admin.message.ModifyDomainResponse.class,
            com.zimbra.soap.admin.message.ModifyLDAPEntryRequest.class,
            com.zimbra.soap.admin.message.ModifyLDAPEntryResponse.class,
            com.zimbra.soap.admin.message.ModifySMIMEConfigRequest.class,
            com.zimbra.soap.admin.message.ModifySMIMEConfigResponse.class,
            com.zimbra.soap.admin.message.ModifyServerRequest.class,
            com.zimbra.soap.admin.message.ModifyServerResponse.class,
            com.zimbra.soap.admin.message.ModifySystemRetentionPolicyRequest.class,
            com.zimbra.soap.admin.message.ModifySystemRetentionPolicyResponse.class,
            com.zimbra.soap.admin.message.ModifyVolumeRequest.class,
            com.zimbra.soap.admin.message.ModifyVolumeResponse.class,
            com.zimbra.soap.admin.message.ModifyZimletRequest.class,
            com.zimbra.soap.admin.message.ModifyZimletResponse.class,
            com.zimbra.soap.admin.message.MoveBlobsRequest.class,
            com.zimbra.soap.admin.message.MoveBlobsResponse.class,
            com.zimbra.soap.admin.message.MoveMailboxRequest.class,
            com.zimbra.soap.admin.message.MoveMailboxResponse.class,
            com.zimbra.soap.admin.message.NoOpRequest.class,
            com.zimbra.soap.admin.message.NoOpResponse.class,
            com.zimbra.soap.admin.message.PingRequest.class,
            com.zimbra.soap.admin.message.PingResponse.class,
            com.zimbra.soap.admin.message.PurgeAccountCalendarCacheRequest.class,
            com.zimbra.soap.admin.message.PurgeAccountCalendarCacheResponse.class,
            com.zimbra.soap.admin.message.PurgeFreeBusyQueueRequest.class,
            com.zimbra.soap.admin.message.PurgeFreeBusyQueueResponse.class,
            com.zimbra.soap.admin.message.PurgeMessagesRequest.class,
            com.zimbra.soap.admin.message.PurgeMessagesResponse.class,
            com.zimbra.soap.admin.message.PurgeMovedMailboxRequest.class,
            com.zimbra.soap.admin.message.PurgeMovedMailboxResponse.class,
            com.zimbra.soap.admin.message.PushFreeBusyRequest.class,
            com.zimbra.soap.admin.message.PushFreeBusyResponse.class,
            com.zimbra.soap.admin.message.QueryMailboxMoveRequest.class,
            com.zimbra.soap.admin.message.QueryMailboxMoveResponse.class,
            com.zimbra.soap.admin.message.QueryWaitSetRequest.class,
            com.zimbra.soap.admin.message.QueryWaitSetResponse.class,
            com.zimbra.soap.admin.message.ReIndexRequest.class,
            com.zimbra.soap.admin.message.ReIndexResponse.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsRequest.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsResponse.class,
            com.zimbra.soap.admin.message.RegisterMailboxMoveOutRequest.class,
            com.zimbra.soap.admin.message.RegisterMailboxMoveOutResponse.class,
            com.zimbra.soap.admin.message.ReloadAccountRequest.class,
            com.zimbra.soap.admin.message.ReloadAccountResponse.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigRequest.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigResponse.class,
            com.zimbra.soap.admin.message.ReloadMemcachedClientConfigRequest.class,
            com.zimbra.soap.admin.message.ReloadMemcachedClientConfigResponse.class,
            com.zimbra.soap.admin.message.RemoteWipeRequest.class,
            com.zimbra.soap.admin.message.RemoteWipeResponse.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasRequest.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasResponse.class,
            com.zimbra.soap.admin.message.RemoveAccountLoggerRequest.class,
            com.zimbra.soap.admin.message.RemoveAccountLoggerResponse.class,
            com.zimbra.soap.admin.message.RemoveDistributionListAliasRequest.class,
            com.zimbra.soap.admin.message.RemoveDistributionListAliasResponse.class,
            com.zimbra.soap.admin.message.RemoveDistributionListMemberRequest.class,
            com.zimbra.soap.admin.message.RemoveDistributionListMemberResponse.class,
            com.zimbra.soap.admin.message.RenameAccountRequest.class,
            com.zimbra.soap.admin.message.RenameAccountResponse.class,
            com.zimbra.soap.admin.message.RenameCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.RenameCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.RenameCosRequest.class,
            com.zimbra.soap.admin.message.RenameCosResponse.class,
            com.zimbra.soap.admin.message.RenameDistributionListRequest.class,
            com.zimbra.soap.admin.message.RenameDistributionListResponse.class,
            com.zimbra.soap.admin.message.RenameLDAPEntryRequest.class,
            com.zimbra.soap.admin.message.RenameLDAPEntryResponse.class,
            com.zimbra.soap.admin.message.ResetAllLoggersRequest.class,
            com.zimbra.soap.admin.message.ResetAllLoggersResponse.class,
            com.zimbra.soap.admin.message.RestoreRequest.class,
            com.zimbra.soap.admin.message.RestoreResponse.class,
            com.zimbra.soap.admin.message.RevokeRightRequest.class,
            com.zimbra.soap.admin.message.RevokeRightResponse.class,
            com.zimbra.soap.admin.message.RolloverRedoLogRequest.class,
            com.zimbra.soap.admin.message.RolloverRedoLogResponse.class,
            com.zimbra.soap.admin.message.RunUnitTestsRequest.class,
            com.zimbra.soap.admin.message.RunUnitTestsResponse.class,
            com.zimbra.soap.admin.message.ScheduleBackupsRequest.class,
            com.zimbra.soap.admin.message.ScheduleBackupsResponse.class,
            com.zimbra.soap.admin.message.SearchAccountsRequest.class,
            com.zimbra.soap.admin.message.SearchAccountsResponse.class,
            com.zimbra.soap.admin.message.SearchAutoProvDirectoryRequest.class,
            com.zimbra.soap.admin.message.SearchAutoProvDirectoryResponse.class,
            com.zimbra.soap.admin.message.SearchCalendarResourcesRequest.class,
            com.zimbra.soap.admin.message.SearchCalendarResourcesResponse.class,
            com.zimbra.soap.admin.message.SearchDirectoryRequest.class,
            com.zimbra.soap.admin.message.SearchDirectoryResponse.class,
            com.zimbra.soap.admin.message.SearchGalRequest.class,
            com.zimbra.soap.admin.message.SearchGalResponse.class,
            com.zimbra.soap.admin.message.SearchMultiMailboxRequest.class,
            com.zimbra.soap.admin.message.SearchMultiMailboxResponse.class,
            com.zimbra.soap.admin.message.SetCurrentVolumeRequest.class,
            com.zimbra.soap.admin.message.SetCurrentVolumeResponse.class,
            com.zimbra.soap.admin.message.SetPasswordRequest.class,
            com.zimbra.soap.admin.message.SetPasswordResponse.class,
            com.zimbra.soap.admin.message.SyncGalAccountRequest.class,
            com.zimbra.soap.admin.message.SyncGalAccountResponse.class,
            com.zimbra.soap.admin.message.UndeployZimletRequest.class,
            com.zimbra.soap.admin.message.UndeployZimletResponse.class,
            com.zimbra.soap.admin.message.UnloadMailboxRequest.class,
            com.zimbra.soap.admin.message.UnloadMailboxResponse.class,
            com.zimbra.soap.admin.message.UnregisterMailboxMoveOutRequest.class,
            com.zimbra.soap.admin.message.UnregisterMailboxMoveOutResponse.class,
            com.zimbra.soap.admin.message.UpdateDeviceStatusRequest.class,
            com.zimbra.soap.admin.message.UpdateDeviceStatusResponse.class,
            com.zimbra.soap.admin.message.UploadDomCertRequest.class,
            com.zimbra.soap.admin.message.UploadDomCertResponse.class,
            com.zimbra.soap.admin.message.UploadProxyCARequest.class,
            com.zimbra.soap.admin.message.UploadProxyCAResponse.class,
            com.zimbra.soap.admin.message.VerifyCertKeyRequest.class,
            com.zimbra.soap.admin.message.VerifyCertKeyResponse.class,
            com.zimbra.soap.admin.message.VerifyIndexRequest.class,
            com.zimbra.soap.admin.message.VerifyIndexResponse.class,
            com.zimbra.soap.admin.message.VersionCheckRequest.class,
            com.zimbra.soap.admin.message.VersionCheckResponse.class,

            // zimbraAdminExt
            com.zimbra.soap.adminext.message.BulkIMAPDataImportRequest.class,
            com.zimbra.soap.adminext.message.BulkIMAPDataImportResponse.class,
            com.zimbra.soap.adminext.message.BulkImportAccountsRequest.class,
            com.zimbra.soap.adminext.message.BulkImportAccountsResponse.class,
            com.zimbra.soap.adminext.message.GenerateBulkProvisionFileFromLDAPRequest.class,
            com.zimbra.soap.adminext.message.GenerateBulkProvisionFileFromLDAPResponse.class,
            com.zimbra.soap.adminext.message.GetBulkIMAPImportTaskListRequest.class,
            com.zimbra.soap.adminext.message.GetBulkIMAPImportTaskListResponse.class,
            com.zimbra.soap.adminext.message.PurgeBulkIMAPImportTasksRequest.class,
            com.zimbra.soap.adminext.message.PurgeBulkIMAPImportTasksResponse.class,

            // zimbraRepl
            com.zimbra.soap.replication.message.BecomeMasterRequest.class,
            com.zimbra.soap.replication.message.BecomeMasterResponse.class,
            com.zimbra.soap.replication.message.BringDownServiceIPRequest.class,
            com.zimbra.soap.replication.message.BringDownServiceIPResponse.class,
            com.zimbra.soap.replication.message.BringUpServiceIPRequest.class,
            com.zimbra.soap.replication.message.BringUpServiceIPResponse.class,
            com.zimbra.soap.replication.message.ReplicationStatusRequest.class,
            com.zimbra.soap.replication.message.ReplicationStatusResponse.class,
            com.zimbra.soap.replication.message.StartCatchupRequest.class,
            com.zimbra.soap.replication.message.StartCatchupResponse.class,
            com.zimbra.soap.replication.message.StartFailoverClientRequest.class,
            com.zimbra.soap.replication.message.StartFailoverClientResponse.class,
            com.zimbra.soap.replication.message.StartFailoverDaemonRequest.class,
            com.zimbra.soap.replication.message.StartFailoverDaemonResponse.class,
            com.zimbra.soap.replication.message.StopFailoverClientRequest.class,
            com.zimbra.soap.replication.message.StopFailoverClientResponse.class,
            com.zimbra.soap.replication.message.StopFailoverDaemonRequest.class,
            com.zimbra.soap.replication.message.StopFailoverDaemonResponse.class,

            // zimbraSync
            com.zimbra.soap.sync.message.CancelPendingRemoteWipeRequest.class,
            com.zimbra.soap.sync.message.CancelPendingRemoteWipeResponse.class,
            com.zimbra.soap.sync.message.GetDeviceStatusRequest.class,
            com.zimbra.soap.sync.message.GetDeviceStatusResponse.class,
            com.zimbra.soap.sync.message.RemoteWipeRequest.class,
            com.zimbra.soap.sync.message.RemoteWipeResponse.class,
            com.zimbra.soap.sync.message.RemoveDeviceRequest.class,
            com.zimbra.soap.sync.message.RemoveDeviceResponse.class,
            com.zimbra.soap.sync.message.ResumeDeviceRequest.class,
            com.zimbra.soap.sync.message.ResumeDeviceResponse.class,
            com.zimbra.soap.sync.message.SuspendDeviceRequest.class,
            com.zimbra.soap.sync.message.SuspendDeviceResponse.class,

            // zimbraAppblast
            com.zimbra.soap.appblast.message.EditDocumentRequest.class,
            com.zimbra.soap.appblast.message.EditDocumentResponse.class,
            com.zimbra.soap.appblast.message.FinishEditDocumentRequest.class,
            com.zimbra.soap.appblast.message.FinishEditDocumentResponse.class
        };

        try {
            JAXB_CONTEXT = JAXBContext.newInstance(MESSAGE_CLASSES);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to initialize JAXB", e);
        }
    }

    private JaxbUtil() {
    }

    public static ImmutableList<Class<?>> getJaxbRequestAndResponseClasses() {
        return ImmutableList.<Class<?>>builder().add(MESSAGE_CLASSES).build();
    }

    /**
     * JAXB marshaling creates XML which makes heavy use of namespace prefixes.  Historical Zimbra SOAP XML
     * has generally not used them inside the SOAP body.  JAXB uses randomly assigned prefixes such as "ns2" which
     * makes the XML ugly and verbose.  If {@link removePrefixes} is set then all namespace prefix usage is
     * expunged from the XML.
     *
     * @param o - associated JAXB class must have an @XmlRootElement annotation
     * @param factory - e.g. XmlElement.mFactory or JSONElement.mFactory
     * @param removePrefixes - If true then remove namepace prefixes from unmarshalled XML.
     * @return
     * @throws ServiceException
     */
    public static Element jaxbToElement(Object o, Element.ElementFactory factory, boolean removePrefixes)
    throws ServiceException {
        try {
            Marshaller marshaller = getContext().createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            DocumentResult dr = new DocumentResult();
            marshaller.marshal(o, dr);
            Document theDoc = dr.getDocument();
            org.dom4j.Element rootElem = theDoc.getRootElement();
            if (removePrefixes) {
                    JaxbUtil.removeNamespacePrefixes(rootElem);
            }
            return Element.convertDOM(rootElem, factory);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Unable to convert " +
                    o.getClass().getName() + " to Element", e);
        }
    }

    /**
     * @param o - associated JAXB class must have an @XmlRootElement annotation
     * @param factory - e.g. XmlElement.mFactory or JSONElement.mFactory
     * @return
     * @throws ServiceException
     */
    public static Element jaxbToElement(Object o, Element.ElementFactory factory)
    throws ServiceException {
        return JaxbUtil.jaxbToElement(o, factory, true);
    }

    public static Element jaxbToElement(Object o) throws ServiceException {
        return jaxbToElement(o, XMLElement.mFactory);
    }

    /**
     * Use namespace inheritance in preference to prefixes
     * @param elem
     * @param defaultNs
     */
    private static void removeNamespacePrefixes(org.dom4j.Element elem) {
        Namespace elemNs = elem.getNamespace();
        if (elemNs != null) {
            if (! Strings.isNullOrEmpty(elemNs.getPrefix())) {
                Namespace newNs = Namespace.get(elemNs.getURI());
                org.dom4j.QName newQName = new org.dom4j.QName(elem.getName(), newNs);
                elem.setQName(newQName);
            }
        }
        Iterator<?> elemIter = elem.elementIterator();
        while (elemIter.hasNext()) {
            JaxbUtil.removeNamespacePrefixes((org.dom4j.Element) elemIter.next());
        }
    }

    private static JAXBContext getJaxbContext(Class<?> klass)
    throws JAXBException {
        JAXBContext jaxb = null;
        if (JaxbUtil.classJaxbContexts == null) {
            JaxbUtil.classJaxbContexts = Maps.newHashMap();
        }
        if (JaxbUtil.classJaxbContexts.containsKey(klass)) {
            jaxb = JaxbUtil.classJaxbContexts.get(klass);
        } else {
            jaxb = JAXBContext.newInstance(klass);
        }
        return jaxb;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Element jaxbToNamedElement(String name, String namespace,
            Object o, Element.ElementFactory factory)
    throws ServiceException {
        try {
            Marshaller marshaller = createMarshaller(o.getClass());
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            DocumentResult dr = new DocumentResult();
            marshaller.marshal(new JAXBElement(new QName(namespace, name),
                    o.getClass(), o) , dr);
            Document theDoc = dr.getDocument();
            org.dom4j.Element rootElem = theDoc.getRootElement();
            return Element.convertDOM(rootElem, factory);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Unable to convert " +
                    o.getClass().getName() + " to Element", e);
        }
    }

    public static Element addChildElementFromJaxb(Element parent,
                String name, String namespace, Object o) {
        Element.ElementFactory factory;
        if (parent instanceof XMLElement) {
            factory = XMLElement.mFactory;
        } else {
            factory = JSONElement.mFactory;
        }
        Element child = null;
        try {
            child = jaxbToNamedElement(name, namespace, o, factory);
        } catch (ServiceException e) {
            ZimbraLog.misc.info("JAXB Problem making " + name + " element", e);
        }
        parent.addElement(child);
        return child;
    }

    /**
     * This appears to be safe but is fairly slow.
     * Note that this method does NOT support Zimbra's greater flexibility
     * for Xml structure.  Something similar to {@link fixupStructureForJaxb}
     * will be needed to add such support.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingByteArray(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            return (T) unmarshaller.unmarshal(new ByteArrayInputStream(
                    rootElem.asXML().getBytes(Charsets.UTF_8)));
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    /**
     * This appears to work if e is an XMLElement but sometimes fails badly if
     * e is a JSONElement - get:
     * javax.xml.bind.UnmarshalException: Namespace URIs and local names
     *      to the unmarshaller needs to be interned.
     * and that seems to make the unmarshaller unstable from then on :-(
     * Note that this method does NOT support Zimbra's greater flexibility
     * for Xml structure.  Something similar to {@link fixupStructureForJaxb}
     * will be needed to add such support.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingDom4j(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            DocumentSource docSrc = new DocumentSource(rootElem);
            return (T) unmarshaller.unmarshal(docSrc);
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    public static boolean isJaxbType(Class<?> klass) {
        if ( (klass == null) || klass.isPrimitive()) {
            return false;
        }
        return klass.getName().startsWith("com.zimbra.soap") || klass.getName().startsWith("com.zimbra.doc.soap");
    }

    /**
     * Manipulates a structure under {@link elem} which obeys Zimbra's
     * SOAP XML structure rules to comply with more stringent JAXB rules.
     * e.g. Zimbra allows attributes to be specified as elements.
     * @param klass is the JAXB class for {@link elem} which must be under
     * the "com.zimbra.soap" package hierarchy.
     */
    private static void fixupStructureForJaxb(org.w3c.dom.Element elem, Class<?> klass) {
        if (elem == null) {
            return;
        }
        if (klass == null) {
            LOG.debug("JAXB no class associated with " + elem.getLocalName());
            return;
        }
        if (!isJaxbType(klass)) {
            return;
        }

        JaxbInfo jaxbInfo = JaxbInfo.getFromCache(klass);
        NodeList list = elem.getChildNodes();
        List<org.w3c.dom.Element> orphans = null;
        for (int i=0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.ELEMENT_NODE) {
                org.w3c.dom.Element child = (org.w3c.dom.Element) subnode;
                String childName = child.getLocalName();
                if (jaxbInfo.hasWrapperElement(childName)) {
                    NodeList wrappedList = child.getChildNodes();
                    for (int j=0; j < wrappedList.getLength(); j++) {
                        Node wSubnode = wrappedList.item(j);
                        if (wSubnode.getNodeType() == Node.ELEMENT_NODE) {
                            org.w3c.dom.Element wChild = (org.w3c.dom.Element) wSubnode;
                            fixupStructureForJaxb(wChild,
                                    jaxbInfo.getClassForWrappedElement(childName, wChild.getLocalName()));
                        }
                    }
                } else if (jaxbInfo.hasElement(childName))  {
                    fixupStructureForJaxb(child, jaxbInfo.getClassForElement(childName));
                } else if (jaxbInfo.hasAttribute(childName)) {
                    elem.setAttribute(childName, child.getTextContent());
                    // Don't remove pre-existing child until later pass to avoid changing the list of child elements
                    if (orphans == null) {
                        orphans = Lists.newArrayList();
                    }
                    orphans.add(child);
                } else {
                    LOG.debug("JAXB class " + klass.getName() + " does NOT recognise element named:" + childName);
                }
            }
        }
        // Prune the promoted elements from the list of children
        if (orphans != null) {
            for (org.w3c.dom.Element orphan : orphans) {
                elem.removeChild(orphan);
            }
        }
    }

    private static Class<?> classForTopLevelElem(Element elem) {
        String className = null;
        try {
            String ns = elem.getQName().getNamespaceURI();
            if (AdminConstants.NAMESPACE_STR.equals(ns)) {
                className = ADMIN_JAXB_PACKAGE  + "." + elem.getName();
            } else if (AccountConstants.NAMESPACE_STR.equals(ns)) {
                className = ACCOUNT_JAXB_PACKAGE  + "." + elem.getName();
            } else if (MailConstants.NAMESPACE_STR.equals(ns)) {
                className = MAIL_JAXB_PACKAGE  + "." + elem.getName();
            } else {
                LOG.info("Unexpected namespace[" + ns + "]");
                return null;
            }
            Class<?> klass = Class.forName(className);
            if (klass == null) {
                LOG.info("Failed to find CLASS for classname=[" + className + "]");
                return null;
            }
            return klass;
        } catch (NullPointerException npe) {
            LOG.info("Problem finding JAXB package", npe);
            return null;
        } catch (ClassNotFoundException cnfe) {
            LOG.info("Problem finding JAXB class", cnfe);
            return null;
        }
    }

    /**
     * @param elem represents a structure which may only match Zimbra's more
     * relaxed rules rather than stringent JAXB rules.
     * e.g. Zimbra allows attributes to be specified as elements.
     * @param klass is the JAXB class for {@link elem}
     * @return a JAXB object
     */
    @SuppressWarnings("unchecked")
    private static <T> T w3cDomDocToJaxb(org.w3c.dom.Document doc,
            Class<?> klass, boolean jaxbClassInContext)
    throws ServiceException {
        fixupStructureForJaxb(doc.getDocumentElement(), klass);
        return (T) rawW3cDomDocToJaxb(doc, klass, jaxbClassInContext);
    }

    /**
     * Return a JAXB object.  This implementation uses a org.w3c.dom.Document
     * as an intermediate representation.  This appears to be more reliable
     * than using a DocumentSource based on org.dom4j.Element
     * @param klass is the JAXB class for {@link doc}
     * @param jaxbClassInContext is true if {@link klass} is the JAXB class
     * for a request or response object that is in {@link JaxbUtil.MESSAGE_CLASSES}
     */
    @SuppressWarnings("unchecked")
    private static <T> T rawW3cDomDocToJaxb(org.w3c.dom.Document doc,
            Class<?> klass, boolean jaxbClassInContext)
    throws ServiceException {
        if ((doc == null || doc.getDocumentElement() == null)) {
            return null;
        }
        try {
            // LOG.warn("Dom to Xml:\n" + domToString(doc));
            Unmarshaller unmarshaller;
            if (jaxbClassInContext) {
                unmarshaller = getContext().createUnmarshaller();
                return (T) unmarshaller.unmarshal(doc);
            } else {
                org.w3c.dom.Element docElem = doc.getDocumentElement();
                unmarshaller = createUnmarshaller(klass);
                JAXBElement<T> ret =
                    (JAXBElement<T>) unmarshaller.unmarshal(docElem, klass);
                return ret.getValue();
            }
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE("Unable to unmarshal response for " +
                    doc.getDocumentElement().getNodeName(), ex);
        }
    }

    /**
     * Return a JAXB object.  This implementation uses a org.w3c.dom.Document
     * as an intermediate representation.  This appears to be more reliable
     * than using a DocumentSource based on org.dom4j.Element
     * @param klass is the JAXB class for {@link elem}
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxb(Element elem, Class<?> klass)
    throws ServiceException {
        return (T) w3cDomDocToJaxb(elem.toW3cDom(), klass, false);
    }

    /**
     * Return a JAXB object corresponding to {@link e} which is the Xml for
     * a Request or Response.
     * @param e MUST be a top level Request or Response element whose
     * corresponding JAXB object is in {@link JaxbUtil.MESSAGE_CLASSES}
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxb(Element e) throws ServiceException {
        Class<?> klass = classForTopLevelElem(e);
        if (klass == null) {
            LOG.info("Failed to find CLASS for name=[" + e.getName() +
                    "]  Is it a Request or Response node?");
            return null;
        }
        return (T) w3cDomDocToJaxb(e.toW3cDom(), klass, true);
    }

    public static String domToString(org.w3c.dom.Document document) {
        try {
            Source xmlSource = new DOMSource(document);
            StreamResult result = new StreamResult(new ByteArrayOutputStream());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes"); //Java XML Indent
            transformer.transform(xmlSource, result);
            return result.getOutputStream().toString();
        } catch (TransformerFactoryConfigurationError factoryError) {
            LOG.error("Error creating TransformerFactory", factoryError);
        } catch (TransformerException transformerError) {
            LOG.error( "Error transforming document", transformerError);
        }
        return null;
    }

    /**
     * Only for use when marshalling request or response objects.
     */
    public static Marshaller createMarshaller() {
        try {
            return getContext().createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Only for use when unmarshalling request or response objects.
     */
    public static Unmarshaller createUnmarshaller() {
        try {
            return getContext().createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Marshaller createMarshaller(Class<?> klass) {
        try {
            JAXBContext jaxb = getJaxbContext(klass);
            return jaxb.createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Unmarshaller createUnmarshaller(Class<?> klass) {
        try {
            JAXBContext jaxb = getJaxbContext(klass);
            return jaxb.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private static JAXBContext getContext() {
        if (JAXB_CONTEXT == null) {
            throw new IllegalStateException("JAXB has not been initialized");
        }
        return JAXB_CONTEXT;
    }
}