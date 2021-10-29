package com.team21.service;

import com.team21.dto.BuyerDTO;
import com.team21.exception.UserMSException;

public interface BuyerService {

	public String buyerRegistration(BuyerDTO buyerDTO) throws UserMSException;

	public String buyerLogin(String email, String password) throws UserMSException;

	public String deleteBuyer(String id) throws UserMSException;

}
