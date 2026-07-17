package com.forecastpro.config;

import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForecastExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger LOG = Logger.getLogger(ForecastExceptionHandler.class.getName());

    private final ExceptionHandler wrapped;

    public ForecastExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }

    @Override
    public void handle() throws FacesException {
        Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();
        while (events.hasNext()) {
            ExceptionQueuedEvent ev = events.next();
            ExceptionQueuedEventContext ctx = ev.getContext();
            Throwable t = ctx.getException();
            Throwable cause = unwrap(t);
            events.remove();
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) {
                continue;
            }
            if (cause instanceof BusinessException || cause instanceof AccessDeniedException) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, cause.getMessage(), null));
                continue;
            }
            LOG.log(Level.SEVERE, cause.getMessage(), cause);
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "An unexpected error occurred. See server logs for details.", null));
        }
        getWrapped().handle();
    }

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            if (c instanceof BusinessException || c instanceof AccessDeniedException) {
                return c;
            }
            c = c.getCause();
        }
        return c;
    }
}
