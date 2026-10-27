package com.qa.mail;

import com.qa.config.ConfigReader;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.RecipientStringTerm;

import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MailReader {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    private MailReader() {
    }

    /** Returns the 6-digit code from the mail addressed to {@code toEmail} (a fresh address, so any mail is the one). */
    public static String waitForCode(String toEmail) {
        Matcher m = CODE.matcher(waitForMailText(toEmail, 0));
        if (!m.find()) {
            throw new IllegalStateException("No 6-digit code in the mail for " + toEmail);
        }
        return m.group(1);
    }

    /**
     * Polls the inbox until a mail addressed to {@code toEmail} that arrived at or after
     * {@code sinceMillis} shows up, and returns its text. The time filter matters for addresses
     * that get mail on every run, where older mails would otherwise match.
     */
    public static String waitForMailText(String toEmail, long sinceMillis) {
        long deadline = System.currentTimeMillis() + 90_000;
        try (Store store = Session.getInstance(new Properties()).getStore("imaps")) {
            store.connect("imap.gmail.com", ConfigReader.get("mail.user"), ConfigReader.get("mail.appPassword"));
            while (System.currentTimeMillis() < deadline) {
                // reopen each poll so the folder sees mail that arrived since the last one
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                try {
                    Message[] found = inbox.search(new RecipientStringTerm(Message.RecipientType.TO, toEmail));
                    for (int i = found.length - 1; i >= 0; i--) {
                        Date received = found[i].getReceivedDate();
                        if (received != null && received.getTime() >= sinceMillis) {
                            return text(found[i]);
                        }
                    }
                } finally {
                    inbox.close(false);
                }
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the mail for " + toEmail, e);
        }
        throw new IllegalStateException("No mail for " + toEmail + " within 90s");
    }

    private static String text(Part part) throws Exception {
        if (part.isMimeType("text/*")) {
            return part.getContent().toString();
        }
        if (part.getContent() instanceof Multipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                sb.append(text(mp.getBodyPart(i))).append('\n');
            }
            return sb.toString();
        }
        return "";
    }
}
