package com.zimbra.qa.selenium.projects.ajax.tests.calendar.appointments.views.day;

import java.util.*;

import org.testng.annotations.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.calendar.DialogConfirmDeleteAppointment;


public class DeleteAppointment extends AjaxCommonTest {

	public DeleteAppointment() {
		logger.info("New "+ DeleteAppointment.class.getCanonicalName());
		
		// All tests start at the Calendar page
		super.startingPage = app.zPageCalendar;

		// Make sure we are using an account with message view
		super.startingAccountPreferences = new HashMap<String, String>() {
			private static final long serialVersionUID = -2913827779459595178L;
		{
		    put("zimbraPrefCalendarInitialView", "day");
		}};


	}
	
	@Test(description = "Delete an appointment using Delete toolbar button",
			groups = { "smoke" })
	public void DeleteAppointment_01() throws HarnessException {
		
		
		//-- Data Setup
		
		
		
		// Creating objects for appointment data
		String tz = ZTimeZone.TimeZoneEST.getID();
		String apptSubject = "subject" + ZimbraSeleniumProperties.getUniqueString();
		String apptBody = "body" + ZimbraSeleniumProperties.getUniqueString();
		
		// Absolute dates in UTC zone
		Calendar now = Calendar.getInstance();
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
        app.zGetActiveAccount().soapSend(
    			"<CreateAppointmentRequest xmlns='urn:zimbraMail'>"
    		+		"<m>"
    		+			"<inv method='REQUEST' type='event' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"
    		+				"<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>"
    		+				"<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>"
    		+				"<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" 
    		+			"</inv>" 
    		+			"<mp content-type='text/plain'>" 
    		+				"<content>" + apptBody + "</content>" 
    		+			"</mp>"
    		+			"<su>" + apptSubject + "</su>" 
    		+		"</m>" 
    		+	"</CreateAppointmentRequest>");

        
        //-- GUI Actions
        
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);
        
        DialogConfirmDeleteAppointment dlgConfirm = (DialogConfirmDeleteAppointment)app.zPageCalendar.zToolbarPressButton(Button.B_DELETE);
		dlgConfirm.zClickButton(Button.B_YES);
		dlgConfirm.zWaitForClose();
		
		
		//-- Verification

		// Verify the appointment is deleted
		app.zGetActiveAccount().soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-7).toMillis() +"' calExpandInstEnd='"+ startUTC.addDays(7).toMillis() +"'>"
			+		"<query>subject:("+ apptSubject +")</query>"
			+	"</SearchRequest>");

		Element[] nodes = app.zGetActiveAccount().soapSelectNodes("//mail:appt");
		ZAssert.assertEquals(nodes.length, 0, "Verify appointment is deleted");

	}
	
}
