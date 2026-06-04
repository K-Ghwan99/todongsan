package com.todongsan.memberpointservice.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfo {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public String getKakaoId() {
        return String.valueOf(id);
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.profile == null) return null;
        return kakaoAccount.profile.nickname;
    }

    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.email : null;
    }

    public String getBirthyear() {
        return kakaoAccount != null ? kakaoAccount.birthyear : null;
    }

    public String getGender() {
        return kakaoAccount != null ? kakaoAccount.gender : null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {

        @JsonProperty("profile")
        private Profile profile;

        @JsonProperty("email")
        private String email;

        @JsonProperty("birthyear")
        private String birthyear;

        @JsonProperty("gender")
        private String gender;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {

        @JsonProperty("nickname")
        private String nickname;
    }

}
