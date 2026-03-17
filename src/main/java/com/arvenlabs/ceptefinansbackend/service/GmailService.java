package com.arvenlabs.ceptefinansbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private static final String APPLICATION_NAME = "CepteFinans";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public List<String> fetchRecentInvoices(String accessToken, int days) {
        List<String> emailBodies = new ArrayList<>();
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            // Not: GoogleCredential kullanılarak Access Token sisteme tanıtılır.
            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

            Gmail service = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Sadece belirli kelimeleri içeren ve son N gündeki mailleri çek
            String query = String.format("(subject:fatura OR subject:harcama OR subject:ödeme OR subject:receipt) newer_than:%dd", days);
            log.info("Gmail dökümü yapılıyor. Query: {}", query);
            
            ListMessagesResponse response = service.users().messages().list("me")
                    .setQ(query)
                    .execute();
            
            List<Message> messages = response.getMessages();
            if (messages != null) {
                for (Message msg : messages) {
                    Message m = service.users().messages().get("me", msg.getId()).setFormat("full").execute();
                    String bodyText = getEmailBodyAsText(m);
                    if (bodyText != null && !bodyText.trim().isEmpty()) {
                        emailBodies.add(bodyText);
                    }
                }
            }
            log.info("Toplam '{}' adet potansiyel fatura e-postası bulundu.", emailBodies.size());
        } catch (Exception e) {
            log.error("Gmail mailleri çekilirken hata oluştu: ", e);
            throw new RuntimeException("Gmail erişim hatası: Lütfen Google Access Token'ın geçerli olduğundan emin olun. Detay: " + e.getMessage());
        }
        return emailBodies;
    }

    private String getEmailBodyAsText(Message message) {
        try {
            if (message.getPayload().getParts() != null) {
                // Önce plain text araması
                for (MessagePart part : message.getPayload().getParts()) {
                    if (part.getMimeType().equals("text/plain") && part.getBody().getData() != null) {
                        return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
                    }
                }
                // Bulunamazsa html araması
                for (MessagePart part : message.getPayload().getParts()) {
                    if (part.getMimeType().equals("text/html") && part.getBody().getData() != null) {
                        return new String(Base64.getUrlDecoder().decode(part.getBody().getData())); 
                    }
                }
                // İç içe geçmiş parmak izleri (multipart/alternative vs) olabilir
                for (MessagePart part : message.getPayload().getParts()) {
                    if (part.getParts() != null) {
                        for (MessagePart subPart : part.getParts()) {
                            if (subPart.getMimeType().equals("text/plain") && subPart.getBody().getData() != null) {
                                return new String(Base64.getUrlDecoder().decode(subPart.getBody().getData()));
                            }
                        }
                    }
                }
            } else if (message.getPayload().getBody() != null && message.getPayload().getBody().getData() != null) {
                return new String(Base64.getUrlDecoder().decode(message.getPayload().getBody().getData()));
            }
        } catch (Exception e) {
            log.warn("Mesaj içeriği okunamadı: {}", message.getId());
        }
        return null;
    }
}
