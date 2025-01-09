package uit.carbon_shop.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uit.carbon_shop.domain.Order;
import uit.carbon_shop.model.MediatorDoneOrderDTO;
import uit.carbon_shop.model.OrderDTO;
import uit.carbon_shop.model.OrderStatus;
import uit.carbon_shop.repos.AppUserRepository;
import uit.carbon_shop.repos.OrderRepository;
import uit.carbon_shop.repos.ProjectRepository;
import uit.carbon_shop.util.NotFoundException;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final OrderMapper orderMapper;
    private final RedissonClient redissonClient;

    public Page<OrderDTO> findAll(final String filter, final Pageable pageable) {
        Page<Order> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = orderRepository.findAllById(longFilter, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<OrderDTO> findAllCreatedBy(final Long userId, final Pageable pageable) {
        final Page<Order> page = orderRepository.findByCreatedBy_Id(userId, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<OrderDTO> findAllByStatusAndCreatedBy(final OrderStatus status, final Long userId,
            final Pageable pageable) {
        final Page<Order> page = orderRepository.findByCreatedBy_IdAndStatus(userId, status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<OrderDTO> findByOwnerCompany(final Long companyId, final Pageable pageable) {
        final Page<Order> page = orderRepository.findByProject_OwnerCompany_Id(companyId, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<OrderDTO> findByStatusAndOwnerCompany(final OrderStatus status, final Long companyId,
            final Pageable pageable) {
        final Page<Order> page = orderRepository.findByProject_OwnerCompany_IdAndStatus(companyId, status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<OrderDTO> findByStatus(OrderStatus status, Pageable pageable) {
        final Page<Order> page = orderRepository.findByStatus(status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public OrderDTO get(final Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> orderMapper.updateOrderDTO(order, new OrderDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final OrderDTO orderDTO) {
        final Order order = new Order();
        order.setId(orderDTO.getOrderId());
        orderMapper.updateOrder(orderDTO, order, projectRepository, appUserRepository);
        return orderRepository.save(order).getId();
    }

    public void update(final Long orderId, final OrderDTO orderDTO) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundException::new);
        orderMapper.updateOrder(orderDTO, order, projectRepository, appUserRepository);
        orderRepository.save(order);
    }

    public void updateStatus(final Long orderId, final OrderStatus status) {
        RLock orderLock = redissonClient.getLock("ORDER_LOCK:" + orderId);
        orderLock.lock();
        try {
            final Order order = orderRepository.findById(orderId)
                    .orElseThrow(NotFoundException::new);
            if (!order.getStatus().canUpdateTo(status)) {
                throw new IllegalArgumentException("Cannot update status from " + order.getStatus() + " to " + status);
            }
            order.setStatus(status);
            orderRepository.save(order);
        } finally {
            orderLock.unlock();
        }
    }

    public void doneOrder(final Long orderId, final MediatorDoneOrderDTO doneOrderDTO) {
        RLock orderLock = redissonClient.getLock("ORDER_LOCK:" + orderId);
        orderLock.lock();
        try {
            final Order order = orderRepository.findById(orderId)
                    .orElseThrow(NotFoundException::new);
            order.setStatus(OrderStatus.DONE);
            order.setContractFile(doneOrderDTO.getContractFile());
            order.setCertImages(doneOrderDTO.getCertImages());
            order.setPaymentBillFile(doneOrderDTO.getPaymentBillFile());
            order.setPayDate(doneOrderDTO.getPayDate());
            order.setDeliveryDate(doneOrderDTO.getDeliveryDate());
            order.setContractSignDate(doneOrderDTO.getContractSignDate());
            orderRepository.save(order);
        } finally {
            orderLock.unlock();
        }
    }

    public void delete(final Long orderId) {
        orderRepository.deleteById(orderId);
    }

}
