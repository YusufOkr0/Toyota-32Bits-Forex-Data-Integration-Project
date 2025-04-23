package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.dtos.request.UpdatePlanRequest;
import com.toyota.restdataprovider.dtos.response.UpdatePlanResponse;
import com.toyota.restdataprovider.service.abstracts.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PutMapping(value = "/pricing/{username}")
    ResponseEntity<UpdatePlanResponse> updatePricingPlan(
            @PathVariable("username") String userName,
            @Valid @RequestBody UpdatePlanRequest updatePlanRequest)
    {
        log.info("Received pricing plan update request for username: {}", userName);

        UpdatePlanResponse updatePricingPlanResponse = userService
                .updatePricingPlanByUsername(
                        userName,
                        updatePlanRequest
                );

        return ResponseEntity
                .ok(updatePricingPlanResponse);
    }


}
