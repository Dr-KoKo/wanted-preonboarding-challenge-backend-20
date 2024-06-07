package com.wanted.market.order.service;

import com.wanted.market.common.exception.InvalidRequestException;
import com.wanted.market.common.exception.NotFoundException;
import com.wanted.market.common.exception.UnauthorizedRequestException;
import com.wanted.market.order.domain.Order;
import com.wanted.market.order.domain.OrderRepository;
import com.wanted.market.order.domain.StockRequester;
import com.wanted.market.order.event.OrderConfirmedEvent;
import com.wanted.market.order.exception.OutOfStockException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmOrderService {
    private final OrderRepository orderRepository;
    private final StockRequester stockRequester;
    private final ApplicationEventPublisher eventPublisher;

    public ConfirmOrderService(OrderRepository orderRepository, StockRequester stockRequester, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.stockRequester = stockRequester;
        this.eventPublisher = eventPublisher;
    }

    /**
     * product의 상태: ON_SALE -> RESERVED -> SOLD (-> ON_SALE)
     * 재고가 소진되었을 때: OutOfStockEventListener에서 (동시성 제어를 위해) StockManager로부터 Lock을 획득한 뒤 product의 status를 RESERVED로 바꾸자
     * 주문들이 모두 구매확정되었을 때: SoldOutEventListener에서 마찬가지로 Lock을 획득한 뒤 product의 status를 SOLD로 바꾸자
     *
     * @param cmd
     * @throws NotFoundException
     * @throws UnauthorizedRequestException
     * @throws OutOfStockException
     */
    public void confirm(Command cmd) throws NotFoundException, UnauthorizedRequestException, OutOfStockException, InvalidRequestException {
        Order findOrder = orderRepository.findByIdOrThrow(cmd.id());
        Integer leftStock = findOrder.confirm(cmd.userId(), (productId, sellerId) -> orderRepository.findSellerIdByProductId(productId) == sellerId, stockRequester);
        eventPublisher.publishEvent(new OrderConfirmedEvent(findOrder.getId(), findOrder.getProductId(), leftStock));
    }

    public record Command(
            Long id,
            Long userId
    ) {
    }
}