package com.forecastpro.controller;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Message;
import com.forecastpro.entity.MessageStatus;
import com.forecastpro.entity.User;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.service.MessageService;
import com.forecastpro.util.DisplayFormats;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Named("messageBean")
@ViewScoped
public class MessageBean implements Serializable {

    @Inject
    private UserRepository userRepository;

    @Inject
    private MessageService messageService;

    @Inject
    private UserSessionBean userSession;

    private List<Message> messages = new ArrayList<>();
    private List<User> recipients = new ArrayList<>();

    private String composeSubject;
    private String composeBody;
    private Long composeRecipientId;

    @PostConstruct
    public void init() {
        refresh();
        if (userSession.isAdminOrManager()) {
            recipients = userRepository.findAllOrdered();
        }
    }

    public void refresh() {
        messages = new ArrayList<>(messageService.listMessages(
                userSession.getRole(), userSession.getCurrentUser().getId()));
    }

    public void sendComplaint() {
        sendToAdmin();
    }

    public void sendToAdmin() {
        try {
            messageService.sendToAdmin(userSession.getRole(), userSession.getCurrentUser().getId(),
                    composeSubject, composeBody);
            composeSubject = null;
            composeBody = null;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Message sent.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void sendDirect() {
        try {
            messageService.sendMessage(userSession.getRole(), userSession.getCurrentUser().getId(),
                    composeRecipientId, composeSubject, composeBody);
            composeSubject = null;
            composeBody = null;
            composeRecipientId = null;
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Message sent.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void closeMessage(Message msg) {
        if (msg == null) {
            return;
        }
        try {
            messageService.closeMessage(userSession.getRole(), userSession.getCurrentUser().getId(), msg.getId());
            refresh();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Message closed.", null));
        } catch (BusinessException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public String formatInstant(Instant i) {
        return DisplayFormats.formatInstantDateTime(i, ZoneId.systemDefault());
    }

    public String recipientName(Message m) {
        if (m == null || m.getRecipient() == null) {
            return "—";
        }
        return m.getRecipient().getUsername();
    }

    public boolean isOpen(Message m) {
        return m != null && m.getStatus() == MessageStatus.OPEN;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<User> getRecipients() {
        return recipients;
    }

    public String getComposeSubject() {
        return composeSubject;
    }

    public void setComposeSubject(String composeSubject) {
        this.composeSubject = composeSubject;
    }

    public String getComposeBody() {
        return composeBody;
    }

    public void setComposeBody(String composeBody) {
        this.composeBody = composeBody;
    }

    public Long getComposeRecipientId() {
        return composeRecipientId;
    }

    public void setComposeRecipientId(Long composeRecipientId) {
        this.composeRecipientId = composeRecipientId;
    }
}
