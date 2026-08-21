package io.github.raeperd.realworld.application.user;

import io.github.raeperd.realworld.domain.user.Profile;
import lombok.Value;

@Value
public class ProfileModel {

    ProfileModelNested profile;

    public static ProfileModel fromProfile(Profile profile) {
        return new ProfileModel(ProfileModelNested.fromProfile(profile));
    }

    @Value
    public static class ProfileModelNested {
        String username;
        String bio;
        String image;
        boolean following;

        public static ProfileModelNested fromProfile(Profile profile) {
            return new ProfileModelNested(String.valueOf(profile.getUserName()),
                    profile.getBio(),
                    String.valueOf(profile.getImage()),
                    profile.isFollowing());
        }
    }
}
