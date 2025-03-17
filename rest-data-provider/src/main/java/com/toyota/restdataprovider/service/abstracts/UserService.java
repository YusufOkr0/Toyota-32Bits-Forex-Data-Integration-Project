package com.toyota.restdataprovider.service.abstracts;

import com.toyota.restdataprovider.dtos.request.UpdatePlanRequest;
import com.toyota.restdataprovider.dtos.response.UpdatePlanResponse;

public interface UserService {

    UpdatePlanResponse updatePricingPlanByUsername(String userName, UpdatePlanRequest updatePlanRequest);

}
