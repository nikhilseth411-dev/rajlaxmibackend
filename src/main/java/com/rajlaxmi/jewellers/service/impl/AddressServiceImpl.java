package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.AddressRequest;
import com.rajlaxmi.jewellers.dto.response.AddressResponse;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.entity.Address;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.AddressRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ApiResponse<AddressResponse> addAddress(Long userId, AddressRequest request) {
        if (addressRepository.countByUserId(userId) >= 5) {
            throw new BusinessException("Maximum 5 addresses allowed. Please remove an existing address first.");
        }

        User user = userRepository.getReferenceById(userId);

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        // Auto-set first address as default
        boolean isFirst = addressRepository.countByUserId(userId) == 0;

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .streetAddress(request.getStreetAddress().trim())
                .landmark(request.getLandmark())
                .city(request.getCity().trim())
                .district(request.getDistrict().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode())
                .addressType(request.getAddressType())
                .isDefault(request.isDefault() || isFirst)
                .build();

        addressRepository.save(address);
        return ApiResponse.success("Address added successfully.", toResponse(address));
    }

    @Override
    public ApiResponse<AddressResponse> updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findUserAddress(userId, addressId);

        address.setFullName(request.getFullName().trim());
        address.setPhone(request.getPhone());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setState(request.getState().trim());
        address.setPincode(request.getPincode());
        address.setAddressType(request.getAddressType());

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        }

        addressRepository.save(address);
        return ApiResponse.success("Address updated.", toResponse(address));
    }

    @Override
    public ApiResponse<String> deleteAddress(Long userId, Long addressId) {
        Address address = findUserAddress(userId, addressId);
        addressRepository.delete(address);

        // If deleted address was default, set another as default
        if (address.isDefault()) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                    .stream().findFirst().ifPresent(a -> {
                        a.setDefault(true);
                        addressRepository.save(a);
                    });
        }
        return ApiResponse.success("Address deleted.");
    }

    @Override
    public ApiResponse<AddressResponse> setDefaultAddress(Long userId, Long addressId) {
        Address address = findUserAddress(userId, addressId);
        addressRepository.clearDefaultForUser(userId);
        address.setDefault(true);
        addressRepository.save(address);
        return ApiResponse.success("Default address updated.", toResponse(address));
    }

    private Address findUserAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessException("Address does not belong to this user.");
        }
        return address;
    }

    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId()).fullName(a.getFullName()).phone(a.getPhone())
                .streetAddress(a.getStreetAddress()).landmark(a.getLandmark())
                .city(a.getCity()).district(a.getDistrict()).state(a.getState())
                .pincode(a.getPincode()).addressType(a.getAddressType())
                .isDefault(a.isDefault()).build();
    }
}
