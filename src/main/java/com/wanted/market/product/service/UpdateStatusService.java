package com.wanted.market.product.service;

import com.wanted.market.common.exception.InvalidRequestException;
import com.wanted.market.common.exception.NotFoundException;
import com.wanted.market.order.domain.OrderRepository;
import com.wanted.market.order.domain.vo.Status;
import com.wanted.market.order.event.OrderConfirmedEvent;
import com.wanted.market.order.event.OrderFinishedEvent;
import com.wanted.market.product.domain.Product;
import com.wanted.market.product.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 상품 정보 수정 vs 상품 상태 수정
 * optimistic lock으로 처리
 */
@Service
@Transactional
public class UpdateStatusService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PlatformTransactionManager transactionManager;

    public UpdateStatusService(ProductRepository productRepository, OrderRepository orderRepository, PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.transactionManager = transactionManager;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void toReserved(OrderConfirmedEvent event) throws NotFoundException {
        if (event.leftStock() <= 0) {
            Product findProduct = productRepository.findByIdOrThrow(event.productId());
            findProduct.reserved();
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void toSold(OrderFinishedEvent event) throws NotFoundException, InvalidRequestException {
        Product findProduct = productRepository.findByIdOrThrow(event.productId());
        if (findProduct.isReserved() && !orderRepository.existsByProductIdAndStatus(findProduct.getId(), Status.CONFIRMED)) {
            findProduct.sold();
        }
    }
}
