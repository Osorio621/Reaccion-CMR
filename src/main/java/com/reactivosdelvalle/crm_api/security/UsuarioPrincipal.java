package com.reactivosdelvalle.crm_api.security;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {

    @Getter
    private final Long id;
    
    private final String email;
    
    private final String password;
    
    @Getter
    private final String nombre;
    
    @Getter
    private final String apellido;
    
    @Getter
    private final RolUsuario rol;
    
    private final Collection<? extends GrantedAuthority> authorities;

    public static UsuarioPrincipal create(Usuario usuario) {
        // Prefixed with ROLE_ as required by Spring Security convention for hasRole()
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());
        
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // We can base this on whether the user is active, but we already check active true when loading
    }
}
