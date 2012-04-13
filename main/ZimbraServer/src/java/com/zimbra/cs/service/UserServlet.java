/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthTokenEncoded;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.admin.AdminAccessControl;
import com.zimbra.cs.service.formatter.Formatter;
import com.zimbra.cs.service.formatter.FormatterFactory;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.formatter.IfbFormatter;
import com.zimbra.cs.service.formatter.OctopusPatchFormatter;
import com.zimbra.cs.service.formatter.TarFormatter;
import com.zimbra.cs.service.formatter.ZipFormatter;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.UserServletUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;

/**
 *
 * <pre>
 *   http://server/service/home/[&tilde;][{username}]/[{folder}]?[{query-params}]
 *          fmt={ics, csv, etc}
 *          id={item-id}
 *          imap_id={item-imap-id}
 *          part={mime-part}
 *          query={search-query}
 *          types={types} // when searching
 *          auth={auth-types}
 *          start={time}
 *          end={time}
 *          sync="1"
 *
 *             {types}   = comma-separated list.  Legal values are:
 *                         appointment|chat|contact|conversation|document|
 *                         message|note|tag|task|wiki
 *                         (default is &quot;conversation&quot;)
 *
 *             {auth-types} = comma-separated list. Legal values are:
 *                            co     cookie
 *                            ba     basic auth
 *                            nsc    do not set a cookie when using basic auth
 *                            (default is &quot;co,ba&quot;, i.e. check both)
 *
 *            {time} = (time in milliseconds) |
 *                     YYYY/dd/mm |
 *                     mm/dd/YYYY |
 *                     [-]nnnn{minute,hour,day,week,month,year}  // relative
 * </pre>
 */

public class UserServlet extends ZimbraServlet {
    private static final long serialVersionUID = -5313094316561384586L;

    public static final String SERVLET_PATH = "/home";

    public static final String QP_AUTHTOKEN = "authToken";

    public static final String QP_FMT = "fmt"; // format query param

    public static final String QP_NOHIERARCHY = "nohierarchy"; // nohierarchy query param

    public static final String QP_ZLV = "zlv"; // zip level query param

    public static final String QP_ID = "id"; // id query param

    public static final String QP_LIST = "list"; // list query param

    public static final String QP_IMAP_ID = "imap_id"; // IMAP id query param

    public static final String QP_PART = "part"; // part query param

    /**
     * Body query param.  Also used by {@link ZipFormatter} and {@link TarFormatter} to specify whether
     * the entire message should be returned (<tt>body=1</tt>), or just the headers (<tt>body=0</tt>).
     * The default is <tt>1</tt>.
     */
    public static final String QP_BODY = "body"; // body query param

    public static final String BODY_TEXT = "text"; // return text body

    public static final String BODY_HTML = "html"; // return html body if possible

    public static final String QP_QUERY = "query"; // query query param

    public static final String QP_VIEW = "view"; // view query param

    public static final String QP_TYPES = "types"; // types

    public static final String QP_START = "start"; // start time

    public static final String QP_END = "end"; // end time

    public static final String QP_FREEBUSY_CALENDAR = "fbcal";  // calendar folder to run free/busy search on

    public static final String QP_IGNORE_ERROR = "ignore";  // ignore and continue on error during ics import

    public static final String QP_PRESERVE_ALARMS = "preserveAlarms";  // preserve existing alarms during ics import

    public static final String QP_OFFSET = "offset"; // offset into results

    public static final String QP_LIMIT = "limit"; // offset into results

    public static final String QP_AUTH = "auth"; // auth types

    public static final String QP_DISP = "disp"; // disposition (a = attachment, i = inline)

    public static final String QP_NAME = "name"; // filename/path segments, added to pathInfo

    public static final String QP_CSVFORMAT = "csvfmt"; // csv type (outlook-2003-csv, yahoo-csv, ...)

    public static final String QP_CSVLOCALE = "csvlocale"; // refining locale for csvfmt - e.g. zh-CN

    public static final String QP_CSVSEPARATOR = "csvsep"; // separator

    public static final String QP_VERSION = "ver";  // version for WikiItem and Document

    public static final String QP_HISTORY = "history";  // history for WikiItem

    public static final String QP_LANGUAGE = "language"; // all three

    public static final String QP_COUNTRY = "country"; // all three

    public static final String QP_VARIANT = "variant"; // all three

    public static final String UPLOAD_NAME = "uploadName"; // upload filename

    public static final String UPLOAD_TYPE = "uploadType"; // upload content type

    public static final String QP_FBFORMAT = "fbfmt"; // free/busy format - "fb" (default) or "event"

    /**
     * Used by {@link OctopusPatchFormatter}
     */
    public static final String QP_MANIFEST = "manifest"; // selects whether server returns patch manifest or not

    /**
     * Used by {@link TarFormatter} to specify whether the <tt>.meta</tt>
     * files should be added to the tarball (<tt>meta=1</tt>) or not (<tt>meta=0</tt>).
     * The default is <tt>1</tt>.
     */
    public static final String QP_META = "meta";

    /**
     * Used by {@link IfbFormatter} to specify the UID of calendar item to exclude when computing free/busy.
     */
    public static final String QP_EXUID = "exuid";

    public static final String AUTH_COOKIE = "co"; // auth by cookie

    public static final String AUTH_BASIC = "ba"; // basic auth

    public static final String AUTH_QUERYPARAM = "qp"; // query parameter

    public static final String AUTH_NO_SET_COOKIE = "nsc"; // don't set auth token cookie after basic auth
                                                           // same as ba after bug 42782

    // see https://bugzilla.zimbra.com/show_bug.cgi?id=42782#c11
    public static final String AUTH_SET_COOKIE = "sc"; // set auth token cookie after basic auth

    public static final String AUTH_DEFAULT = "co,nsc,qp"; // all three

    public static final String HTTP_URL = "http_url";
    public static final String HTTP_STATUS_CODE = "http_code";

    public static final String QP_MAX_WIDTH = "max_width";

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";

    public static final Log log = LogFactory.getLog(UserServlet.class);

    /** Returns the REST URL for the account. */
    public static String getRestUrl(Account acct) throws ServiceException {
        return getServiceUrl(acct, UserServlet.SERVLET_PATH);
    }

    /** Returns the REST URL for the mail item. */
    public static String getRestUrl(MailItem item) throws ServiceException {
        Account acct = item.getMailbox().getAccount();
        return getRestUrl(acct) + HttpUtil.urlEscape(item.getPath());
    }

    public static Formatter getFormatter(FormatType type) {
        return FormatterFactory.mFormatters.get(type);
    }

    private void sendError(UserServletContext ctxt, HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        if(resp.isCommitted()) {
            log.info("Response already committed. Skipping sending error code for response");
            return;
        }
        if (ctxt != null &&!ctxt.cookieAuthHappened && ctxt.basicAuthAllowed() && !ctxt.basicAuthHappened) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserServletContext context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = new UserServletContext(req, resp, this);
            if (!checkAuthentication(context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            checkTargetAccountStatus(context);

            if (proxyIfNecessary(req, resp, context)) {
                return;
            }
            // at this point context.authAccount is set either from the Cookie,
            // or from basic auth.  if there was no credential in either the Cookie
            // or basic auth, authAccount is set to anonymous account.
            if (context.getAuthAccount() != null) {
                ZimbraLog.addAccountNameToContext(context.getAuthAccount().getName());
            }

            doAuthGet(req, resp, context);

        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED ||
                se instanceof NoSuchItemException)
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errNoSuchItem, req));
            else if (se.getCode() == AccountServiceException.MAINTENANCE_MODE ||
                     se.getCode() == AccountServiceException.ACCOUNT_INACTIVE)
                sendError(context, req, resp, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private boolean checkAuthentication(UserServletContext context)
        throws IOException, ServletException, UserServletException {

        // if they specify /~/, we must auth
        if (context.targetAccount == null && context.accountPath.equals("~")) {
            UserServletUtil.getAccount(context);
            if (context.getAuthAccount() == null) {
                return false;
            }
            context.targetAccount = context.getAuthAccount();
        }

        // need this before proxy if we want to support sending cookie from a basic-auth
        UserServletUtil.getAccount(context);
        if (context.getAuthAccount() == null) {
            context.setAnonymousRequest();
        }
        return true;
    }

    private void checkTargetAccountStatus(UserServletContext context) throws ServiceException {
        if (context.targetAccount != null) {
            String acctStatus = context.targetAccount.getAccountStatus(Provisioning.getInstance());

            // no one can touch an account if it in maintenance mode
            if (Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(acctStatus))
                throw AccountServiceException.MAINTENANCE_MODE();

            // allow only admin access if the account is not active
            if (!Provisioning.ACCOUNT_STATUS_ACTIVE.equals(acctStatus) && !(context.authToken != null &&
                    (context.authToken.isDelegatedAuth() ||
                            AdminAccessControl.isAdequateAdminAccount(context.getAuthAccount())))) {
                throw AccountServiceException.ACCOUNT_INACTIVE(context.targetAccount.getName());
            }
        }
    }

    private static AuthToken getProxyAuthToken(UserServletContext context) throws ServiceException {
        String encoded = Provisioning.getInstance().getProxyAuthToken(context.targetAccount.getId(), null);
        if (encoded != null) {
            return new ZimbraAuthTokenEncoded(encoded);
        } else if (context.basicAuthHappened) {
            return context.authToken;
        } else {
            return null;
        }
    }

    private boolean proxyIfNecessary(HttpServletRequest req, HttpServletResponse resp, UserServletContext context) throws IOException, ServiceException {
        // this should handle both explicit /user/user-on-other-server/ and
        // /user/~/?id={account-id-on-other-server}:id

        if (context.targetAccount != null && !Provisioning.onLocalServer(context.targetAccount)) {
            proxyServletRequest(req, resp, Provisioning.getInstance().getServer(context.targetAccount), getProxyAuthToken(context));
            return true;
        }

        return false;
    }

    /**
     * Constructs the exteral url for a mount point. This gets the link back to the correct server without need for proxying it
     * @param authToken
     * @param mpt The mount point to create the url for
     * @return The url for the mountpoint/share that goes back to the original user/share/server
     * @throws ServiceException
     */
    public static String getExternalRestUrl(OperationContext octxt, Mountpoint mpt) throws ServiceException {
        AuthToken authToken = octxt.getAuthToken();
        // check to see if it is a local mount point, if it is there's
        // no need to do anything
        if(mpt.isLocal()) {
            return null;
        }

        String folderPath = null;

        // Figure out the target server from the target user's account.
        // This will let us get the correct server/port
        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null) {
            // Remote owner account has been deleted.
            return null;
        }
        Server targetServer = prov.getServer(targetAccount);


        // Avoid the soap call if its a local mailbox
        if (Provisioning.onLocalServer(targetAccount)) {
            Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccountId(targetAccount.getId());
            if(mailbox == null){
                // no mailbox (shouldn't happen normally)
                return null;
            }
            // Get the folder from the mailbox
            Folder folder = mailbox.getFolderById(octxt, mpt.getRemoteId());
            if(folder == null) {
                return null;
            }
            folderPath = folder.getPath();
        } else {
            // The remote server case
            // Get the target user's mailbox..
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(targetAccount));
            zoptions.setTargetAccount(mpt.getOwnerId());
            zoptions.setTargetAccountBy(AccountBy.id);
            zoptions.setNoSession(true);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            if(zmbx == null) {
                // we didn't manage to get a mailbox
                return null;
            }

            // Get an instance of their folder so we can build the path correctly
             ZFolder folder = zmbx.getFolderById(mpt.getTarget().toString(authToken.getAccount().getId()));
            // if for some reason we can't find the folder, return null
            if(folder == null){
                return null;
            }
            folderPath = folder.getPath();
        }
        // For now we'll always use SSL
        return URLUtil.getServiceURL(targetServer, SERVLET_PATH + HttpUtil.urlEscape(getAccountPath(targetAccount) +folderPath) , true);
    }

    /**
     * Constructs the exteral url for a mount point. This gets the link back to the correct server without need for proxying it
     * @param authToken
     * @param mpt The mount point to create the url for
     * @return The url for the mountpoint/share that goes back to the original user/share/server
     * @throws ServiceException
     */
    public static String getExternalRestUrl(Folder folder) throws ServiceException {
        // Figure out the target server from the target user's account.
        // This will let us get the correct server/port
        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = folder.getAccount();

        Server targetServer = prov.getServer(targetAccount);

        // For now we'll always use SSL
        return URLUtil.getServiceURL(targetServer, SERVLET_PATH + HttpUtil.urlEscape(getAccountPath(targetAccount) +folder.getPath()) , true);
    }

    private void doAuthGet(HttpServletRequest req, HttpServletResponse resp, UserServletContext context)
    throws ServletException, IOException, ServiceException, UserServletException {
        if (log.isDebugEnabled()) {
            StringBuffer reqURL = context.req.getRequestURL();
            String queryParam = context.req.getQueryString();
            if (queryParam != null) reqURL.append('?').append(queryParam);
            log.debug("UserServlet: " + reqURL.toString());
        }

        context.opContext = new OperationContext(context.getAuthAccount(), isAdminRequest(req));
        Mailbox mbox = UserServletUtil.getTargetMailbox(context);
        if (mbox != null) {
            ZimbraLog.addMboxToContext(mbox.getId());
            if (context.reqListIds != null) {
                UserServletUtil.resolveItems(context);
            } else {
                MailItem item = UserServletUtil.resolveItem(context, true);
                if (isProxyRequest(req, resp, context, item)) {
                    // if the target is a mountpoint, proxy the request on to the resolved target
                    return;
                }
            }
        }

        resolveFormatter(context);

        // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
        // return auth error instead of "no such mailbox".  If request/formatter doesn't require
        // authentication, call the formatter and let it deal with preventing harvest attacks.
        if (mbox == null && context.formatter.requiresAuth())
            throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

        context.formatter.format(context);
    }

    /**
     * Adds an item to a folder specified in the URI.  The item content is provided in the PUT request's body.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    /**
     * Adds an item to a folder specified in the URI.  The item content is provided in the POST request's body.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserServletContext context = null;
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        try {
            context = new UserServletContext(req, resp, this);
            if (!checkAuthentication(context)) {
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errMustAuthenticate, req));
                return;
            }

            checkTargetAccountStatus(context);

            if (proxyIfNecessary(req, resp, context))
                return;

            if (context.getAuthAccount() != null) {
                ZimbraLog.addAccountNameToContext(context.getAuthAccount().getName());
            }
            Folder folder = null;
            String filename = null;
            Mailbox mbox = UserServletUtil.getTargetMailbox(context);
            if (mbox != null) {
                ZimbraLog.addMboxToContext(mbox.getId());

                log.info("POST: " + context.req.getRequestURL().toString());

                context.opContext = new OperationContext(context.getAuthAccount(), isAdminRequest(req));

                try {
                    context.target = UserServletUtil.resolveItem(context, false);
                } catch (NoSuchItemException nsie) {
                    // perhaps it's a POST to "Notebook/new-file-name" -- find the parent folder and proceed from there
                    if (context.itemPath == null)
                        throw nsie;
                    int separator = context.itemPath.lastIndexOf('/');
                    if (separator <= 0)
                        throw nsie;
                    filename = context.itemPath.substring(separator + 1);
                    context.itemPath = context.itemPath.substring(0, separator);
                    context.target = UserServletUtil.resolveItem(context, false);
                    context.extraPath = filename;
                }

                folder = (context.target instanceof Folder ? (Folder) context.target : mbox.getFolderById(context.opContext, context.target.getFolderId()));

                if (context.target != folder) {
                    if (filename == null)
                        filename = context.target.getName();
                    else
                        // need to fail on POST to "Notebook/existing-file/random-cruft"
                        throw MailServiceException.NO_SUCH_FOLDER(context.itemPath);
                }

                if (isProxyRequest(req, resp, context, folder)) {
                    // if the target is a mountpoint, proxy the request on to the resolved target
                    return;
                }
            }

            // if they specified a filename, default to the native formatter
            if (context.format == null && filename != null)
                context.format = FormatType.HTML_CONVERTED;

            String ctype = context.req.getContentType();

            // if no format explicitly specified, try to guess it from the Content-Type header
            if (context.format == null && ctype != null) {
                String normalizedType = new ContentType(ctype).getContentType();
                Formatter fmt = FormatterFactory.mDefaultFormatters.get(normalizedType);
                if (fmt != null)
                    context.format = fmt.getType();
            }

            context.target = folder;
            resolveFormatter(context);
            if (!context.formatter.supportsSave())
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, req));

            // Prevent harvest attacks.  If mailbox doesn't exist for a request requiring authentication,
            // return auth error instead of "no such mailbox".  If request/formatter doesn't require
            // authentication, call the formatter and let it deal with preventing harvest attacks.
            if (mbox == null && context.formatter.requiresAuth())
                throw ServiceException.PERM_DENIED(L10nUtil.getMessage(MsgKey.errPermissionDenied, req));

            context.formatter.save(context, ctype, folder, filename);
        } catch (ServiceException se) {
            if (se.getCode() == ServiceException.PERM_DENIED ||
                se instanceof NoSuchItemException)
                sendError(context, req, resp, L10nUtil.getMessage(MsgKey.errNoSuchItem, req));
            else if (se.getCode() == AccountServiceException.MAINTENANCE_MODE ||
                     se.getCode() == AccountServiceException.ACCOUNT_INACTIVE)
               sendError(context, req, resp, se.getMessage());
            else
                throw new ServletException(se);
        } catch (UserServletException e) {
            // add check for ServiceException root cause?
            resp.sendError(e.getHttpStatusCode(), e.getMessage());
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /** Determines the <code>format</code> and <code>formatter<code> for the
     *  request, if not already set. */
    private void resolveFormatter(UserServletContext context) throws UserServletException {
        if (context.format == null) {
            context.format = defaultFormat(context);
            if (context.format == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
        }

        if (context.formatter == null) {
            context.formatter = FormatterFactory.mFormatters.get(context.format);
            if (context.formatter == null)
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errUnsupportedFormat, context.req));
        }
    }

    private boolean isProxyRequest(HttpServletRequest req, HttpServletResponse resp, UserServletContext context, MailItem item)
    throws IOException, ServiceException, UserServletException {
        if (!(item instanceof Mountpoint))
            return false;
        if (context.format != null && context.format.equals("html"))
            return false;
        Mountpoint mpt = (Mountpoint) item;

        String uri = SERVLET_PATH + "/~/?" + QP_ID + '=' + HttpUtil.urlEscape(mpt.getOwnerId()) + "%3A" + mpt.getRemoteId();
        if (context.format != null)
            uri += '&' + QP_FMT + '=' + HttpUtil.urlEscape(context.format.toString());
        if (context.extraPath != null)
            uri += '&' + QP_NAME + '=' + HttpUtil.urlEscape(context.extraPath);
        for (Map.Entry<String, String> entry : HttpUtil.getURIParams(req).entrySet()) {
            String qp = entry.getKey();
            if (!qp.equals(QP_ID) && !qp.equals(QP_FMT))
                uri += '&' + HttpUtil.urlEscape(qp) + '=' + HttpUtil.urlEscape(entry.getValue());
        }

        Provisioning prov = Provisioning.getInstance();
        Account targetAccount = prov.get(AccountBy.id, mpt.getOwnerId());
        if (targetAccount == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, L10nUtil.getMessage(MsgKey.errNoSuchAccount, req));

        proxyServletRequest(req, resp, prov.getServer(targetAccount), uri, getProxyAuthToken(context));
        return true;
    }

    private static HashSet<String> ZIMBRA_DOC_CONTENT_TYPE = new HashSet<String>();
    static {
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-doc");
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-slides");
        ZIMBRA_DOC_CONTENT_TYPE.add("application/x-zimbra-xls");
    }

    private FormatType defaultFormat(UserServletContext context) {
        if (context.hasPart()) {
            return FormatType.HTML_CONVERTED;
        }
        MailItem.Type type = MailItem.Type.UNKNOWN;
        if (context.target instanceof Folder)
            type = ((Folder) context.target).getDefaultView();
        else if (context.target != null)
            type = context.target.getType();

        switch (type) {
        case APPOINTMENT:
        case TASK:
            return FormatType.ICS;
        case CONTACT:
            return context.target instanceof Folder? FormatType.CSV : FormatType.VCF;
        case DOCUMENT:
            // Zimbra docs and folder rendering should use html formatter.
            if (context.target instanceof Folder)
                return FormatType.HTML;
            String contentType = ((Document)context.target).getContentType();
            if (contentType != null && contentType.indexOf(';') > 0)
                contentType = contentType.substring(0, contentType.indexOf(';')).toLowerCase();
            if (ZIMBRA_DOC_CONTENT_TYPE.contains(contentType))
                return FormatType.HTML;
            return FormatType.HTML_CONVERTED;
        default:
            return FormatType.HTML_CONVERTED;
        }
    }

    @Override
    public void init() throws ServletException {
        log.info("Starting up");
        super.init();
    }

    @Override
    public void destroy() {
        log.info("Shutting down");
        super.destroy();
    }

    public static byte[] getRemoteContent(AuthToken authToken, ItemId iid, Map<String, String> params) throws ServiceException {
        Account target = Provisioning.getInstance().get(AccountBy.id, iid.getAccountId(), authToken);
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());
        return getRemoteContent(authToken, target, (String)null, pcopy);
    }

    public static byte[] getRemoteContent(AuthToken authToken, Account target, String folder, Map<String,String> params) throws ServiceException {
        return getRemoteContent(authToken.toZAuthToken(), getRemoteUrl(target, folder, params));
    }

    public static byte[] getRemoteContent(ZAuthToken authToken, String url) throws ServiceException {
        return getRemoteResource(authToken, url).getSecond();
    }

    public static HttpInputStream getRemoteContentAsStream(AuthToken authToken, Account target, String folder, Map<String,String> params) throws ServiceException, IOException {
        String url = getRemoteUrl(target, folder, params);
        return getRemoteResourceAsStream(authToken.toZAuthToken(), url).getSecond();
    }

    private static String getRemoteUrl(Account target, String folder, Map<String, String> params) throws ServiceException {
        if (folder == null) {
            folder = "";
        } else {
            if (folder.endsWith("/"))
                folder = folder.substring(0, folder.length() - 1);
            if (folder.startsWith("/"))
                folder = folder.substring(1);
        }

        Server server = Provisioning.getInstance().getServer(target);
        StringBuffer url = new StringBuffer(getProxyUrl(null, server, SERVLET_PATH + getAccountPath(target)));
        if (folder.length() > 0)
            url.append("/").append(folder);
        url.append("/?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet())
                url.append('&').append(HttpUtil.urlEscape(param.getKey())).append('=').append(HttpUtil.urlEscape(param.getValue()));
        }
        return url.toString();
    }

    public static Pair<Header[], byte[]> getRemoteResource(ZAuthToken authToken, String url) throws ServiceException {
        HttpMethod get = null;
        try {
            Pair<Header[], HttpMethod> pair = doHttpOp(authToken, new GetMethod(url));
            get = pair.getSecond();
            return new Pair<Header[], byte[]>(pair.getFirst(), get.getResponseBody());
        } catch (IOException x) {
            throw ServiceException.FAILURE("Can't read response body " + url, x);
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
    }

    public static FileUploadServlet.Upload getRemoteResourceAsUpload(AuthToken at, ItemId iid, Map<String,String> params)
    throws ServiceException, IOException {
        Map<String, String> pcopy = new HashMap<String, String>(params);
        pcopy.put(QP_ID, iid.toString());

        // fetch from remote store
        Provisioning prov = Provisioning.getInstance();
        Account target = prov.get(AccountBy.id, iid.getAccountId(), at);
        String url = getRemoteUrl(target, null, pcopy);

        Pair<Header[], HttpInputStream> response = getRemoteResourceAsStream(at.toZAuthToken(), url);

        // and save the result as an upload
        String ctype = "text/plain", filename = null;
        for (Header hdr : response.getFirst()) {
            String hname = hdr.getName().toLowerCase();
            if (hname.equals("content-type"))
                ctype = hdr.getValue();
            else if (hname.equals("content-disposition"))
                filename = new ContentDisposition(hdr.getValue()).getParameter("filename");
        }
        if (filename == null || filename.equals(""))
            filename = new ContentType(ctype).getParameter("name");
        if (filename == null || filename.equals(""))
            filename = "unknown";
        return FileUploadServlet.saveUpload(response.getSecond(), filename, ctype, at.getAccountId());
    }


    /** Helper class so that we can close connection upon stream close */
    public static class HttpInputStream extends FilterInputStream {
        private final HttpMethod method;

        public HttpInputStream(HttpMethod m) throws IOException {
            super(m.getResponseBodyAsStream());
            this.method = m;
        }
        public int getContentLength() {
            String cl = getHeader("Content-Length");
            if (cl != null)
                return Integer.parseInt(cl);
            return -1;
        }
        public String getHeader(String headerName) {
            Header cl = method.getResponseHeader(headerName);
            if (cl != null)
                return cl.getValue();
            return null;
        }
        public int getStatusCode() {
            return method.getStatusCode();
        }
        @Override public void close() {
            method.releaseConnection();
        }
    }

    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, ItemId iid, String extraPath)
            throws ServiceException, IOException {
        Map<String,String> params = new HashMap<String,String>();
        params.put(QP_ID, iid.toString());
        if (extraPath != null)
            params.put(QP_NAME, extraPath);
        Account target = Provisioning.getInstance().getAccountById(iid.getAccountId());
        String url = getRemoteUrl(target, null, params);
        return getRemoteResourceAsStream(authToken, url);
    }

    public static Pair<Integer, InputStream> getRemoteResourceAsStreamWithLength(ZAuthToken authToken, String url) throws ServiceException, IOException {
        HttpInputStream his = getRemoteResourceAsStream(authToken, url).getSecond();
        return new Pair<Integer, InputStream>(his.getContentLength(), his);
    }

    public static Pair<Header[], HttpInputStream> getRemoteResourceAsStream(ZAuthToken authToken, String url)
    throws ServiceException, IOException {
        Pair<Header[], HttpMethod> pair = doHttpOp(authToken, new GetMethod(url));
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    public static Pair<Header[], HttpInputStream> putMailItem(ZAuthToken authToken, String url, MailItem item)
    throws ServiceException, IOException {
        if (item instanceof Document) {
            Document doc = (Document) item;
            StringBuilder u = new StringBuilder(url);
            u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
            if (doc.getType() == MailItem.Type.WIKI) {
                u.append("&fmt=wiki");
            }
            PutMethod method = new PutMethod(u.toString());
            String contentType = doc.getContentType();
            method.addRequestHeader("Content-Type", contentType);
            method.setRequestEntity(new InputStreamRequestEntity(doc.getContentStream(), doc.getSize(), contentType));
            method = HttpClientUtil.addInputStreamToHttpMethod(method, doc.getContentStream(), doc.getSize(), contentType);
            method.addRequestHeader("X-Zimbra-Description", doc.getDescription());
            method.setRequestEntity(new InputStreamRequestEntity(doc.getContentStream(), doc.getSize(), contentType));
            Pair<Header[], HttpMethod> pair = doHttpOp(authToken, method);
            return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
        }
        return putRemoteResource(authToken, url, item.getContentStream(), null);
    }

    public static Pair<Header[], HttpInputStream> putRemoteResource(
            AuthToken authToken, String url, InputStream req, Header[] headers)
            throws ServiceException, IOException {
        return putRemoteResource(authToken.toZAuthToken(), url, req, headers);
    }

    public static Pair<Header[], HttpInputStream> putRemoteResource(ZAuthToken authToken, String url, InputStream req, Header[] headers)
    throws ServiceException, IOException {
        StringBuilder u = new StringBuilder(url);
        u.append("?").append(QP_AUTH).append('=').append(AUTH_COOKIE);
        PutMethod method = new PutMethod(u.toString());
        String contentType = "application/octet-stream";
        if (headers != null) {
            for (Header hdr : headers) {
                String name = hdr.getName();
                method.addRequestHeader(hdr);
                if (name.equals("Content-Type"))
                    contentType = hdr.getValue();
            }
        }
        method.setRequestEntity(new InputStreamRequestEntity(req, contentType));
        Pair<Header[], HttpMethod> pair = doHttpOp(authToken, method);
        return new Pair<Header[], HttpInputStream>(pair.getFirst(), new HttpInputStream(pair.getSecond()));
    }

    private static Pair<Header[], HttpMethod> doHttpOp(ZAuthToken authToken, HttpMethod method)
    throws ServiceException {
        // create an HTTP client with the same cookies
        String url = "";
        String hostname = "";
        try {
            url = method.getURI().toString();
            hostname = method.getURI().getHost();
        } catch (IOException e) {
            log.warn("can't parse target URI", e);
        }

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        Map<String, String> cookieMap = authToken.cookieMap(false);
        if (cookieMap != null) {
            HttpState state = new HttpState();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                state.addCookie(new org.apache.commons.httpclient.Cookie(hostname, ck.getKey(), ck.getValue(), "/", null, false));
            }
            client.setState(state);
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        }

        if (method instanceof PutMethod) {
            long contentLength = ((PutMethod)method).getRequestEntity().getContentLength();
            if (contentLength > 0) {
                int timeEstimate = Math.max(10000, (int)(contentLength / 100));  // 100kbps in millis
                // cannot set connection time using our ZimbrahttpConnectionManager,
                // see comments in ZimbrahttpConnectionManager.
                // actually, length of the content to Put should not be a factor for
                // establishing a connection, only read time out matter, which we set
                // client.getHttpConnectionManager().getParams().setConnectionTimeout(timeEstimate);

                method.getParams().setSoTimeout(timeEstimate);
            }
        }

        try {
            int statusCode = HttpClientUtil.executeMethod(client, method);
            if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_FORBIDDEN)
                throw MailServiceException.NO_SUCH_ITEM(-1);
            else if (statusCode != HttpStatus.SC_OK &&
                    statusCode != HttpStatus.SC_CREATED &&
                    statusCode != HttpStatus.SC_NO_CONTENT)
                throw ServiceException.RESOURCE_UNREACHABLE(method.getStatusText(), null,
                        new ServiceException.InternalArgument(HTTP_URL, url, ServiceException.Argument.Type.STR),
                        new ServiceException.InternalArgument(HTTP_STATUS_CODE, statusCode, ServiceException.Argument.Type.NUM));

            List<Header> headers = new ArrayList<Header>(Arrays.asList(method.getResponseHeaders()));
            headers.add(new Header("X-Zimbra-Http-Status", ""+statusCode));
            return new Pair<Header[], HttpMethod>(headers.toArray(new Header[0]), method);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException while fetching " + url, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException while fetching " + url, e);
        }
    }
}