package org.apache.commons.mail;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmailTest {
    private Email email;

    // Using more realistic example emails
    private static final String[] TEST_EMAILS = {"john.doe@example.com", "jane.smith@testmail.org", "longemailaddress@verylongdomain.edu"};

    @Before 
    public void setUpEmailTest() throws Exception {
        email = new EmailConcrete(); // Setting up a new email instance before each test
    }

    @After
    public void tearDownEmailTest() throws Exception {
        email = null; // Reset the email object after each test
    }

    @Test 
    public void testAddBcc() throws Exception {
        email.addBcc(TEST_EMAILS);
        assertEquals(3, email.getBccAddresses().size()); // Checking if all BCC addresses were added
    }

    @Test
    public void testAddCc() throws Exception {
        email.addCc(TEST_EMAILS[0]);
        assertEquals(1, email.getCcAddresses().size()); // Confirming CC address was added
    }

    @Test
    public void testAddHeader() {
        try {
            email.addHeader("X-Example-Header", "HeaderValue"); // Adding a header to check if it works
        } catch (Exception e) {
            fail("Should not throw an exception");
        }
        try {
            email.addHeader("", "Invalid"); // Attempting to add a header with an empty name, should fail
            fail("Expected IllegalArgumentException for an empty header name");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testAddReplyTo() throws Exception {
        email.addReplyTo(TEST_EMAILS[1], "Support Team");
        assertEquals(1, email.getReplyToAddresses().size()); // Ensuring reply-to email was added
    }

    @Test
    public void testBuildMimeMessage() {
        try {
            // Setting up email details
            email.setHostName("random@email.com");
            email.setFrom("noreply@mail.com");
            email.addTo("user123@gmail.com");
            email.setSubject("Meeting Reminder");
            email.setMsg("Don't forget about the meeting tomorrow at 10 AM.");

            email.buildMimeMessage();
            MimeMessage mimeMessage = email.getMimeMessage();

            assertNotNull("MimeMessage should be initialized", mimeMessage);
            assertEquals("Meeting Reminder", mimeMessage.getSubject());
            assertEquals("noreply@mail.com", email.getFromAddress().getAddress());
            assertNotNull("Recipient list should not be empty", mimeMessage.getAllRecipients());

           // Calling buildMimeMessage() twice should throw IllegalStateException
            try {
                email.buildMimeMessage();
                fail("Expected IllegalStateException on second build");
            } catch (IllegalStateException e) {
                assertEquals("The MimeMessage is already built.", e.getMessage());
            }

           // Ensure an exception is thrown when no From address is set
            Email emailNoFrom = new EmailConcrete();
            emailNoFrom.setHostName("random@email.com");
            emailNoFrom.addTo("jasonderulo@email.com");
            emailNoFrom.setSubject("Project Update");
            emailNoFrom.setMsg("Here is the latest update on the project.");

            try {
                emailNoFrom.buildMimeMessage();
                fail("Expected EmailException for missing From address.");
            } catch (EmailException e) {
                assertEquals("From address required", e.getMessage());
            }

            // Ensure an exception is thrown when no recipients are set
            Email emailNoRecipients = new EmailConcrete();
            emailNoRecipients.setHostName("random@mail.com");
            emailNoRecipients.setFrom("admin@support.com");
            emailNoRecipients.setSubject("System Maintenance");
            emailNoRecipients.setMsg("Scheduled maintenance will occur at midnight.");

            try {
                emailNoRecipients.buildMimeMessage();
                fail("Expected EmailException for missing recipients.");
            } catch (EmailException e) {
                assertEquals("At least one receiver address required", e.getMessage());
            }

          // Ensure CC and BCC and Reply-To addresses are added
            Email emailWithCCBCC = new EmailConcrete();
            emailWithCCBCC.setHostName("random@email.com");
            emailWithCCBCC.setFrom("info@company.com");
            emailWithCCBCC.addTo("client@random.org");
            emailWithCCBCC.addCc("manager@business.net");
            emailWithCCBCC.addBcc("hiddenuser@private.com");
            emailWithCCBCC.addReplyTo("support@company.com", "Customer Support");
            emailWithCCBCC.setSubject("Invoice Attached");
            emailWithCCBCC.setMsg("Please find your invoice attached.");

            emailWithCCBCC.buildMimeMessage();
            assertEquals(1, emailWithCCBCC.getMimeMessage().getRecipients(javax.mail.Message.RecipientType.CC).length);
            assertEquals(1, emailWithCCBCC.getMimeMessage().getRecipients(javax.mail.Message.RecipientType.BCC).length);
            assertEquals(1, emailWithCCBCC.getMimeMessage().getReplyTo().length);

           // Ensure headers are properly added
            Email emailWithHeaders = new EmailConcrete();
            emailWithHeaders.setHostName("random@email.com");
            emailWithHeaders.setFrom("selenagomez@mail.com");
            emailWithHeaders.addTo("subscriber@mail.org");
            emailWithHeaders.setSubject("Weekly Newsletter");
            emailWithHeaders.setMsg("Here is your weekly newsletter.");
            emailWithHeaders.addHeader("X-Newsletter-ID", "12345");

            emailWithHeaders.buildMimeMessage();
            assertEquals("12345", emailWithHeaders.getMimeMessage().getHeader("X-Newsletter-ID")[0]);

         // Verify that a sent date is automatically set even if none was provided
            Email emailWithoutSentDate = new EmailConcrete();
            emailWithoutSentDate.setHostName("random@email.com");
            emailWithoutSentDate.setFrom("ccffss@random.com");
            emailWithoutSentDate.addTo("college@edu.com");
            emailWithoutSentDate.setSubject("Event Reminder");
            emailWithoutSentDate.setMsg("Your event starts in 30 minutes.");

            emailWithoutSentDate.buildMimeMessage();
            assertNotNull("Sent date should be auto-set", emailWithoutSentDate.getMimeMessage().getSentDate());

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetHostName() {
        email.setHostName("smtp.provider.com");
        assertEquals("smtp.provider.com", email.getHostName()); // Checking hostname retrieval
        email.setMailSession(Session.getInstance(new Properties()));
        assertNull(email.getHostName()); // Should return null if no hostname is set
    }

    @Test
    public void testGetMailSession() throws Exception {
        try {
            email.getMailSession(); // Try getting session without hostname
            fail("Expected EmailException");
        } catch (EmailException e) {
            assertNotNull(e.getMessage());
        }
        email.setHostName("smtp.provider.com");
        Session session = email.getMailSession();
        assertNotNull(session); // Ensure session creation
        assertEquals("smtp.provider.com", session.getProperty(Email.MAIL_HOST));
    }

    @Test
    public void testGetSentDate() {
        Date date = new Date();
        email.setSentDate(date);
        assertEquals(date, email.getSentDate()); // Making sure the date is set correctly
    }

    @Test
    public void testGetSocketConnectionTimeout() {
        assertEquals(60000, email.getSocketConnectionTimeout()); // Verifying default socket timeout
    }

    @Test
    public void testSetFrom() throws Exception {
        email.setFrom(TEST_EMAILS[0]);
        InternetAddress fromAddress = email.getFromAddress();
        assertNotNull(fromAddress);
        assertEquals(TEST_EMAILS[0], fromAddress.getAddress()); // Ensuring 'From' email is set correctly
    }
}
