package com.team21.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.team21.dto.BuyerDTO;
import com.team21.dto.CartDTO;
import com.team21.dto.LoginDTO;
import com.team21.dto.OrderDTO;
import com.team21.entity.BuyerEntity;
import com.team21.entity.CartEntity;
import com.team21.entity.WishlistEntity;
import com.team21.exception.UserMSException;
import com.team21.repository.BuyerRepository;
import com.team21.repository.CartRepository;
import com.team21.repository.WishlistRepository;
import com.team21.utility.CompositeKey;
import com.team21.validator.UserValidator;

@Transactional
@Service(value = "buyerService")
public class BuyerServiceImpl implements BuyerService {

	// To maintain Buyer's Id in sequential manner
	private static int buyerCount;

	static {
		buyerCount = 100;
	}

	@Autowired
	private BuyerRepository buyerRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private CartRepository cartRepository;

	// Registration for Buyer
	@Override
	public String buyerRegistration(BuyerDTO buyerDTO) throws UserMSException {

		// find if a buyer with same email
		BuyerEntity buyerByEmail = buyerRepository.findByEmail(buyerDTO.getEmail());
		// find if a buyer with same phone number
		BuyerEntity buyerByPhoneNumber = buyerRepository.findByPhoneNumber(buyerDTO.getPhoneNumber());

		// using Demorgan's Law in programming logic
		if (!(buyerByEmail == null && buyerByPhoneNumber == null))
			throw new UserMSException("Buyer already exist");

		UserValidator.validateBuyer(buyerDTO);

		String id = "BUY" + buyerCount++;

		BuyerEntity buyer = new BuyerEntity();

		buyer.setBuyerId(id);
		buyer.setEmail(buyerDTO.getEmail());
		buyer.setName(buyerDTO.getName());
		buyer.setPhoneNumber(buyerDTO.getPhoneNumber());
		buyer.setPassword(buyerDTO.getPassword());
		buyer.setIsActive("False");
		buyer.setIsPrivileged("False");
		buyer.setRewardPoints(0);

		buyerRepository.save(buyer);

		return buyer.getBuyerId();

	}

	// Login for Buyer
	@Override
	public String buyerLogin(LoginDTO loginDTO) throws UserMSException {
		String email = loginDTO.getEmailId();
		String password = loginDTO.getPassword();

		if (!UserValidator.validateEmail(email))
			throw new UserMSException("You have entered wrong EmailId!");

		BuyerEntity buyer = buyerRepository.findByEmail(email);

		if (buyer == null)
			throw new UserMSException("Buyer does not exist!");

		if (!buyer.getPassword().equals(password))
			throw new UserMSException("Wrong credentials");

		buyer.setIsActive("True");

		buyerRepository.save(buyer);

		return "Logged in Successfully";
	}

	// Get details of specific buyer by giving buyer ID
	@Override
	public BuyerDTO getSepcificBuyer(String buyerId) throws UserMSException {

		Optional<BuyerEntity> optional = buyerRepository.findById(buyerId);

		BuyerDTO buyerDTO = null;
		if (optional.isPresent()) {
			buyerDTO = new BuyerDTO();
			// System.out.println(optional.get().getBuyerId());
			BuyerEntity buyer = optional.get();
			buyerDTO.setEmail(buyer.getEmail());
			buyerDTO.setIsActive(buyer.getIsActive());
			buyerDTO.setIsPrivileged(buyer.getIsPrivileged());
			buyerDTO.setName(buyer.getName());
			buyerDTO.setPassword(null);
			buyerDTO.setPhoneNumber(buyer.getPhoneNumber());
			buyerDTO.setRewardPoints(buyer.getRewardPoints());

		} else
			throw new UserMSException("No such buyer found");

		return buyerDTO;
	}

	// Delete Buyer
	@Override
	public String deleteBuyer(String id) throws UserMSException {

		BuyerEntity buyer = buyerRepository.findByBuyerId(id);

		if (buyer == null)
			throw new UserMSException("Buyer does not exist!");

		buyerRepository.delete(buyer);

		return "Account Deleted Successfully";
	}

	// Add Product to Buyers's WishList
	@Override
	public String addToWishlist(String prodId, String buyerId) throws UserMSException {

		CompositeKey productBuyerCompositeKey = new CompositeKey(prodId, buyerId);

		Optional<WishlistEntity> optionalWishlistEntity = wishlistRepository.findById(productBuyerCompositeKey);

		Optional<BuyerEntity> optionalBuyerEntity = buyerRepository.findById(buyerId);

		if (!optionalBuyerEntity.isPresent())
			throw new UserMSException("Buyer does not exist");

		if (optionalWishlistEntity.isPresent())
			throw new UserMSException("Product already present in Wishlist");

		WishlistEntity newWish = new WishlistEntity();

		newWish.setCompoundId(productBuyerCompositeKey);

		wishlistRepository.save(newWish);

		return "Added Successfully to Wishlist";
	}

	// Add Product to Buyers's Cart
	@Override
	public String addToCart(String prodId, String buyerId, Integer quantity) throws UserMSException {
		Optional<BuyerEntity> optionalBuyerEntity = buyerRepository.findById(buyerId);

		if (!optionalBuyerEntity.isPresent())
			throw new UserMSException("Buyer does not exist");

		// if quantity to be added is zero
		if (quantity == 0)
			return "No Update needed";

		CompositeKey productBuyerCompositeKey = new CompositeKey(prodId, buyerId);

		Optional<CartEntity> optional = cartRepository.findById(productBuyerCompositeKey);

		CartEntity cart;

		if (optional.isPresent()) {

			cart = optional.get();

			quantity = quantity + cart.getQuantity();

		} else {

			cart = new CartEntity();

			cart.setCompoundKey(productBuyerCompositeKey);
		}

		cart.setQuantity(quantity);

		cartRepository.save(cart);

		return "Added Successfully to Cart";
	}

	// Get List of Cart Items
	@Override
	public List<CartDTO> getCart(String id) throws UserMSException {
		Optional<BuyerEntity> optionalBuyerEntity = buyerRepository.findById(id);

		if (!optionalBuyerEntity.isPresent())
			throw new UserMSException("Buyer does not exist");

		List<CartEntity> listOfProdsWithQty = cartRepository.findByCompoundKeyBuyerId(id);

		if (listOfProdsWithQty.isEmpty())
			throw new UserMSException("Cart is Empty");

		List<CartDTO> CartDTOs = new ArrayList<CartDTO>();

		for (CartEntity productFromCart : listOfProdsWithQty) {
			CartDTO cartDTO = new CartDTO();

			cartDTO.setBuyerId(productFromCart.getCompoundKey().getBuyerId());
			cartDTO.setProdId(productFromCart.getCompoundKey().getProdId());
			cartDTO.setQuantity(productFromCart.getQuantity());

			CartDTOs.add(cartDTO);
		}

		return CartDTOs;
	}

	// Remove products from Cart
	@Override
	public String removeFromCart(String buyerId, String prodId) throws UserMSException {

		CartEntity cartItem = cartRepository.findByCompoundKeyBuyerIdAndCompoundKeyProdId(buyerId, prodId);

		if (cartItem == null)
			throw new UserMSException("Item not found in your cart!");

		cartRepository.deleteByCompoundKeyBuyerIdAndCompoundKeyProdId(buyerId, prodId);

		return "Sucess! Cart item Deleted!";
	}

	// Add Reward points of Buyer
	@Override
	public void addRewardPoints(String buyerId, double amount) {
		Optional<BuyerEntity> optional = buyerRepository.findById(buyerId);
		if (optional.isPresent() == true) {
			int rewardPoints = optional.get().getRewardPoints();
			int finalRewardPoints = rewardPoints + ((int) amount / 100);
			optional.get().setRewardPoints(finalRewardPoints);
			if (finalRewardPoints >= 10000) {
				optional.get().setIsPrivileged("True");
			}
			buyerRepository.save(optional.get());
		}
	}

	// calculate and store remaining reward points after discount
	@Override
	public void updateRewardPoints(String buyerId) {
		Optional<BuyerEntity> optional = buyerRepository.findById(buyerId);
		if (optional.isPresent()) {
			BuyerEntity buyerEntity = optional.get();
			buyerEntity.setRewardPoints(buyerEntity.getRewardPoints() % 4);
			buyerRepository.save(buyerEntity);
		}
	}

	// get reward points for specific user
	@Override
	public Integer getRewardPoints(String buyerId) throws UserMSException {
		Optional<BuyerEntity> optional = buyerRepository.findById(buyerId);
		if (optional.isPresent()) {
			return optional.get().getRewardPoints();
		} else
			throw new UserMSException("Buyer does not exist");
	}

	// Remove products from Cart
	@Override
	public String removeFromWishlist(String buyerId, String prodId) throws UserMSException {
		WishlistEntity wishlistItem = wishlistRepository.findByCompoundIdBuyerIdAndCompoundIdProdId(buyerId, prodId);

		if (wishlistItem == null)
			throw new UserMSException("Item not found in your wishlist!");

		wishlistRepository.deleteByCompoundIdBuyerIdAndCompoundIdProdId(buyerId, prodId);

		return "Sucess! Wishlist item Deleted!";
	}

	// Move Product from Wishlist to cart
	@Override
	public String moveFromWishlistToCart(String buyerId, String prodId, Integer quantity) throws UserMSException {
		// if quantity to be added is zero
		if (quantity == 0)
			return "No Update needed";

		this.removeFromWishlist(buyerId, prodId);

		String result = this.addToCart(prodId, buyerId, quantity);

		return result;

	}

}
