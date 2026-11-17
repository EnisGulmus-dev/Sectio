package com.enis.sectio.user;

import com.enis.sectio.security.SessionUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.DateTimeException;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileRepository profiles;
    private final UserRepository users;
    private final ProfilePhotoStore photos;
    private static final java.util.Set<String> AVATARS = java.util.Set.of("erkek_01_sakalsiz", "erkek_02_hafif_sakal", "erkek_03_biyik_top_sakal", "erkek_04_kel_gur_sakal", "erkek_05_kir_sac_sakal", "kadin_01_uzun_siyah_sac", "kadin_02_dalgali_kestane", "kadin_03_kisa_kivircik", "kadin_04_sarisin_topuz", "kadin_05_kisa_kizil");
    public ProfileController(ProfileRepository profiles, UserRepository users, ProfilePhotoStore photos) { this.profiles = profiles; this.users = users; this.photos = photos; }

    @GetMapping @Transactional(readOnly = true)
    public ProfileResponse get(Authentication authentication) {
        User user = user(authentication);
        return response(user, profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId())));
    }

    @PatchMapping @Transactional
    public ProfileResponse update(Authentication authentication, @Valid @RequestBody ProfileInput input) {
        User user = userForUpdate(authentication);
        if (input.timeZone() != null && !input.timeZone().isBlank()) {
            try { ZoneId.of(input.timeZone().trim()); }
            catch (DateTimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz saat dilimi"); }
        }
        Profile profile = profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId()));
        profile.update(input.displayName(), input.biography(), input.timeZone());
        return response(user, profiles.save(profile));
    }

    @PutMapping("/avatar") @Transactional
    public ProfileResponse avatar(Authentication authentication, @Valid @RequestBody AvatarInput input) {
        User user = userForUpdate(authentication);
        if (input.avatarId() != null && !AVATARS.contains(input.avatarId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar bulunamadı.");
        Profile profile = profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId()));
        photos.deleteAfterCommit(profile.getPhotoKey()); profile.setPhotoKey(null); profile.setAvatarId(input.avatarId());
        return response(user, profiles.save(profile));
    }

    @PostMapping(value = "/photo", consumes = "multipart/form-data") @Transactional
    public ProfileResponse upload(Authentication authentication, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        User user = userForUpdate(authentication);
        Profile profile = profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId()));
        String key = photos.save(file); photos.deleteAfterCommit(profile.getPhotoKey());
        profile.setPhotoKey(key); profile.setAvatarId(null);
        return response(user, profiles.save(profile));
    }

    @DeleteMapping("/photo") @Transactional
    public ProfileResponse removePhoto(Authentication authentication) {
        User user = userForUpdate(authentication);
        Profile profile = profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId()));
        photos.deleteAfterCommit(profile.getPhotoKey()); profile.setPhotoKey(null);
        return response(user, profiles.save(profile));
    }

    @PostMapping(value = "/commit", consumes = "multipart/form-data") @Transactional
    public ProfileResponse commit(Authentication authentication, @Valid @RequestPart("data") ProfileCommitInput input,
                                  @RequestPart(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) {
        User user = userForUpdate(authentication);
        if (input.timeZone() != null && !input.timeZone().isBlank()) {
            try { ZoneId.of(input.timeZone().trim()); }
            catch (DateTimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz saat dilimi"); }
        }
        String mode = input.imageMode();
        if (!java.util.Set.of("KEEP", "INITIALS", "AVATAR", "PHOTO").contains(mode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz profil resmi seçimi");
        if ("AVATAR".equals(mode) && (input.avatarId() == null || !AVATARS.contains(input.avatarId()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar bulunamadı.");
        if ("PHOTO".equals(mode) && file == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotoğraf seçin.");
        if (!"PHOTO".equals(mode) && file != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beklenmeyen fotoğraf.");
        Profile profile = profiles.findById(user.getId()).orElseGet(() -> new Profile(user.getId()));
        profile.update(input.displayName(), input.biography(), input.timeZone());
        if (!"KEEP".equals(mode)) {
            String oldKey = profile.getPhotoKey();
            String newKey = "PHOTO".equals(mode) ? photos.save(file) : null;
            profile.setPhotoKey(newKey); profile.setAvatarId("AVATAR".equals(mode) ? input.avatarId() : null);
            photos.deleteAfterCommit(oldKey);
        }
        return response(user, profiles.save(profile));
    }

    @GetMapping(value = "/{userId}/photo", produces = "image/png") @Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable Long userId) {
        user(authentication);
        User target = users.findById(userId).filter(u -> u.isActive() && u.getDeletedAt() == null).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String key = profiles.findById(target.getId()).map(Profile::getPhotoKey).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(photos.read(key));
    }

    private User user(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof SessionUserDetails principal)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findById(principal.getUserId()).filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private User userForUpdate(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof SessionUserDetails principal)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByIdForUpdate(principal.getUserId()).filter(u -> u.isActive() && u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private static ProfileResponse response(User user, Profile profile) {
        return new ProfileResponse(user.getId(), user.getName(), user.getJobTitle(), profile.getDisplayName(), profile.getBiography(), profile.getTimeZone(), profile.getAvatarId(), profile.getPhotoKey() != null);
    }
    public record ProfileInput(@Size(max = 100) String displayName, @Size(max = 500) String biography, @Size(max = 64) String timeZone) {}
    public record AvatarInput(@Size(max = 80) String avatarId) {}
    public record ProfileCommitInput(@Size(max=100) String displayName, @Size(max=500) String biography,
                                     @Size(max=64) String timeZone, @jakarta.validation.constraints.NotNull String imageMode,
                                     @Size(max=80) String avatarId) {}
    public record ProfileResponse(Long userId, String name, JobTitle jobTitle, String displayName, String biography, String timeZone, String avatarId, boolean hasPhoto) {}
}
