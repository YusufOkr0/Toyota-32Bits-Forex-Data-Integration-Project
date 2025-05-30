package com.toyota.restdataprovider.service;


import com.toyota.restdataprovider.dtos.request.UpdatePlanRequest;
import com.toyota.restdataprovider.dtos.response.UpdatePlanResponse;
import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.entity.PricingPlan;
import com.toyota.restdataprovider.exception.InvalidPricingPlanException;
import com.toyota.restdataprovider.exception.UserNotFoundException;
import com.toyota.restdataprovider.repository.UserRepository;
import com.toyota.restdataprovider.service.abstracts.RateLimitService;
import com.toyota.restdataprovider.service.concretes.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    public void whenUpdatePricingPlanWithValidPlan_ThenReturnUpdatePlanResponse(){
        String username = "validUsername";
        ForexUser testUser = ForexUser.builder()
                .username(username)
                .pricingPlan(PricingPlan.STANDARD)
                .build();
        UpdatePlanRequest updatePlanRequest = UpdatePlanRequest.builder()
                .newPricingPlan(PricingPlan.PREMIUM.name())
                .build();


        when(userRepository.findByUsername(username)).thenReturn(Optional.ofNullable(testUser));

        UpdatePlanResponse updatePlanResponse = userService.updatePricingPlanByUsername(username,updatePlanRequest);

        assertNotNull(updatePlanResponse);
        assertEquals(username,updatePlanResponse.getUsername());
        assertEquals(PricingPlan.PREMIUM.name(),updatePlanResponse.getPricingPlan());
        verify(rateLimitService, times(1)).removeUserBucket(username);
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository,times(1)).save(any(ForexUser.class));

    }

    @Test
    public void whenUpdatePricingPlanWithNonExistsUsername_ThenThrowUserNotFoundException(){

        String nonExistsUsername = "Invalid_Username";
        UpdatePlanRequest updatePlanRequest = UpdatePlanRequest.builder()
                .newPricingPlan(PricingPlan.STANDARD.name())
                .build();

        when(userRepository.findByUsername(nonExistsUsername)).thenReturn(Optional.empty());


        assertThrows(UserNotFoundException.class, () -> {
            userService.updatePricingPlanByUsername(nonExistsUsername, updatePlanRequest);
        });

        verify(userRepository, never()).save(any());
        verify(rateLimitService, never()).removeUserBucket(any());

    }



    @Test
    public void whenUpdatePricingPlanWithInvalidPlan_ThenThrowInvalidPricingPlanException(){
        String username = "validUsername";
        String invalidPricingPlan = "INVALID_PLAN";
        ForexUser testUser = ForexUser.builder()
                .username(username)
                .pricingPlan(PricingPlan.STANDARD)
                .build();

        UpdatePlanRequest updatePlanRequest = UpdatePlanRequest.builder()
                .newPricingPlan(invalidPricingPlan)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.ofNullable(testUser));

        assertThrows(
                InvalidPricingPlanException.class,
                () -> userService.updatePricingPlanByUsername(username, updatePlanRequest)
        );
        verify(userRepository, never()).save(any());
        verify(rateLimitService, never()).removeUserBucket(any());

    }





}
