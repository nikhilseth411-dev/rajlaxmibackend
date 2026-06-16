package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.AddressRequest;
import com.rajlaxmi.jewellers.dto.response.AddressResponse;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getUserAddresses(Long userId);
    ApiResponse<AddressResponse> addAddress(Long userId, AddressRequest request);
    ApiResponse<AddressResponse> updateAddress(Long userId, Long addressId, AddressRequest request);
    ApiResponse<String> deleteAddress(Long userId, Long addressId);
    ApiResponse<AddressResponse> setDefaultAddress(Long userId, Long addressId);
}
