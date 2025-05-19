package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.ReadStatus;
import java.util.List;
import java.util.UUID;



  List<ReadStatus> findAllByUserId(UUID userId);



  void deleteAllByChannelId(UUID channelId);
}
