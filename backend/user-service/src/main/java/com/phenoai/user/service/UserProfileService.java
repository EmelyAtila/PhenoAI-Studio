package com.phenoai.user.service;

import com.phenoai.user.domain.UserProfile;
import com.phenoai.user.dto.UpdateProfileRequest;
import com.phenoai.user.dto.UserProfileResponse;
import com.phenoai.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileResponse getMyProfile(String email) {
        UserProfile profile = repository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));
        return UserProfileResponse.from(profile);
    }

    public UserProfileResponse upsertMyProfile(String email, UpdateProfileRequest request) {
        UserProfile profile = repository.findByEmail(email)
            .orElse(UserProfile.builder().email(email).build());

        profile.setName(request.name());
        profile.setBio(request.bio());
        profile.setOrganization(request.organization());

        return UserProfileResponse.from(repository.save(profile));
    }

    public List<UserProfileResponse> listAll() {
        return repository.findAll()
            .stream()
            .map(UserProfileResponse::from)
            .toList();
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        repository.deleteById(id);
    }
}