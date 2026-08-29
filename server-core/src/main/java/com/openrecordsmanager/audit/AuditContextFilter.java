package com.openrecordsmanager.audit;

import com.openrecordsmanager.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String comment = request.getHeader(AuditContext.COMMENT_HEADER);
        Actor actor = resolveActor();

        AuditContext.begin(actor.id(), actor.username(), comment, true);
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }

    private static Actor resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return new Actor(user.getId(), user.getUsername());
        }
        return new Actor(null, null);
    }

    private record Actor(@Nullable UUID id, @Nullable String username) {
    }
}
