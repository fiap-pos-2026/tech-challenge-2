package br.com.fiap.pos.tech_challenge.core.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

@Component
@RequiredArgsConstructor
public class Translator {

    private final MessageSource messageSource;

    private final LocaleResolver localeResolver;

    public String translate(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    public String translate(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public String translateFromRequest(String key, HttpServletRequest request) {
        return messageSource.getMessage(key, null, localeResolver.resolveLocale(request));
    }

    public String translateFromRequest(String key, HttpServletRequest request, Object... args) {
        return messageSource.getMessage(key, args, localeResolver.resolveLocale(request));
    }
}
