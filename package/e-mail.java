// This code is related to an email plugin system using IMAP and SMTP protocols.

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmailPlugin {

    private static final String SLUG = "email";
    private String yourPassword = "your_password"; // 请替换为您的实际密码
    private String emailAddress = "example@qq.com"; // 请替换为您的实际邮箱地址
    private String email;
    private String password;
    private String imapServer;
    private String imapPort;
    private String smtpServer;
    private String smtpPort;
    private int currentAccountIndex;
    private Config config;

    public EmailPlugin() {
        loadConfig();
        currentAccountIndex = 0;
        updateAccountInfo();
    }

    private void loadConfig() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            config = objectMapper.readValue(new File("./email_config.json"), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateAccountInfo() {
        Account account = config.getAccounts().get(currentAccountIndex);
        email = account.getEmail();
        password = account.getPassword();
        imapServer = account.getImapServer();
        imapPort = account.getImapPort();
        smtpServer = account.getSmtpServer();
        smtpPort = account.getSmtpPort();
    }

    public void say(String message) {
        System.out.println(message);
    }

    public void switchAccount(int index) {
        if (index >= 0 && index < config.getAccounts().size()) {
            currentAccountIndex = index;
            updateAccountInfo();
            System.out.println("已切换到账户: " + email);
        } else {
            System.out.println("无效的账户索引");
        }
    }

    public String getSender(Message msg) throws MessagingException, IOException {
        String fromStr = msg.getFrom()[0].toString();
        String[] parts = fromStr.split(" ");
        String sender = "";
        if (parts.length == 2) {
            sender = MimeUtility.decodeText(parts[0]);
        } else if (parts.length > 2) {
            sender = MimeUtility.decodeText(fromStr.substring(0, fromStr.indexOf("<")).trim());
        } else {
            sender = fromStr;
        }
        return sender;
    }

    public boolean isSelfEmail(Message msg) throws MessagingException {
        String fromStr = msg.getFrom()[0].toString();
        String addr = fromStr.substring(fromStr.indexOf("<") + 1, fromStr.indexOf(">")).trim();
        String address = config.getEmail().get(SLUG).getAddress().trim();
        return addr.equals(address);
    }

    public String getSubject(Message msg) throws MessagingException, IOException {
        String subject = msg.getSubject();
        return subject != null ? subject.trim() : "";
    }

    public boolean isNewEmail(Message msg) throws MessagingException {
        Date date = msg.getReceivedDate();
        Date current = new Date();
        return (current.getTime() - date.getTime()) < 24 * 60 * 60 * 1000; // within 24 hours
    }

    public Date getDate(Message email) throws MessagingException {
        return email.getReceivedDate();
    }

    public Date getMostRecentDate(List<Message> emails) throws MessagingException {
        List<Date> dates = new ArrayList<>();
        for (Message email : emails) {
            dates.add(getDate(email));
        }
        dates.sort((d1, d2) -> d2.compareTo(d1));
        return dates.isEmpty() ? null : dates.get(0);
    }

    public void saveAttachments(Message msg, String downloadFolder) throws IOException, MessagingException {
        if (!new File(downloadFolder).exists()) {
            new File(downloadFolder).mkdirs();
        }
        if (msg.isMimeType("multipart")) {
            Multipart multipart = (Multipart) msg.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();
                if (disposition != null && Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                    String filename = bodyPart.getFileName();
                    if (filename != null) {
                        String filepath = downloadFolder + File.separator + filename;
                        try (FileOutputStream fos = new FileOutputStream(filepath)) {
                            fos.write(bodyPart.getInputStream().readAllBytes());
                        }
                        System.out.println("附件已保存：" + filepath);
                    }
                }
            }
        }
    }

    public List<Message> fetchUnreadEmails(Date since, boolean markRead, Integer limit) {
        List<Message> msgs = new ArrayList<>();
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore("imaps");
            store.connect(imapServer, email, password);
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for (Message msg : messages) {
                if (since == null || getDate(msg).after(since)) {
                    msgs.add(msg);
                }
            }
            folder.close(false);
            store.close();
        } catch (Exception e) {
            System.out.println("抱歉，您的邮箱账户验证失败了，请检查下配置");
        }
        return msgs;
    }

    public void sendEmail(String subject, String message, String receiver) {
        try {
            MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
            msg.setSubject(subject);
            msg.setFrom(new InternetAddress(emailAddress));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
            msg.setText(message, StandardCharsets.UTF_8.name());

            Transport transport = Session.getInstance(new Properties()).getTransport("smtp");
            transport.connect(smtpServer, Integer.parseInt(smtpPort), emailAddress, yourPassword);
            transport.sendMessage(msg, msg.getAllRecipients());
            System.out.println("邮件发送成功！");
        } catch (Exception e) {
            System.out.println("邮件发送失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        EmailPlugin emailPlugin = new EmailPlugin();
        emailPlugin.switchAccount(1);

        // 要发送的邮件内容
        String emailSubject = System.console().readLine("主题：");
        String emailMessage = System.console().readLine("内容：");
        String recipientEmail = System.console().readLine("邮箱地址：");

        // 调用发送邮件函数
        emailPlugin.sendEmail(emailSubject, emailMessage, recipientEmail);
    }

    // Assuming Config and Account classes are implemented with appropriate fields and methods
}
