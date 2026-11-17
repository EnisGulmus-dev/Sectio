package com.enis.sectio.user;

import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class Profile {
    @Id @Column(name = "user_id") private Long userId;
    @Column(name = "display_name", length = 100) private String displayName;
    @Column(length = 500) private String biography;
    @Column(name = "time_zone", length = 64) private String timeZone;
    @Column(name = "photo_key", length = 80) private String photoKey;
    @Column(name = "avatar_id", length = 80) private String avatarId;

    protected Profile() {}
    public Profile(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getBiography() { return biography; }
    public String getTimeZone() { return timeZone; }
    public String getPhotoKey() { return photoKey; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public void setPhotoKey(String photoKey) { this.photoKey = photoKey; }
    public void update(String displayName, String biography, String timeZone) {
        this.displayName = clean(displayName); this.biography = clean(biography); this.timeZone = clean(timeZone);
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
