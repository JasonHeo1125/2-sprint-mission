package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface MessageService {

      List<BinaryContentCreateRequest> binaryContentCreateRequests);




  void delete(UUID messageId);
}
