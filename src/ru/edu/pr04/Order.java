package ru.edu.pr04;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    public static final double CONFIRM_LIMIT = 500_000.0;

    private final Employee employee;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(Employee employee) {
        this.employee = Objects.requireNonNull(employee, "employee is null");
    }

    public void addProduct(Product product, int quantity) {
        checkDraft();
        OrderItem newItem = new OrderItem(product, quantity);
        for (OrderItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increase(quantity);
                return;
            }
        }
        items.add(newItem);
    }

    public void removeProduct(String productId) {
        checkDraft();
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId is null or blank");
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProduct().getId().equals(productId)) {
                items.remove(i);
                return;
            }
        }
    }

    public double total() {
        double sum = 0;
        for (OrderItem item : items) {
            sum += item.getLineTotal();
        }
        return sum;
    }

    public void confirm() {
        checkDraft();
        if (items.isEmpty()) {
            throw new IllegalStateException("order is empty");
        }
        if (total() > CONFIRM_LIMIT) {
            throw new IllegalStateException("order total exceeds " + CONFIRM_LIMIT);
        }
        status = OrderStatus.CONFIRMED;
    }

    public List<OrderItem> getItems() {
        // Копируем также позиции: increase у полученной позиции не изменит заказ.
        List<OrderItem> copy = new ArrayList<>();
        for (OrderItem item : items) {
            copy.add(new OrderItem(item.getProduct(), item.getQuantity()));
        }
        return List.copyOf(copy);
    }

    public OrderStatus getStatus() { return status; }
    public Employee getEmployee() { return employee; }

    private void checkDraft() {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("order is confirmed");
        }
    }
}
