package com.sprint.mission.discodeit.entity;

import lombok.Getter;

@Getter

  private String username;
  private String email;
  private String password;

    this.username = username;
    this.email = email;
    this.password = password;
  }

    if (newUsername != null && !newUsername.equals(this.username)) {
      this.username = newUsername;
    }
    if (newEmail != null && !newEmail.equals(this.email)) {
      this.email = newEmail;
    }
    if (newPassword != null && !newPassword.equals(this.password)) {
      this.password = newPassword;
    }
    }
  }
}
