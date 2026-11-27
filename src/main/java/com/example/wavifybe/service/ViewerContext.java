package com.example.wavifybe.service;

import com.example.wavifybe.entity.Song;
import com.example.wavifybe.entity.Visibility;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


record ViewerContext(boolean authenticated, String email, boolean admin) {

    static ViewerContext fromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ViewerContext(false, null, false);
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new ViewerContext(true, authentication.getName(), isAdmin);
    }

    boolean canView(Song song) {
        if (song.getVisibility() == Visibility.PUBLIC) {
            return true;
        }
        return authenticated && (admin || song.getUploader().getEmail().equals(email));
    }
}
