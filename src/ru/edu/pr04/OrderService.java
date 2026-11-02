package ru.edu.pr04;

import java.util.Objects;

public class OrderService {
    public Order createOrder(Employee employee) {
        return new Order(employee);
    }

    public String addTotalCategory(Order order) {
        Objects.requireNonNull(order, "order is null");
        double sum = order.total();
        if (sum < 10_000) {
            return "малая";
        }
        if (sum <= 100_000) {
            return "средняя";
        }
        if (sum <= Order.CONFIRM_LIMIT) {
            return "крупная";
        }
        return "крупнейшая";
    }
}
