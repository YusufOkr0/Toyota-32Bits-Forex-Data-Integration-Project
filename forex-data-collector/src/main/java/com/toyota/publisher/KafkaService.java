package com.toyota.publisher;

import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;

public interface KafkaService {
    void sendRawRate(Rate rawRate);
    void sendCalculatedRate(CalculatedRate calculatedRate);
}
