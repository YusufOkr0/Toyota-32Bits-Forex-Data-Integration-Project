package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.dtos.request.UpdatePlanRequest;
import com.toyota.restdataprovider.dtos.response.UpdatePlanResponse;
import com.toyota.restdataprovider.service.abstracts.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping(value = "/pricing/{username}")
    ResponseEntity<UpdatePlanResponse> updatePricingPlan(
            @PathVariable("username") String userName,
            @Valid @RequestBody UpdatePlanRequest updatePlanRequest)
    {
        UpdatePlanResponse updatePricingPlanResponse = userService.updatePricingPlanByUsername(userName,updatePlanRequest);

        return ResponseEntity
                .ok(updatePricingPlanResponse);
    }


}
