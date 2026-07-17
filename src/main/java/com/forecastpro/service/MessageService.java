package com.forecastpro.service;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.Message;
import com.forecastpro.entity.MessageStatus;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.MessageRepository;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

@Stateless
public class MessageService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private SecurityService securityService;

    /** Admin sees all; others see sent or received messages. */
    public List<Message> listMessages(UserRole caller, Long currentUserId) {
        securityService.requireAuthenticated(caller);
        if (caller == UserRole.ADMIN) {
            return messageRepository.findAllOrderByCreatedDesc();
        }
        return messageRepository.findForUser(currentUserId);
    }

    public void sendMessage(UserRole caller, Long senderId, Long receiverId, String subject, String body) {
        securityService.requireAuthenticated(caller);
        ValidationUtil.requireNonBlank(subject, "Subject");
        ValidationUtil.requireNonBlank(body, "Message");
        User sender = userRepository.findById(senderId).orElseThrow(() -> new BusinessException("User not found."));
        Message m = new Message();
        m.setSender(sender);
        if (receiverId != null) {
            m.setRecipient(userRepository.findById(receiverId)
                    .orElseThrow(() -> new BusinessException("Recipient not found.")));
        }
        m.setSubject(subject.trim());
        m.setBody(body.trim());
        m.setStatus(MessageStatus.OPEN);
        messageRepository.save(m);
    }

    /** Employee or vendor message to admin (receiver = first admin). */
    public void sendToAdmin(UserRole caller, Long senderId, String subject, String body) {
        if (caller != UserRole.EMPLOYEE && caller != UserRole.VENDOR) {
            throw new AccessDeniedException("Only employees or vendors can message admin.");
        }
        User admin = userRepository.findAdminAndSalesManagers().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .findFirst()
                .orElseThrow(() -> new BusinessException("No admin user configured."));
        sendMessage(caller, senderId, admin.getId(), subject, body);
    }

    /** Employee complaint to admin (receiver = first admin). */
    public void sendComplaint(UserRole caller, Long senderId, String subject, String body) {
        securityService.requireEmployee(caller);
        sendToAdmin(caller, senderId, subject, body);
    }

    public void closeMessage(UserRole caller, Long userId, Long messageId) {
        securityService.requireAuthenticated(caller);
        Message m = messageRepository.findById(messageId);
        if (m == null) {
            throw new BusinessException("Message not found.");
        }
        if (caller != UserRole.ADMIN
                && (m.getSender() == null || !userId.equals(m.getSender().getId()))
                && (m.getRecipient() == null || !userId.equals(m.getRecipient().getId()))) {
            throw new BusinessException("Not allowed to update this message.");
        }
        m.setStatus(MessageStatus.CLOSED);
        messageRepository.save(m);
    }
}
