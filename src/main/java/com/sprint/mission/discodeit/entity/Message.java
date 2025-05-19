package com.sprint.mission.discodeit.entity;

import java.util.List;

@Getter

  private String content;

    this.content = content;
  }

  public void update(String newContent) {
    if (newContent != null && !newContent.equals(this.content)) {
      this.content = newContent;
    }
  }
}
