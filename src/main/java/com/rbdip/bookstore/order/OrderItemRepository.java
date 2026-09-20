package com.rbdip.bookstore.order;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("select distinct item from OrderItem item join fetch item.product where item.orderId in :orderIds")
    List<OrderItem> findAllByOrderIdInWithProduct(@Param("orderIds") Collection<Long> orderIds);

    @Query("select case when count(item) > 0 then true else false end "
            + "from OrderItem item where item.product.id = :productId")
    boolean existsForProductId(@Param("productId") Long productId);
}
