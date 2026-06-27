package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.mapper.UserMapper;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.util.AuthUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository repository;

    private final UserMapper mapper;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByLogin(username)
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return mapper.toDTOs(repository.findAll());
    }

    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLoginState(User user) {
        repository.save(user);
    }

    @Transactional
    public void updateLastLogin(final Long id) {
        repository.updateLastLogin(id);
    }

    @Transactional
    public UserDTO create(final CreateUserDTO dto) {
        this.validate(dto);
        final User user = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(user));
    }

    public void validate(final CreateUserDTO dto) {
        if (repository.existsByLoginIgnoreCase(dto.login())) {
            throw new CoreException(EApplicationError.LOGIN_ALREADY_EXISTS);
        }

        if (repository.existsByEmailIgnoreCase(dto.email())) {
            throw new CoreException(EApplicationError.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void deleteById(final Long id) {
        if (!repository.existsById(id)) {
            throw new CoreException(EApplicationError.USER_NOT_FOUND);
        }
        repository.deleteById(id);
    }

    @Transactional
    public UserDTO update(final Long id, final UpdateUserDTO dto) {
        User current = repository.findById(id)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        if (!Strings.CI.equals(current.getLogin(), dto.login()) &&
            repository.existsByLoginIgnoreCase(dto.login())) {
            throw new CoreException(EApplicationError.LOGIN_ALREADY_EXISTS);
        }

        if (!Strings.CI.equals(current.getEmail(), dto.email()) &&
            repository.existsByEmailIgnoreCase(dto.email())) {
            throw new CoreException(EApplicationError.EMAIL_ALREADY_EXISTS);
        }

        User toUpdate = mapper.fullUpdate(dto, current);

        return mapper.toDTO(repository.save(toUpdate));
    }

    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        return repository.findByLogin(login)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserDTO getLoggedUser() {
        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();

        User entity = repository.findById(loggedUser.getId())
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        return mapper.toDTO(entity);
    }
}
