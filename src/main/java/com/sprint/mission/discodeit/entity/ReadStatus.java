package com.sprint.mission.discodeit.entity;

import java.time.Instant;

@Getter

  private Instant lastReadAt;

    this.lastReadAt = lastReadAt;
  }

  public void update(Instant newLastReadAt) {
    if (newLastReadAt != null && !newLastReadAt.equals(this.lastReadAt)) {
      this.lastReadAt = newLastReadAt;
    }
    }
  }