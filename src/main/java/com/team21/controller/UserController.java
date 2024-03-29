package com.team21.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.team21.dto.BuyerDTO;
import com.team21.dto.CartDTO;
import com.team21.dto.LoginDTO;
import com.team21.dto.OrderDTO;
import com.team21.dto.ProductDTO;
import com.team21.dto.ProductOrderedDTO;
import com.team21.dto.SellerDTO;
import com.team21.dto.SubscribedProductDTO;
import com.team21.exception.UserMSException;
import com.team21.service.BuyerService;
import com.team21.service.SellerService;
import com.team21.utility.CurrentOrderStatus;

@RestController
public class UserController {

	private static final String PRODUCT_DOES_NOT_EXISTS = "PRODUCT_DOES_NOT_EXISTS";
	private static final String PRODUCT_INVALID_UNAVAILABLE = "";
	@Autowired
	Environment environment;

	@Autowired
	BuyerService buyerService;

	@Autowired
	SellerService sellerService;

	@Value("${product.uri}")
	String productUri;

	@Value("${order.uri}")
	String orderUri;

	// Register the Buyer
	@PostMapping(value = "/userMS/buyer/register")
	public ResponseEntity<String> registerBuyer(@RequestBody BuyerDTO buyerDto) {

		try {
			String result = "Buyer registered successfully with buyer Id : " + buyerService.buyerRegistration(buyerDto);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.EXPECTATION_FAILED);
		}
	}

	// Login for Buyer
	@PostMapping(value = "/userMS/buyer/login")
	public ResponseEntity<String> loginBuyer(@RequestBody LoginDTO loginDTO) {
		try {
			String msg = buyerService.buyerLogin(loginDTO);
			return new ResponseEntity<>(msg, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// get all details of specific buyer
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "/userMS/buyer/{buyerId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<BuyerDTO> getSepcificBuyerDetails(@PathVariable String buyerId) {
		try {
			BuyerDTO buyerDTO = buyerService.getSepcificBuyer(buyerId);
			return new ResponseEntity<>(buyerDTO, HttpStatus.OK);

		} catch (UserMSException e) {
			return new ResponseEntity(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}

	}

	// Seller will be able to view orders placed on their products
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "/userMS/seller/view/soldProducts/{sellerId}/{productId}")
	public ResponseEntity<List<ProductOrderedDTO>> getSellerProducts(@PathVariable String sellerId,
			@PathVariable String productId) {
		try {
			sellerService.isSellerPresent(sellerId);
			List<ProductOrderedDTO> productOrdered = new RestTemplate()
					.getForObject(orderUri + "order/view/bySellersProducts/" + sellerId + "/" + productId, List.class);
			return new ResponseEntity<>(productOrdered, HttpStatus.OK);

		} catch (HttpClientErrorException e) {
			String response = "No Order details for this product";
			return new ResponseEntity(response, HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			return new ResponseEntity(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// Delete Buyer
	@DeleteMapping(value = "/userMS/buyer/deregister/{id}")
	public ResponseEntity<String> deleteBuyerAccount(@PathVariable String id) {
		try {
			String result = buyerService.deleteBuyer(id);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// view products by name by Visitor
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "userMS/buyer/view/products/byName/{productName}")
	public ResponseEntity<ProductDTO> viewProductsByName(@PathVariable String productName) {
		try {
			ProductDTO productDTO = new RestTemplate().getForObject(productUri + "product/get/name/" + productName,
					ProductDTO.class);
			return new ResponseEntity<>(productDTO, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity(environment.getProperty(PRODUCT_DOES_NOT_EXISTS), HttpStatus.NOT_FOUND);
		}
	}

	// view list of all the products by category by Visitor
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "userMS/buyer/view/products/byCategory/{categoryName}")
	public ResponseEntity<List<ProductDTO>> viewProductsByCategory(@PathVariable String categoryName) {
		try {
			List<ProductDTO> productDTOList = new RestTemplate()
					.getForObject(productUri + "product/get/category/" + categoryName, List.class);
			return new ResponseEntity<>(productDTOList, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity(environment.getProperty(PRODUCT_DOES_NOT_EXISTS), HttpStatus.NOT_FOUND);
		}
	}

	// Add Product to Buyer's WishList
	@PostMapping(value = "/userMS/buyer/wishlist/add/{buyerId}/{prodId}")
	public ResponseEntity<String> addToWishlist(@PathVariable String buyerId, @PathVariable String prodId)
			throws UserMSException {
		try {
			ProductDTO productDTO = new RestTemplate().getForObject(productUri + "product/get/Id/" + prodId,
					ProductDTO.class);
			if (productDTO == null)
				throw new UserMSException(PRODUCT_INVALID_UNAVAILABLE);
			String result = buyerService.addToWishlist(productDTO.getProdId(), buyerId);

			return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(environment.getProperty(PRODUCT_DOES_NOT_EXISTS), HttpStatus.NOT_FOUND);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	// Add Product to Buyers's Cart
	@PostMapping(value = "/userMS/buyer/cart/add/{buyerId}/{prodId}/{quantity}")
	public ResponseEntity<String> addProductToCart(@PathVariable String buyerId, @PathVariable String prodId,
			@PathVariable Integer quantity) throws UserMSException {
		try {
			/*
			 * Here we are using rest template to fetch the productDTO from ProductMS, and
			 * from that productDTO we will fetch the product id. If product is not found
			 * then we are throwing an exception
			 */
			ProductDTO productDTO = new RestTemplate().getForObject(productUri + "product/get/Id/" + prodId,
					ProductDTO.class);
			if (productDTO == null)
				throw new UserMSException(PRODUCT_INVALID_UNAVAILABLE);
			if (productDTO.getStock() < quantity)
				throw new UserMSException("STOCK_INSUFFICIENT");
			String result = buyerService.addToCart(productDTO.getProdId(), buyerId, quantity);
			return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(environment.getProperty(PRODUCT_DOES_NOT_EXISTS), HttpStatus.NOT_FOUND);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	// Get List of Cart Items
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "/userMS/buyer/cart/get/{buyerId}")
	public ResponseEntity<List<CartDTO>> getProductListFromCart(@PathVariable String buyerId) throws UserMSException {
		try {
			List<CartDTO> cartDTOs = buyerService.getCart(buyerId);
			return new ResponseEntity<>(cartDTOs, HttpStatus.ACCEPTED);
		} catch (UserMSException e) {

			return new ResponseEntity(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// get order history of particular Buyer
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "userMS/buyer/orderHistory/{buyerId}")
	public ResponseEntity<List<OrderDTO>> getOrderHistoryOfBuyer(@PathVariable String buyerId) {
		try {

			List<OrderDTO> orders = new RestTemplate().getForObject(orderUri + "order/view/byBuyerId/" + buyerId,
					List.class);
			return new ResponseEntity<>(orders, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity("No order history found for given BuyerID", HttpStatus.NOT_FOUND);
		}
	}

	// get product id list from buyer's cart
	@GetMapping(value = "/cart/product/{buyerId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<String> getProductIdListFromCart(@PathVariable String buyerId) {
		try {
			List<CartDTO> listCart = buyerService.getCart(buyerId);
			List<String> productList = new ArrayList<>();
			for (CartDTO cart : listCart) {
				productList.add(cart.getProdId());
			}
			return productList;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	// Get Quantity from cart by giving buyerId and product ID
	@GetMapping(value = "/cart/product/quantity/{buyerId}/{prodId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Integer getQuantityFromCart(@PathVariable String buyerId, @PathVariable String prodId)
			throws UserMSException {
		try {
			List<CartDTO> listCart = buyerService.getCart(buyerId);
			for (CartDTO cart : listCart) {
				if (cart.getProdId().equals(prodId))
					return cart.getQuantity();
			}
			return 0;
		} catch (UserMSException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	// Remove item from Cart
	@PutMapping(value = "/userMS/buyer/cart/remove/{buyerId}/{prodId}")
	public ResponseEntity<String> removeFromCart(@PathVariable String buyerId, @PathVariable String prodId)
			throws UserMSException {
		try {
			String result = buyerService.removeFromCart(buyerId, prodId);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			String errorMsg = e.getMessage();
			return new ResponseEntity<>(errorMsg, HttpStatus.NOT_FOUND);

		}
	}

	// Remove item from Wishlist
	@PostMapping(value = "/userMS/buyer/wishlist/remove/{buyerId}/{prodId}")
	public ResponseEntity<String> removeFromWishlist(@PathVariable String buyerId, @PathVariable String prodId)
			throws UserMSException {

		try {
			String result = buyerService.removeFromWishlist(buyerId, prodId);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);

		}
	}

	// Move from Wishlist to cart
	@SuppressWarnings("null")
	@PostMapping(value = "/userMS/buyer/wishlist/cart/{buyerId}/{prodId}/{quantity}")
	public ResponseEntity<String> moveWishlistToCart(@PathVariable String buyerId, @PathVariable String prodId,
			@PathVariable Integer quantity) {
		try {
			ProductDTO productDTO = new RestTemplate().getForObject(productUri + "product/get/Id/" + prodId,
					ProductDTO.class);
			if (productDTO == null)
				throw new UserMSException(PRODUCT_INVALID_UNAVAILABLE);
			if (productDTO.getStock() < quantity)
				throw new UserMSException("STOCK_INSUFFICIENT");
			String result = buyerService.moveFromWishlistToCart(buyerId, prodId, quantity);
			return new ResponseEntity<>(result, HttpStatus.ACCEPTED);

		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(environment.getProperty(PRODUCT_INVALID_UNAVAILABLE), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	// Re-Order from previous order
	@PostMapping(value = "/userMS/buyer/reOrder/{orderId}")
	public ResponseEntity<String> reOrder(@PathVariable String orderId) {
		try {
			String result = new RestTemplate().postForObject(orderUri + "order/reOrder/" + orderId, null, String.class);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>("No Order history found for you", HttpStatus.NOT_FOUND);
		}
	}

	// Register the Seller
	@PostMapping(value = "/userMS/seller/register")
	public ResponseEntity<String> registerSeller(@RequestBody SellerDTO sellerDto) {

		try {
			String result = "Seller registered successfully with seller Id : "
					+ sellerService.sellerRegistration(sellerDto);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.EXPECTATION_FAILED);
		}

	}

	// Login for Seller
	@PostMapping(value = "/userMS/seller/login")
	public ResponseEntity<String> loginSeller(@RequestBody LoginDTO loginDTO) {
		try {
			String result = sellerService.sellerLogin(loginDTO);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// Deactivate seller account and delete all his products
	@DeleteMapping(value = "/userMS/seller/deregister/{sellerId}")
	public ResponseEntity<String> deactivateSellerAccount(@PathVariable String sellerId) {

		try {
			String msg = sellerService.deleteSeller(sellerId);
			new RestTemplate().delete(productUri + "product/deleteAll/" + sellerId);
			return new ResponseEntity<>(msg, HttpStatus.OK);
		} catch (UserMSException e) {
			return new ResponseEntity<>(environment.getProperty(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	// Seller can add the products
	@PostMapping(value = "/seller/products/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> addProduct(@RequestBody ProductDTO productDTO) {
		try {
			String result = new RestTemplate().postForObject(productUri + "product/add", productDTO, String.class);
			return new ResponseEntity<>(result, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(environment.getProperty("PRODUCT_ALREADY_EXISTS"), HttpStatus.ALREADY_REPORTED);
		}
	}

	// seller can delete his products
	@GetMapping(value = "/seller/products/delete/{prodId}")
	public ResponseEntity<String> deleteProduct(@PathVariable String prodId) {
		try {
			new RestTemplate().delete(productUri + "product/delete/" + prodId);

			String response = "Deleted Successfully";
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(environment.getProperty(PRODUCT_INVALID_UNAVAILABLE), HttpStatus.BAD_REQUEST);
		}
	}

	// after discount add additional reward points
	@PostMapping(value = "/userMS/updateRewards/{buyerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public boolean orderUpdate(@RequestBody Float amount, @PathVariable String buyerId) {
		buyerService.updateRewardPoints(buyerId);
		buyerService.addRewardPoints(buyerId, amount);
		return true;

	}

	@PutMapping(value = "/userMS/seller/update/stock/{productId}/{quantity}")
	public ResponseEntity<String> updateStockBYSeller(@PathVariable String productId, @PathVariable Integer quantity) {
		try {
			new RestTemplate().put(productUri + "product/update/stock/" + productId + "/" + quantity, null);
			String result = "Stock Updated Successfully with productId-: " + productId + " and quantity: " + quantity;
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		}
	}

	// checkout cart products for final order
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping(value = "/userMS/cart/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> checkout(@RequestBody OrderDTO orderDTO) {
		try {
			if (buyerService.isCartEmpty(orderDTO.getBuyerId()))
				throw new UserMSException("Service.CART_EMPTY");
			String result = new RestTemplate().postForObject(orderUri + "order/place", orderDTO, String.class);
			return new ResponseEntity<String>(result, HttpStatus.ACCEPTED);

		} catch (HttpClientErrorException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		} catch (UserMSException e) {
			return new ResponseEntity(environment.getProperty(e.getMessage()), HttpStatus.NOT_ACCEPTABLE);
		}
	}

	// get reward points for specific user
	@GetMapping(value = "/userMS/get/rewardPoints/{buyerId}")
	public Integer getRewardPoints(@PathVariable String buyerId) throws UserMSException {

		try {
			return buyerService.getRewardPoints(buyerId);
		} catch (UserMSException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer NOT found", e);
		}

	}

	// update status of placed orders
	@GetMapping(value = "/userMS/seller/order/status/update/{orderId}/{status}")
	public ResponseEntity<String> orderStatusUpdate(@PathVariable String orderId,
			@PathVariable CurrentOrderStatus status) {
		try {
			String result = new RestTemplate().postForObject(orderUri + "order/status/update/" + orderId + "/" + status,
					null, String.class);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (HttpClientErrorException e) {

			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);

		}
	}
	
	@PostMapping(value = "/userMS/buyer/addSubscription")
	public ResponseEntity<String> addSubscription(@RequestBody SubscribedProductDTO subscribedProductDTO) {
		try {
			String result = new RestTemplate().postForObject(productUri + "product/subscriptions/add/",
					subscribedProductDTO, String.class);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GetMapping(value = "/userMS/buyer/get/specificSubscription/{buyerId}/{prodId}")
	public ResponseEntity<SubscribedProductDTO> getSubscription(@PathVariable String buyerId, @PathVariable String prodId){ 
		try {
			SubscribedProductDTO subscribedProductDTO =
					new RestTemplate().getForObject(productUri+ "product/subscriptions/get/"+buyerId+"/"+prodId, SubscribedProductDTO.class);
			return new ResponseEntity<>(subscribedProductDTO, HttpStatus.OK);
			
		}catch(Exception e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}


}
